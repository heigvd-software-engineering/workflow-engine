package com.heig.entities.workflow.execution;

import com.heig.entities.workflow.errors.FailedExecution;
import com.heig.entities.workflow.errors.MissingOutputValue;
import com.heig.entities.workflow.errors.WorkflowErrors;
import com.heig.entities.workflow.errors.WrongType;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.ResultOrError;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorkflowExecutor {
    private final Object stateLock = new Object();
    private State state = State.IDLE;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<>();
    private final BiConsumer<Node, State> nodeStateChanged;
    private final Consumer<State> workflowStateChanged;
    private final WorkflowErrors workflowErrors = new WorkflowErrors();

    public WorkflowExecutor(Workflow workflow, Consumer<State> workflowStateChanged, BiConsumer<Node, State> nodeStateChanged) {
        this.workflow = workflow;
        this.workflowStateChanged = workflowStateChanged;
        this.nodeStateChanged = nodeStateChanged;
    }

    private NodeState getStateFor(Node node) {
        return states.computeIfAbsent(node.getId(), id -> new NodeState(node));
    }

    private void changeNodeState(Node node, State state) {
        var ns = getStateFor(node);
        ns.setState(state);
        nodeStateChanged.accept(node, state);
    }

    private CompletableFuture<Void> executeNode(Node node) {
        return CompletableFuture.supplyAsync(() -> {
            var ns = getStateFor(node);
            var error = false;
            var we = new WorkflowErrors();
            var args = new NodeArguments();
            //To run a node, we need all inputs (except the ones marked as optional) values to be available with no error
            for (var input : node.getInputs().values()) {
                var inputValue = ns.getInputValue(input.getId());
                //The only way for res to be null is when it is an optional input
                if (inputValue == null) {
                    continue;
                }
                if (inputValue.getErrorMessage().isPresent()) {
                    error = true;
                    we.merge(inputValue.getErrorMessage().get());
                } else {
                    args.putArgument(input.getName(), inputValue.getResult().get());
                }
            }
            if (error) {
                changeNodeState(node, State.FAILED);
                return ResultOrError.<NodeArguments>error(we);
            }

            changeNodeState(node, State.RUNNING);
            try {
                var result = node.execute(args);
                var isErrored = false;
                for (var output : node.getOutputs().values()) {
                    var argument = result.getArgument(output.getName());
                    if (argument.isEmpty()) {
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
                    return ResultOrError.<NodeArguments>error(we);
                } else {
                    changeNodeState(node, State.FINISHED);
                    return ResultOrError.result(result);
                }
            } catch (Exception e) {
                changeNodeState(node, State.FAILED);
                we.addError(new FailedExecution(node, e.getMessage()));
                return ResultOrError.<NodeArguments>error(we);
            }
        }).thenComposeAsync(o -> {
            var toWait = new LinkedList<CompletableFuture<Void>>();
            for (var output : node.getOutputs().values()) {
                AtomicReference<ResultOrError<Object>> res = new AtomicReference<>();
                o.executePresent(
                    value -> res.set(ResultOrError.result(value.getArgument(output.getName()).get())),
                    errorMessage -> res.set(ResultOrError.error(errorMessage))
                );

                for (var input : output.getConnectedTo()) {
                    var other = input.getParent();
                    var stateOther = getStateFor(other);
                    synchronized (stateOther) {
                        stateOther.setInputValue(input.getId(), res.get());

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
            workflowStateChanged.accept(state);
        }

        var errors = workflow.isValid();
        if (errors.isPresent()) {
            workflowErrors.merge(errors.get());
            synchronized (stateLock) {
                state = State.FAILED;
                workflowStateChanged.accept(state);
            }
            return false;
        }

        //The starting nodes of our workflow are the nodes that have no input connected (we already know that all inputs
        //that are not optional are connected to an output thanks to workflow.isValid())
        var noInputsNodes = workflow.getNodes().values().stream().filter(n -> n.getInputs().values().stream().noneMatch(i -> i.getConnectedTo().isPresent())).toList();
        var toWait = new LinkedList<CompletableFuture<Void>>();
        for (var node : noInputsNodes) {
            toWait.add(executeNode(node));
        }
        var waitAll = CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        waitAll.thenAcceptAsync(v -> {
            var failed = states.values().stream().anyMatch(ns -> ns.getState() == State.FAILED);
            if (failed) {
                synchronized (stateLock) {
                    state = State.FAILED;
                    workflowStateChanged.accept(state);
                }
            } else {
                synchronized (stateLock) {
                    state = State.FINISHED;
                    workflowStateChanged.accept(state);
                }
            }
        });
        return true;
    }

    public WorkflowErrors getWorkflowErrors() {
        return workflowErrors;
    }
}
