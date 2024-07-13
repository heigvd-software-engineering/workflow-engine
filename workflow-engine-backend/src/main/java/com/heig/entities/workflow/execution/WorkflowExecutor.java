package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.NodeModifiedListener;
import com.heig.entities.workflow.cache.Cache;
import com.heig.entities.workflow.errors.*;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.ResultOrStringError;
import com.heig.helpers.ResultOrWorkflowError;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.PolyglotException;

import java.awt.*;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WorkflowExecutor {
    private final Object stateLock = new Object();
    private State state = State.IDLE;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<>();
    private final WorkflowExecutionListener listener;
    private final WorkflowErrors workflowErrors = new WorkflowErrors();
    private final Cache cache;
    private CompletableFuture<Void> toWaitFor = null;
    private final NodeModifiedListener nodeModifiedListener;

    WorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener listener) {
        this.workflow = Objects.requireNonNull(workflow);
        this.listener = Objects.requireNonNull(listener);
        this.cache = Cache.get(workflow);

        this.nodeModifiedListener = node -> {
            var state = getStateFor(node);
            synchronized (state) {
                state.setHasBeenModified(true);
            }
        };
        this.workflow.addNodeModifiedListener(nodeModifiedListener);
    }

    public NodeState getStateFor(@Nonnull Node node) {
        Objects.requireNonNull(node);
        return states.computeIfAbsent(node.getId(), id -> new NodeState(node));
    }

    private void changeNodeState(@Nonnull NodeState ns, @Nonnull State state) {
        Objects.requireNonNull(ns);
        Objects.requireNonNull(state);
        ns.setState(state);
        listener.nodeStateChanged(ns);
    }

    private CompletableFuture<Void> executeNode(@Nonnull Node node) {
        Objects.requireNonNull(node);
        return CompletableFuture.supplyAsync((Supplier<ResultOrWorkflowError<NodeArguments>>) () -> {
            var ns = getStateFor(node);
            var error = false;
            var we = new WorkflowErrors();
            var args = new NodeArguments();
            //To run a node, we need all inputs (except the ones marked as optional) values to be available with no error
            for (var input : node.getInputs().values()) {
                var inputValueOpt = ns.getInputValue(input.getId());
                //The only way for res to be null is when it is an optional input
                if (inputValueOpt.isEmpty()) {
                    if (!input.isOptional()) {
                        throw new RuntimeException("Should never happen ! The input should have been optional to be null !");
                    }
                    continue;
                }
                var inputValue = inputValueOpt.get();

                if (inputValue.isEmpty()) {
                    error = true;
                    we.addError(new ErroredInputConnector(input));
                } else {
                    args.putArgument(input.getName(), inputValue.get());
                }
            }
            if (error) {
                return ResultOrWorkflowError.error(we);
            }

            //Case if the node hasn't been modified and neither have the previous nodes (the node needs to be deterministic too)
            if (!ns.hasBeenModified() && node.isDeterministic()) {
                var optCache = cache.get(node, args);
                if (optCache.isPresent()) {
                    return ResultOrWorkflowError.result(optCache.get());
                }
            }

            changeNodeState(ns, State.RUNNING);

            //Only used if the node finished with a timeout
            var weTimeout = new WorkflowErrors();
            weTimeout.addError(new ExecutionTimeout(node));

            var fut = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return ResultOrWorkflowError.result(node.execute(args));
                    } catch (Exception e) {
                        we.addError(new FailedExecution(node, e.getMessage() == null ? "Unknown error" : e.getMessage()));
                        return ResultOrWorkflowError.<NodeArguments>error(we);
                    }
                })
                .completeOnTimeout(ResultOrWorkflowError.error(weTimeout), node.getTimeout(), TimeUnit.MILLISECONDS);
            var resultOpt = fut.join();
            if (resultOpt.getErrorMessage().isPresent()) {
                return ResultOrWorkflowError.error(resultOpt.getErrorMessage().get());
            }
            var result = resultOpt.getResult().get();

            //Checking that every output needed (non-optional) is present and of the correct type
            var isErrored = false;
            for (var output : node.getOutputs().values()) {
                var argument = result.getArgument(output.getName());
                if (argument.isEmpty()) {
                    if (output.isOptional()) {
                        //If there are no value for the output and that the output is optional, we put the default value for the output type
                        result.putArgument(output.getName(), output.getType().defaultValue());
                        continue;
                    }

                    we.addError(new MissingOutputValue(output));
                    isErrored = true;
                    continue;
                }
                var argumentValue = argument.get();
                var argumentType = WorkflowTypes.fromObject(argumentValue);
                //We check if we can convert the execution result to the type of the output connector
                if (!output.getType().canBeConvertedFrom(argumentType)) {
                    we.addError(new WrongType(argumentType, output));
                    isErrored = true;
                }
            }
            if (isErrored) {
                return ResultOrWorkflowError.error(we);
            } else {
                if (node.isDeterministic()) {
                    ns.setHasBeenModified(false);
                    cache.set(node, args, result);
                }

                return ResultOrWorkflowError.result(result);
            }
        }).thenApplyAsync(o -> {
            o.execute(value -> {
                changeNodeState(getStateFor(node), State.FINISHED);
            }, we -> {
                var ns = getStateFor(node);
                synchronized (ns) {
                    ns.setErrors(we);
                    changeNodeState(ns, State.FAILED);
                }
            });
            return o.getResult();
        }).thenComposeAsync(o -> {
            var toWait = new LinkedList<CompletableFuture<Void>>();

            for (var output : node.getOutputs().values()) {
                var res = o.map(
                    value -> {
                        var opt = value.getArgument(output.getName());
                        if (opt.isPresent()) {
                            return opt.get();
                        }
                        throw new RuntimeException("Should never happen! Already checked before");
                    }
                ).orElse(null);

                for (var input : output.getConnectedTo()) {
                    var other = input.getParent();
                    var stateOther = getStateFor(other);
                    synchronized (stateOther) {
                        stateOther.setInputValue(input.getId(), res);

                        if (stateOther.isReady()) {
                            toWait.add(executeNode(other));
                        }
                    }
                }
            }
            return CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        });
    }

    public void checkForErrors() {
        workflowErrors.clear();

        var errors = workflow.isValid();
        if (errors.isPresent()) {
            workflowErrors.merge(errors.get());
            synchronized (stateLock) {
                state = State.FAILED;
                listener.workflowStateChanged(this);
            }
        } else {
            synchronized (stateLock) {
                //If we start checkForErrors from executeWorkflow, the state will be State.RUNNING
                //If not, the previous state will be State.FAILED or State.IDLE
                if (state == State.FAILED) {
                    state = State.IDLE;
                    listener.workflowStateChanged(this);
                }
            }
        }
    }

    public boolean executeWorkflow() {
        synchronized (stateLock) {
            if (state == State.RUNNING) {
                return false;
            }
            state = State.RUNNING;
            listener.workflowStateChanged(this);
        }

        workflow.getNodes().values().forEach(n -> {
            var ns = getStateFor(n);
            synchronized (ns) {
                ns.clearInputs();
                changeNodeState(ns, State.IDLE);
            }
        });

        checkForErrors();
        if (!workflowErrors.getErrors().isEmpty()) {
            return false;
        }

        //The starting nodes of our workflow are the nodes that have no input connected (we already know that all inputs
        //that are not optional are connected to an output thanks to workflow.isValid())
        var notConnectedNodes = workflow.getNodes().values().stream().filter(n -> n.getInputs().values().stream().noneMatch(i -> i.getConnectedTo().isPresent())).toList();
        var toWait = new LinkedList<CompletableFuture<Void>>();
        for (var node : notConnectedNodes) {
            toWait.add(executeNode(node));
        }
        toWaitFor = CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        toWaitFor.thenAcceptAsync(v -> {
            var failed = states.values().stream().anyMatch(ns -> ns.getState() == State.FAILED);
            if (failed) {
                synchronized (stateLock) {
                    state = State.FAILED;
                    listener.workflowStateChanged(this);
                }
            } else {
                synchronized (stateLock) {
                    state = State.FINISHED;
                    listener.workflowStateChanged(this);
                }
            }
        });
        return true;
    }

    public boolean stopWorkflow() {
        if (state != State.RUNNING) {
            return false;
        }
        if (!toWaitFor.cancel(true)) {
            return false;
        }
        for (var node : workflow.getNodes().values()) {
            var ns = getStateFor(node);
            //For all node that have not been yet finished, we set their state to failed and set the error for each input
            synchronized (ns) {
                if (ns.getState() != State.FINISHED || ns.getState() != State.FAILED) {
                    var errors = new WorkflowErrors();
                    errors.addError(new WorkflowCancelled());
                    var res = ResultOrWorkflowError.error(errors);

                    for (var input : node.getInputs().values()) {
                        ns.setInputValue(input.getId(), res);
                    }
                    changeNodeState(ns, State.FAILED);
                }
            }
        }
        synchronized (stateLock) {
            workflowErrors.addError(new WorkflowCancelled());
            state = State.FAILED;
            listener.workflowStateChanged(this);
        }
        return true;
    }

    public void setNodePosition(@Nonnull Node node, @Nonnull Point.Double pos) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(pos);

        var ns = getStateFor(node);
        synchronized (ns) {
            ns.setPosition(pos);
            listener.nodeStateChanged(ns);
        }
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public State getState() {
        return state;
    }

    public WorkflowErrors getWorkflowErrors() {
        return workflowErrors;
    }

    public void clearCache() {
        cache.clear();
    }

    public void delete() {
        clearCache();
        workflow.removeNodeModifiedListener(nodeModifiedListener);
    }
}
