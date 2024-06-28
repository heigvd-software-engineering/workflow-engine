package com.heig.services;

import com.google.gson.JsonElement;
import com.heig.entities.workflow.Workflow;
import com.heig.entities.workflow.execution.State;
import com.heig.entities.workflow.execution.WorkflowExecutionListener;
import com.heig.entities.workflow.execution.WorkflowExecutor;
import com.heig.entities.workflow.execution.WorkflowManager;
import com.heig.entities.workflow.connectors.Connector;
import com.heig.entities.workflow.connectors.InputConnector;
import com.heig.entities.workflow.connectors.OutputConnector;
import com.heig.entities.workflow.nodes.CodeNode;
import com.heig.entities.workflow.nodes.ModifiableNode;
import com.heig.entities.workflow.nodes.Node;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WPrimitive;
import com.heig.entities.workflow.types.WType;
import com.heig.entities.workflow.types.WorkflowTypes;
import com.heig.helpers.ResultOrStringError;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@ApplicationScoped
public class WorkflowService {
    private ResultOrStringError<WType> getWType(@Nonnull String wTypeStr) {
        Objects.requireNonNull(wTypeStr);

        try {
            return ResultOrStringError.result(WorkflowTypes.typeFromString(wTypeStr));
        } catch (Exception e) {
            return ResultOrStringError.error("Invalid primitiveType: " + e.getMessage());
        }
    }

    private ResultOrStringError<Workflow> getWorkflow(@Nonnull WorkflowExecutor we) {
        Objects.requireNonNull(we);

        return ResultOrStringError.result(we.getWorkflow());
    }

    private ResultOrStringError<WorkflowExecutor> getWorkflowExecutor(@Nonnull UUID workflowUUID) {
        Objects.requireNonNull(workflowUUID);

        var weOpt = WorkflowManager.getWorkflowExecutor(workflowUUID);
        return weOpt.map(ResultOrStringError::result).orElseGet(() -> ResultOrStringError.error("Workflow executor not found"));
    }

    private <T extends Node> ResultOrStringError<T> getNode(@Nonnull Workflow workflow, int nodeId, @Nonnull Class<T> clazz) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(clazz);

