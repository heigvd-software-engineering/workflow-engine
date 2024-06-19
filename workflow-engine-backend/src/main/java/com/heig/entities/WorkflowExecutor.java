package com.heig.entities;

import com.heig.entities.workflowErrors.FailedExecution;
import com.heig.entities.workflowErrors.WorkflowErrors;
import com.heig.entities.workflowErrors.WrongType;
import com.heig.helpers.ResultOrError;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorkflowExecutor {
    private final Object stateLock = new Object();
    private State state = State.IDLE;
    private final Workflow workflow;
    private final ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<>();
    private BiConsumer<Node, State> nodeStateChanged;
    private final WorkflowErrors workflowErrors = new WorkflowErrors();

    public WorkflowExecutor(Workflow workflow) {
        this.workflow = workflow;
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
            ResultOrError<Object> res;

            var error = false;
            var we = new WorkflowErrors();
            //To run a node, we need all inputs (except the ones marked as optional) values to be available with no error
            for (var input : node.getInputs().values()) {
                res = ns.getInputValue(input.getId());
                //The only way for res to be null is when it is an optional input
                if (res == null) {
                    continue;
                }
                if (res.getErrorMessage().isPresent()) {
                    error = true;
                    we.merge(res.getErrorMessage().get());
                }
            }
            if (error) {
                changeNodeState(node, State.FAILED);
                return ResultOrError.error(we);
            }

            changeNodeState(node, State.RUNNING);
            try {
                node.execute();
                changeNodeState(node, State.FINISHED);
                //TODO: Add checking the output type of the execute function
                //TODO: Create a class to pass values between the code and graal -> use the name of the connector
//                we.addError(new WrongType());
                res = ResultOrError.result(1);
            } catch (Exception e) {
                changeNodeState(node, State.FAILED);
                we.addError(new FailedExecution(node, e.getMessage()));
                res = ResultOrError.error(we);
            }
            return res;
        }).thenComposeAsync(o -> {
            var toWait = new LinkedList<CompletableFuture<Void>>();
            for (var output : node.getOutputs().values()) {
                for (var input : output.getConnectedTo()) {
                    var other = input.getParent();
                    var stateOther = getStateFor(other);
                    synchronized (stateOther) {
                        o.executePresent(
                            value -> stateOther.setInputValue(input.getId(), ResultOrError.result(1))
                            ,
                            error -> stateOther.setInputValue(input.getId(), ResultOrError.error(error))
                        );

                        if (stateOther.isReady()) {
                            toWait.add(executeNode(other));
                        }
                    }
                }
            }
            return CompletableFuture.allOf(toWait.toArray(CompletableFuture[]::new));
        });
    }

    public boolean executeWorkflow(Consumer<State> workflowStateChanged, BiConsumer<Node, State> nodeStateChanged) {
        workflowErrors.clear();
        this.nodeStateChanged = nodeStateChanged;
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
