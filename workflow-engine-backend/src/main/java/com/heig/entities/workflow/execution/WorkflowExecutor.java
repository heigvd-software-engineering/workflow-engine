package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.cache.Cache;
import com.heig.entities.workflow.errors.*;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.ResultOrWorkflowError;
import jakarta.annotation.Nonnull;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class WorkflowExecutor {
    private final Object stateLock = new Object();
    private State state = State.IDLE;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<>();
    private final WorkflowExecutionListener listener;
    private final WorkflowErrors workflowErrors = new WorkflowErrors();
    private final Cache cache;

    public WorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener listener) {
        this.workflow = Objects.requireNonNull(workflow);
        this.listener = Objects.requireNonNull(listener);
        this.cache = Cache.get(workflow);
    }

    private NodeState getStateFor(@Nonnull Node node) {
        Objects.requireNonNull(node);
        return states.computeIfAbsent(node.getId(), id -> new NodeState(node));
    }

    private void changeNodeState(@Nonnull Node node, @Nonnull State state) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(state);
        var ns = getStateFor(node);
        ns.setState(state);
        listener.nodeStateChanged(node, state);
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

                if (inputValue.getErrorMessage().isPresent()) {
                    error = true;
                    we.addError(new ErroredInputConnector(input));
                } else {
                    args.putArgument(input.getName(), inputValue.getResult().get());
                }
            }
            if (error) {
                changeNodeState(node, State.FAILED);
                return ResultOrWorkflowError.error(we);
            }

            //Case if the node hasn't been modified and neither have the previous nodes (the node needs to be deterministic too)
            if (!ns.hasBeenModified() && node.isDeterministic()) {
                var optCache = cache.get(node, args);
                if (optCache.isPresent()) {
                    return ResultOrWorkflowError.result(optCache.get());
                }
            }

            changeNodeState(node, State.RUNNING);
            try {
                var fut = CompletableFuture
                    .supplyAsync(() -> Optional.of(node.execute(args)))
                    .completeOnTimeout(Optional.empty(), node.getTimeout(), TimeUnit.MILLISECONDS);
                var resultOpt = fut.join();
                if (resultOpt.isEmpty()) {
                    we.addError(new ExecutionTimeout(node));
                    return ResultOrWorkflowError.error(we);
                }
                var result = resultOpt.get();

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
                    changeNodeState(node, State.FAILED);
                    return ResultOrWorkflowError.error(we);
                } else {
                    if (node.isDeterministic()) {
                        ns.setHasBeenModified(false);
                        cache.set(node, args, result);
                    }

                    changeNodeState(node, State.FINISHED);
                    return ResultOrWorkflowError.result(result);
                }
            } catch (Exception e) {
                changeNodeState(node, State.FAILED);
                we.addError(new FailedExecution(node, e.getMessage()));
                return ResultOrWorkflowError.error(we);
            }
        }).thenComposeAsync(o -> {
            var toWait = new LinkedList<CompletableFuture<Void>>();
            for (var output : node.getOutputs().values()) {
                var res = o.apply(
                    value -> {
                        var opt = value.getArgument(output.getName());
                        if (opt.isPresent()) {
                            return ResultOrWorkflowError.result(opt.get());
                        }
                        throw new RuntimeException("Should never happen! Already checked before");
                    }, ResultOrWorkflowError::error
                );

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

    public boolean executeWorkflow() {
        workflowErrors.clear();
        synchronized (stateLock) {
            if (state == State.RUNNING) {
                return false;
            }
            state = State.RUNNING;
            listener.workflowStateChanged(state);
        }

        var errors = workflow.isValid();
        if (errors.isPresent()) {
            workflowErrors.merge(errors.get());
            synchronized (stateLock) {
                state = State.FAILED;
                listener.workflowStateChanged(state);
            }
            return false;
        }

        workflow.getNodes().values().forEach(n -> {
            var ns = getStateFor(n);
            ns.setState(State.IDLE);
            ns.clearInputs();
        });

        //The starting nodes of our workflow are the nodes that have no input connected (we already know that all inputs
        //that are not optional are connected to an output thanks to workflow.isValid())
        var notConnectedNodes = workflow.getNodes().values().stream().filter(n -> n.getInputs().values().stream().noneMatch(i -> i.getConnectedTo().isPresent())).toList();
        var toWait = new LinkedList<CompletableFuture<Void>>();
        for (var node : notConnectedNodes) {
            toWait.add(executeNode(node));
        }
        var waitAll = CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        waitAll.thenAcceptAsync(v -> {
            var failed = states.values().stream().anyMatch(ns -> ns.getState() == State.FAILED);
            if (failed) {
                synchronized (stateLock) {
                    state = State.FAILED;
                    listener.workflowStateChanged(state);
                }
            } else {
                synchronized (stateLock) {
                    state = State.FINISHED;
                    listener.workflowStateChanged(state);
                }
            }
        });
        return true;
    }

    public WorkflowErrors getWorkflowErrors() {
        return workflowErrors;
    }

    public void clearCache() {
        cache.clear();
    }
}