        var nodeOpt = workflow.getNode(nodeId);
        return nodeOpt.map(node -> {
            if (clazz.isInstance(node)) {
                return ResultOrStringError.result(clazz.cast(node));
            }
            return ResultOrStringError.<T>error("Incorrect node type");
        }).orElseGet(() -> ResultOrStringError.error("Node not found"));
    }

    private ResultOrStringError<InputConnector> getInputConnector(@Nonnull Node node, int inputConnectorId) {
        Objects.requireNonNull(node);

        var connectorOpt = node.getInput(inputConnectorId);
        return connectorOpt.map(ResultOrStringError::result).orElseGet(() -> ResultOrStringError.error("Input connector not found"));
    }

    private ResultOrStringError<OutputConnector> getOutputConnector(@Nonnull Node node, int outputConnectorId) {
        Objects.requireNonNull(node);

        var connectorOpt = node.getOutput(outputConnectorId);
        return connectorOpt.map(ResultOrStringError::result).orElseGet(() -> ResultOrStringError.error("Output connector not found"));
    }

    private <T> ResultOrStringError<T> ensureNotRunning(@Nonnull WorkflowExecutor workflowExecutor) {
        Objects.requireNonNull(workflowExecutor);

        if (workflowExecutor.getState() == State.RUNNING) {
            return ResultOrStringError.error("Workflow executor is currently running");
        }
        return ResultOrStringError.result(null);
    }

    private <T> ResultOrStringError<T> ensureNotRunning(@Nonnull Workflow workflow) {
        return getWorkflowExecutor(workflow.getUUID()).continueWith(this::ensureNotRunning);
    }

    public synchronized ResultOrStringError<Void> executeWorkflow(@Nonnull WorkflowExecutor workflowExecutor) {
        Objects.requireNonNull(workflowExecutor);

        return ensureNotRunning(workflowExecutor).continueWith(v -> {
            if (!workflowExecutor.executeWorkflow()) {
                return ResultOrStringError.error("Failed to execute workflow");
            }
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> removeWorkflowExecutor(@Nonnull WorkflowExecutor workflowExecutor) {
        Objects.requireNonNull(workflowExecutor);
        if (!WorkflowManager.removeWorkflowExecutor(workflowExecutor)) {
            return ResultOrStringError.error("Failed to remove the workflow executor");
        }
        return ResultOrStringError.result(null);
    }

    public synchronized ResultOrStringError<Node> createNode(@Nonnull Workflow workflow, @Nonnull String nodeType, String primitiveType) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(nodeType);

        return ensureNotRunning(workflow).continueWith(v -> {
            switch (nodeType) {
                case "primitive":
                    if (primitiveType == null) {
                        return ResultOrStringError.error("primitiveType not found");
                    }
                    return getWType(primitiveType).continueWith(wType -> {
                        if (wType instanceof WPrimitive wPrimitive) {
                            return ResultOrStringError.result(workflow.getNodeBuilder().buildPrimitiveNode(wPrimitive));
                        }
                        return ResultOrStringError.error("primitiveType should be an instance of WPrimitive");
                    });
                case "code":
                    return ResultOrStringError.result(workflow.getNodeBuilder().buildCodeNode());
                default:
                    return ResultOrStringError.error("Invalid node type");
            }
        });
    }

    public synchronized ResultOrStringError<Void> removeNode(@Nonnull Workflow workflow, @Nonnull Node node) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(node);

        return ensureNotRunning(workflow).continueWith(v -> {
            if (!workflow.removeNode(node)) {
                return ResultOrStringError.error("Failed to remove node");
            }
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Connector> createConnector(@Nonnull ModifiableNode mNode, boolean isInput, @Nonnull String name, @Nonnull String connectorType) {
        Objects.requireNonNull(mNode);
        Objects.requireNonNull(name);
        Objects.requireNonNull(connectorType);

        return ensureNotRunning(mNode.getWorkflow()).continueWith(v ->
            getWType(connectorType).continueWith(wType -> {
                if (isInput) {
                    return ResultOrStringError.result(mNode.getConnectorBuilder().buildInputConnector(name, wType));
                }
                return ResultOrStringError.result(mNode.getConnectorBuilder().buildOutputConnector(name, wType));
            })
        );
    }

    public synchronized ResultOrStringError<Void> removeConnector(@Nonnull ModifiableNode mNode, int connectorId, boolean isInput) {
        Objects.requireNonNull(mNode);

        return ensureNotRunning(mNode.getWorkflow()).continueWith(v ->
            isInput ?
                getInputConnector(mNode, connectorId).continueWith(input ->
                    mNode.removeInput(input) ?
                        ResultOrStringError.result(null) :
                        ResultOrStringError.error("Could not remove input")
                ) :
                getOutputConnector(mNode, connectorId).continueWith(output ->
                    mNode.removeOutput(output) ? ResultOrStringError.result(null) : ResultOrStringError.error("Could not remove output")
                )
        );
    }

    public synchronized ResultOrStringError<Void> changeConnectorType(@Nonnull Connector connector, @Nonnull String newType) {
        Objects.requireNonNull(connector);
        Objects.requireNonNull(newType);

        return ensureNotRunning(connector.getParent().getWorkflow()).continueWith(v ->
            getWType(newType).continueWith(wType ->
                connector.setType(wType)
                    .<ResultOrStringError<Void>>map(
                        workflowError -> ResultOrStringError.error("Failed to change the connector type: " + workflowError)
                    )
                    .orElseGet(
                        () -> ResultOrStringError.result(null)
                    )
            )
        );
    }

    public synchronized ResultOrStringError<Void> changeConnectorName(@Nonnull Connector connector, @Nonnull String newName) {
        Objects.requireNonNull(connector);
        Objects.requireNonNull(newName);

        return ensureNotRunning(connector.getParent().getWorkflow()).continueWith(v ->
            connector.setName(newName)
                .<ResultOrStringError<Void>>map(
                    workflowError -> ResultOrStringError.error("Failed to change the connector name: " + workflowError)
                )
                .orElseGet(
                    () -> ResultOrStringError.result(null)
                )
        );
    }

    public synchronized ResultOrStringError<Void> connect(@Nonnull OutputConnector from, @Nonnull InputConnector to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        var fromW = from.getParent().getWorkflow();
        var toW = to.getParent().getWorkflow();
        if (fromW != toW) {
            return ResultOrStringError.error("Trying to connect nodes from different workflows");
        }

        return ensureNotRunning(fromW).continueWith(v -> {
            if (!fromW.connect(from, to)) {
                return ResultOrStringError.error("Failed to connect the two connectors");
            }
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> disconnect(@Nonnull InputConnector connector) {
        Objects.requireNonNull(connector);

        return ensureNotRunning(connector.getParent().getWorkflow()).continueWith(v -> {
            if (!connector.getParent().getWorkflow().disconnect(connector)) {
                return ResultOrStringError.error("Failed to disconnect from connector");
            }
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> changeModifiableNodeTimeout(@Nonnull ModifiableNode mNode, int newNodeTimeout) {
        Objects.requireNonNull(mNode);

        return ensureNotRunning(mNode.getWorkflow()).continueWith(v -> {
            try {
                mNode.setTimeout(newNodeTimeout);
                return ResultOrStringError.result(null);
            } catch (Exception e) {
                return ResultOrStringError.error("Failed to change the node timeout : " + e.getMessage());
            }
        });
    }

    public synchronized ResultOrStringError<Void> changeModifiableNodeIsDeterministic(@Nonnull ModifiableNode mNode, boolean isDeterministic) {
        Objects.requireNonNull(mNode);

        return ensureNotRunning(mNode.getWorkflow()).continueWith(v -> {
            mNode.setIsDeterministic(isDeterministic);
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> changePrimitiveNodeValue(@Nonnull PrimitiveNode node, @Nonnull Object newValue) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(newValue);

        return ensureNotRunning(node.getWorkflow()).continueWith(v -> {
            try {
                node.setValue(newValue);
                return ResultOrStringError.result(null);
            } catch (Exception e) {
                return ResultOrStringError.error("Failed to change the primitive node value : " + e.getMessage());
            }
        });
    }

    public synchronized ResultOrStringError<Void> changeCodeNodeCode(@Nonnull CodeNode node, @Nonnull String newCode) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(newCode);

        return ensureNotRunning(node.getWorkflow()).continueWith(v -> {
            node.setCode(newCode);
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> changeCodeNodeLanguage(@Nonnull CodeNode node, @Nonnull String newLanguage) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(newLanguage);

        return ensureNotRunning(node.getWorkflow()).continueWith(v -> {
            var language = switch (newLanguage) {
                case "JS" -> CodeNode.Language.JS;
                default -> null;
            };
            if (language == null) {
                return ResultOrStringError.error("Language not found");
            }

            node.setLanguage(language);
            return ResultOrStringError.result(null);
        });
    }

    public synchronized Workflow createWorkflow(@Nonnull JsonElement nameJson) {
        Objects.requireNonNull(nameJson);
        return new Workflow(nameJson.getAsString());
    }

    public synchronized WorkflowExecutor createWorkflowExecutor(@Nonnull Workflow workflow, @Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(listener);
        return WorkflowManager.createWorkflowExecutor(workflow, listener);
    }

    public synchronized ResultOrStringError<Workflow> getWorkflow(@Nonnull JsonElement workflowUUIDJson) {
        Objects.requireNonNull(workflowUUIDJson);

        return getWorkflowExecutor(workflowUUIDJson).continueWith(this::getWorkflow);
    }

    public synchronized ResultOrStringError<WorkflowExecutor> getWorkflowExecutor(@Nonnull JsonElement workflowUUIDJson) {
        Objects.requireNonNull(workflowUUIDJson);

        return getUUID(workflowUUIDJson).continueWith(this::getWorkflowExecutor);
    }

    public synchronized <T extends Node> ResultOrStringError<T> getNode(@Nonnull Workflow workflow, @Nonnull JsonElement nodeIdJson, @Nonnull Class<T> clazz) {
        Objects.requireNonNull(workflow);
        Objects.requireNonNull(nodeIdJson);
        Objects.requireNonNull(clazz);

        var nodeId = nodeIdJson.getAsInt();
        return getNode(workflow, nodeId, clazz);
    }

    public synchronized ResultOrStringError<InputConnector> getInputConnector(@Nonnull Node node, @Nonnull JsonElement inputConnectorIdJson) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(inputConnectorIdJson);

        var connectorId = inputConnectorIdJson.getAsInt();
        return getInputConnector(node, connectorId);
    }

    public synchronized ResultOrStringError<OutputConnector> getOutputConnector(@Nonnull Node node, @Nonnull JsonElement outputConnectorIdJson) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(outputConnectorIdJson);

        var connectorId = outputConnectorIdJson.getAsInt();
        return getOutputConnector(node, connectorId);
    }

    public synchronized ResultOrStringError<UUID> getUUID(@Nonnull JsonElement UUIDJson) {
        Objects.requireNonNull(UUIDJson);

        try {
            return ResultOrStringError.result(UUID.fromString(UUIDJson.getAsString()));
        } catch (Exception e) {
            return ResultOrStringError.error("Error in the UUID : " + e.getMessage());
        }
    }
}
