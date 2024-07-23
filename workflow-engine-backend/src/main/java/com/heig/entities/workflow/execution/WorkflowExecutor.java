package com.heig.entities.workflow.execution;

import com.google.gson.*;
import com.heig.entities.workflow.NodeModifiedListener;
import com.heig.entities.workflow.data.Data;
import com.heig.entities.workflow.errors.*;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.CustomJsonDeserializer;
import com.heig.helpers.CustomJsonSerializer;
import com.heig.helpers.ResultOrWorkflowError;
import com.heig.helpers.Utils;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Handles the execution of the workflow
 */
public class WorkflowExecutor {
    /**
     * Used to convert a {@link WorkflowExecutor} to its json representation
     */
    public static class Serializer implements CustomJsonSerializer<WorkflowExecutor> {
        @Override
        public JsonElement serialize(WorkflowExecutor workflowExecutor) {
            var obj = new JsonObject();
            var w = workflowExecutor.getWorkflow();
            obj.add("workflow", new Workflow.Serializer().serialize(w));
            obj.add("nodeStates", Utils.serializeList(new NodeState.Serializer(), w.getNodes().values().stream().map(workflowExecutor::getStateFor).toList()));
            return obj;
        }
    }

    /**
     * Used to convert json to a {@link WorkflowExecutor}
     */
    public static class Deserializer implements CustomJsonDeserializer<WorkflowExecutor> {
        private final WorkflowExecutionListener listener;
        private final Data data;
        public Deserializer(@Nonnull Data data, @Nonnull WorkflowExecutionListener listener) {
            this.listener = Objects.requireNonNull(listener);
            this.data = Objects.requireNonNull(data);
        }

        @Override
        public WorkflowExecutor deserialize(JsonElement jsonElement) throws JsonParseException {
            var obj = jsonElement.getAsJsonObject();
            var workflow = new Workflow.Deserializer().deserialize(obj.get("workflow"));
            var nodeStates = Utils.deserializeList(new NodeState.Deserializer(workflow), obj.get("nodeStates").getAsJsonArray());
            return new WorkflowExecutor(workflow, listener, data, nodeStates);
        }
    }

    /**
     * Object used to synchronize the {@link State} of this {@link WorkflowExecutor}
     */
    private final Object stateLock = new Object();

    /**
     * The {@link State}
     */
    private State state = State.IDLE;

    /**
     * The {@link Workflow} linked to this {@link WorkflowExecutor}
     */
    private final Workflow workflow;

    /**
     * Stores the {@link NodeState} for each {@link Node} (here represented by its id)
     */
    private final ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<>();

    /**
     * The listener to notify when a change happens
     */
    private final WorkflowExecutionListener listener;

    /**
     * The execution errors
     */
    private final WorkflowErrors workflowExecutionErrors = new WorkflowErrors();

    /**
     * The validity errors
     */
    private final WorkflowErrors workflowValidityErrors = new WorkflowErrors();

    /**
     * The {@link Data} object
     */
    private final Data data;

    /**
     * The listener to notify when a node has been modified
     */
    private final NodeModifiedListener nodeModifiedListener;

    /**
     * True if a stop has been requested by the user, false otherwise
     */
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    /**
     * List of all the {@link Runnable} to start if a stop has been requested
     */
    private final ConcurrentHashSet<Runnable> waitingForStop = new ConcurrentHashSet<>();

    WorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener listener) {
        this(workflow, listener, null, new LinkedList<>());
    }

    WorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener listener, Data data, @Nonnull List<NodeState> states) {
        this.workflow = Objects.requireNonNull(workflow);
        this.listener = Objects.requireNonNull(listener);
        Objects.requireNonNull(states);

        for (var nodeState : states) {
            this.states.put(nodeState.getNode().getId(), nodeState);
        }

        if (data == null) {
            this.data = Data.getOrCreate(this);
        } else {
            this.data = data;
        }

        //When a node has been modified, we set that the NodeState linked has been modified
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

    public void removeStateFor(@Nonnull Node node) {
        states.remove(node.getId());
    }

    /**
     * Changes the state for a {@link Node} and notifies the {@link WorkflowExecutionListener}
     * @param ns The {@link NodeState} to change
     * @param state The new {@link State}
     */
    private void changeNodeState(@Nonnull NodeState ns, @Nonnull State state) {
        Objects.requireNonNull(ns);
        Objects.requireNonNull(state);
        ns.setState(state);
        listener.nodeStateChanged(ns);
    }

    /**
     * The thread pool to execute the tasks
     */
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Executes the node
     * @param node The {@link Node} to execute
     * @return A future that completes when the {@link Node} and all the nodes depending on this node are executed
     */
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
                var optCache = data.getCache().get(node, args);
                if (optCache.isPresent()) {
                    return ResultOrWorkflowError.result(optCache.get());
                }
            }

            synchronized (ns) {
                changeNodeState(ns, State.RUNNING);
            }

            if (stopRequested.get()) {
                we.addError(new FailedExecution(node, "A stop has been requested"));
                return ResultOrWorkflowError.error(we);
            }

            var fut = executor
                .<ResultOrWorkflowError<NodeArguments>>submit(() -> {
                    try {
                        return ResultOrWorkflowError.result(node.execute(args, listener::newLogLine));
                    } catch (Exception e) {
                        we.addError(new FailedExecution(node, e.getMessage() == null ? "Unknown error" : e.getMessage()));
                        return ResultOrWorkflowError.error(we);
                    }
                });
            Runnable listener = node::cancel;
            waitingForStop.add(listener);
            if (stopRequested.get()) {
                node.cancel();
            }

            ResultOrWorkflowError<NodeArguments> resultOpt;
            try {
                resultOpt = fut.get(node.getTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                waitingForStop.remove(listener);
                var otherErrors = new WorkflowErrors();
                if (e instanceof TimeoutException) {
                    otherErrors.addError(new ExecutionTimeout(node));
                } else {
                    otherErrors.addError(new FailedExecution(node, e.getMessage() == null ? "Unknown error" : e.getMessage()));
                }
                node.cancel();
                return ResultOrWorkflowError.error(otherErrors);
            }

            waitingForStop.remove(listener);
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

                var fixedValue = WorkflowTypes.fixObject(argumentValue);
                result.putArgument(output.getName(), fixedValue);

                var argumentType = WorkflowTypes.fromObject(fixedValue);
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
                    try {
                        data.getCache().set(node, args, result);
                    } catch (Exception e) {
                        we.addError(new FailedExecution(node, e.getMessage()));
                        return ResultOrWorkflowError.error(we);
                    }
                    ns.setHasBeenModified(false);
                }

                return ResultOrWorkflowError.result(result);
            }
        }).thenApplyAsync(o -> {
            var ns = getStateFor(node);
            synchronized (ns) {
                o.execute(value -> {
                    changeNodeState(ns, State.FINISHED);
                }, we -> {
                    ns.setErrors(we);
                    changeNodeState(ns, State.FAILED);
                });
            }
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
                        //Here we use fixObject again to obtain new instances for the map and the collection for example
                        //If this wasn't done, all the nodes connected to the outputs would have the same instance and could lead to concurrent modification
                        if (res != null) {
                            res = WorkflowTypes.fixObject(res);
                        }
                        stateOther.setInputValue(input.getId(), res);

                        if (stateOther.isReady()) {
                            toWait.add(executeNode(other));
                        }
                    }
                }
            }

            //Clean the resources when the workflow execution is finished
            node.cancel();
            return CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        });
    }

    /**
     * Checks if the current workflow is valid. Notifies that the state of the workflow has changed to the {@link WorkflowExecutionListener}.
     */
    public void checkForErrors() {
        workflowValidityErrors.clear();

        var errors = workflow.isValid();
        errors.ifPresent(workflowValidityErrors::merge);
        //Notify because the workflowValidityErrors have changed
        listener.workflowStateChanged(this);
    }

    /**
     * Executes the workflow
     * @return True if the execution has been correctly started, false otherwise
     */
    public boolean executeWorkflow() {
        synchronized (stateLock) {
            if (state == State.RUNNING) {
                return false;
            }
            state = State.RUNNING;
            listener.workflowStateChanged(this);
        }

        stopRequested.set(false);

        checkForErrors();
        //If a validity error is present, we stop here
        if (!workflowValidityErrors.getErrors().isEmpty()) {
            state = State.IDLE;
            listener.workflowStateChanged(this);
            return false;
        }

        //Resets the state of the entire workflow
        listener.clearLog();
        workflowExecutionErrors.clear();
        workflow.getNodes().values().forEach(n -> {
            var ns = getStateFor(n);
            synchronized (ns) {
                ns.getErrors().ifPresent(WorkflowErrors::clear);
                ns.clearInputs();
                changeNodeState(ns, State.IDLE);
            }
        });

        //Before starting the workflow, we save the current workflow.
        //Like this, if the cache changed, we will have the correct workflow when restarting the backend
        data.getSave().save();

        //The starting nodes of our workflow are the nodes that have no input connected (we already know that all inputs
        //that are not optional are connected to an output thanks to workflow.isValid())
        var notConnectedNodes = workflow.getNodes().values().stream().filter(n -> n.getInputs().values().stream().noneMatch(i -> i.getConnectedTo().isPresent())).toList();
        var toWait = new LinkedList<CompletableFuture<Void>>();
        for (var node : notConnectedNodes) {
            toWait.add(executeNode(node));
        }
        var toWaitFor = CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        toWaitFor.thenAcceptAsync(v -> {
            var failed = states.values().stream().anyMatch(ns -> ns.getState() == State.FAILED);

            if (stopRequested.get()) {
                workflowExecutionErrors.addError(new WorkflowCancelled());
                failed = true;
            }

            //When the execution ends, we save the workflow again. It is possible that the cache has changed and that the node state too
            data.getSave().save();

            synchronized (stateLock) {
                if (failed) {
                    state = State.FAILED;
                } else {
                    state = State.FINISHED;
                }
                listener.workflowStateChanged(this);
            }
        });
        return true;
    }

    /**
     * Requests a stop of the workflow
     * @return False if the workflow is not running, true otherwise
     */
    public boolean stopWorkflow() {
        if (state != State.RUNNING) {
            return false;
        }
        stopRequested.set(true);
        for (var listener : waitingForStop) {
            listener.run();
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

    /**
     * Returns the workflow errors (execution + validity errors)
     * @return The workflow errors
     */
    public WorkflowErrors getWorkflowErrors() {
        var all = new WorkflowErrors();
        all.merge(workflowExecutionErrors);
        all.merge(workflowValidityErrors);
        return all;
    }

    /**
     * Clears the cache for the workflow
     */
    public void clearCache() {
        data.getCache().clear();
    }

    /**
     * Deletes the data directory for the current workflow and removed the {@link NodeModifiedListener} from the workflow
     */
    public void delete() {
        data.delete();
        workflow.removeNodeModifiedListener(nodeModifiedListener);
    }

    public Map<Integer, NodeState> getNodeStates() {
        return Collections.unmodifiableMap(states);
    }
}
