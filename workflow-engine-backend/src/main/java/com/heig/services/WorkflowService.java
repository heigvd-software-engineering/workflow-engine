package com.heig.services;

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

    private ResultOrStringError<Workflow> getWorkflow(@Nonnull String workflowUUID) {
        Objects.requireNonNull(workflowUUID);

        return getWorkflowExecutor(workflowUUID).continueWith(we -> {
            if (we.getState() == State.RUNNING) {
                return ResultOrStringError.error("The workflow is currently running !");
            }
            return ResultOrStringError.result(we.getWorkflow());
        });
    }

    private ResultOrStringError<WorkflowExecutor> getWorkflowExecutor(@Nonnull String workflowUUID) {
        Objects.requireNonNull(workflowUUID);

        var weOpt = WorkflowManager.getWorkflowExecutor(UUID.fromString(workflowUUID));
        return weOpt.map(ResultOrStringError::result).orElseGet(() -> ResultOrStringError.error("Workflow executor not found"));
    }

    private <T extends Node> ResultOrStringError<T> getNode(@Nonnull String workflowUUID, int nodeId, @Nonnull Class<T> clazz) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(clazz);

        var wOpt = getWorkflow(workflowUUID);
        return wOpt.continueWith(w -> getNode(w, nodeId, clazz));
    }

    private ResultOrStringError<Node> getNode(@Nonnull String workflowUUID, int nodeId) {
        return getNode(workflowUUID, nodeId, Node.class);
    }

    private <T extends Node> ResultOrStringError<T> getNode(@Nonnull Workflow workflow, int nodeId, Class<T> clazz) {
        Objects.requireNonNull(workflow);

        var nodeOpt = workflow.getNode(nodeId);
        return nodeOpt.map(node -> {
            if (clazz.isInstance(node)) {
                return ResultOrStringError.result(clazz.cast(node));
            }
            return ResultOrStringError.<T>error("Incorrect node type");
        }).orElseGet(() -> ResultOrStringError.error("Node not found"));
    }

    private ResultOrStringError<Node> getNode(@Nonnull Workflow workflow, int nodeId) {
        return getNode(workflow, nodeId, Node.class);
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

    private ResultOrStringError<InputConnector> getInputConnector(@Nonnull Workflow workflow, int nodeId, int inputConnectorId) {
        Objects.requireNonNull(workflow);

        return getNode(workflow, nodeId).continueWith(node -> getInputConnector(node, inputConnectorId));
    }

    private ResultOrStringError<OutputConnector> getOutputConnector(@Nonnull Workflow workflow, int nodeId, int outputConnectorId) {
        Objects.requireNonNull(workflow);

        return getNode(workflow, nodeId).continueWith(node -> getOutputConnector(node, outputConnectorId));
    }

    private ResultOrStringError<InputConnector> getInputConnector(@Nonnull String workflowUUID, int nodeId, int inputConnectorId) {
        Objects.requireNonNull(workflowUUID);

        return getNode(workflowUUID, nodeId).continueWith(node -> getInputConnector(node, inputConnectorId));
    }

    private ResultOrStringError<OutputConnector> getOutputConnector(@Nonnull String workflowUUID, int nodeId, int outputConnectorId) {
        Objects.requireNonNull(workflowUUID);

        return getNode(workflowUUID, nodeId).continueWith(node -> getOutputConnector(node, outputConnectorId));
    }

    private ResultOrStringError<Void> changeConnector(@Nonnull String workflowUUID, int nodeId, int connectorId, boolean isInput, @Nonnull Function<Connector, ResultOrStringError<Void>> modifier) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(modifier);

        if (isInput) {
            return getInputConnector(workflowUUID, nodeId, connectorId).continueWith(modifier::apply);
        }
        return getOutputConnector(workflowUUID, nodeId, connectorId).continueWith(modifier::apply);
    }

    private ResultOrStringError<Void> changeModifiableNode(@Nonnull String workflowUUID, int nodeId, @Nonnull Function<ModifiableNode, ResultOrStringError<Void>> modifier) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(modifier);

        return getNode(workflowUUID, nodeId, ModifiableNode.class).continueWith(modifier);
    }

    public synchronized WorkflowExecutor createWorkflowExecutor(@Nonnull String name, @Nonnull WorkflowExecutionListener listener) {
        Objects.requireNonNull(name);
        return WorkflowManager.createWorkflowExecutor(new Workflow(name), listener);
    }

    public synchronized ResultOrStringError<Void> executeWorkflow(@Nonnull String workflowUUID) {
        Objects.requireNonNull(workflowUUID);
        return WorkflowManager.getWorkflowExecutor(UUID.fromString(workflowUUID)).map(we -> {
            if (!we.executeWorkflow()) {
                return ResultOrStringError.<Void>error("Failed to execute workflow");
            }
            return ResultOrStringError.<Void>result(null);
        }).orElse(ResultOrStringError.error(""));
    }

    public synchronized ResultOrStringError<Void> removeWorkflowExecutor(@Nonnull String workflowUUID) {
        Objects.requireNonNull(workflowUUID);
        return getWorkflowExecutor(workflowUUID).continueWith(we -> {
            if (!WorkflowManager.removeWorkflowExecutor(we)) {
                return ResultOrStringError.error("Failed to remove the workflow executor");
            }
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Node> createNode(@Nonnull String workflowUUID, @Nonnull String nodeType, String primitiveType) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(nodeType);

        var wOpt = getWorkflow(workflowUUID);
        return wOpt.continueWith(w -> {
            switch (nodeType) {
                case "primitive":
                    if (primitiveType == null) {
                        return ResultOrStringError.error("primitiveType not found");
                    }
                    return getWType(primitiveType).continueWith(wType -> {
                        if (wType instanceof WPrimitive wPrimitive) {
                            return ResultOrStringError.result(w.getNodeBuilder().buildPrimitiveNode(wPrimitive));
                        }
                        return ResultOrStringError.error("primitiveType should be an instance of WPrimitive");
                    });
                case "code":
                    return ResultOrStringError.result(w.getNodeBuilder().buildCodeNode());
                default:
                    return ResultOrStringError.error("Invalid node type");
            }
        });
    }

    public synchronized ResultOrStringError<Void> removeNode(@Nonnull String workflowUUID, int nodeId) {
        Objects.requireNonNull(workflowUUID);

        return getWorkflow(workflowUUID).continueWith(w ->
           getNode(w, nodeId).continueWith(node -> {
              if (!w.removeNode(node)) {
                  return ResultOrStringError.error("Failed to remove node");
              }
              return ResultOrStringError.result(null);
           })
        );
    }

    public synchronized ResultOrStringError<Connector> createConnector(@Nonnull String workflowUUID, int nodeId, boolean isInput, @Nonnull String name, @Nonnull String connectorType) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(name);
        Objects.requireNonNull(connectorType);

        return getNode(workflowUUID, nodeId, ModifiableNode.class).continueWith(node ->
            getWType(connectorType).continueWith(wType -> {
                if (isInput) {
                    return ResultOrStringError.result(node.getConnectorBuilder().buildInputConnector(name, wType));
                }
                return ResultOrStringError.result(node.getConnectorBuilder().buildOutputConnector(name, wType));
            })
        );
    }

    public synchronized ResultOrStringError<Void> removeConnector(@Nonnull String workflowUUID, int nodeId, int connectorId, boolean isInput) {
        return getNode(workflowUUID, nodeId, ModifiableNode.class).continueWith(node -> isInput ?
            getInputConnector(node, connectorId).continueWith(input ->
                node.removeInput(input) ?
                    ResultOrStringError.result(null) :
                    ResultOrStringError.error("Could not remove input")
            ) :
            getOutputConnector(node, connectorId).continueWith(output ->
                node.removeOutput(output) ? ResultOrStringError.result(null) : ResultOrStringError.error("Could not remove output")
            )
        );
    }

    public synchronized ResultOrStringError<Void> changeConnectorType(@Nonnull String workflowUUID, int nodeId, int connectorId, boolean isInput, @Nonnull String newType) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(newType);

        return changeConnector(workflowUUID, nodeId, connectorId, isInput, connector ->
            getWType(newType).continueWith(wType -> {
                if (connector.setType(wType).isPresent()) {
                    return ResultOrStringError.error("Failed to change the connector type");
                }
                return ResultOrStringError.result(null);
            })
        );
    }

    public synchronized ResultOrStringError<Void> changeConnectorName(@Nonnull String workflowUUID, int nodeId, int connectorId, boolean isInput, @Nonnull String newName) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(newName);

        return changeConnector(workflowUUID, nodeId, connectorId, isInput, connector -> connector.setName(newName).isPresent() ?
            ResultOrStringError.error("Failed to change the connector name") :
            ResultOrStringError.result(null)
        );
    }

    public synchronized ResultOrStringError<Void> connect(@Nonnull String workflowUUID, int nodeIdFrom, int connectorIdFrom, int nodeIdTo, int connectorIdTo) {
        Objects.requireNonNull(workflowUUID);

        return getWorkflow(workflowUUID).continueWith(w ->
            getOutputConnector(w, nodeIdFrom, connectorIdFrom).continueWith(from ->
                getInputConnector(w, nodeIdTo, connectorIdTo).continueWith(to -> {
                    if (!w.connect(from, to)) {
                        return ResultOrStringError.error("Failed to connect the two connectors");
                    }
                    return ResultOrStringError.result(null);
                })
            )
        );
    }

    public synchronized ResultOrStringError<Void> disconnect(@Nonnull String workflowUUID, int nodeId, int inputConnectorId) {
        Objects.requireNonNull(workflowUUID);

        return getWorkflow(workflowUUID).continueWith(w ->
           getInputConnector(w, nodeId, inputConnectorId).continueWith(connector -> {
               w.disconnect(connector);
              return ResultOrStringError.result(null);
           })
        );
    }

    public synchronized ResultOrStringError<Void> changeModifiableNodeTimeout(@Nonnull String workflowUUID, int nodeId, int newNodeTimeout) {
        Objects.requireNonNull(workflowUUID);

        return changeModifiableNode(workflowUUID, nodeId, mNode -> {
            try {
                mNode.setTimeout(newNodeTimeout);
                return ResultOrStringError.result(null);
            } catch (Exception e) {
                return ResultOrStringError.error("Failed to change the node timeout : " + e.getMessage());
            }
        });
    }

    public synchronized ResultOrStringError<Void> changeModifiableNodeIsDeterministic(@Nonnull String workflowUUID, int nodeId, boolean isDeterministic) {
        Objects.requireNonNull(workflowUUID);

        return changeModifiableNode(workflowUUID, nodeId, mNode -> {
            mNode.setIsDeterministic(isDeterministic);
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> changePrimitiveNodeValue(@Nonnull String workflowUUID, int nodeId, @Nonnull Object newValue) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(newValue);

        return getNode(workflowUUID, nodeId, PrimitiveNode.class).continueWith(node -> {
            try {
                node.setValue(newValue);
                return ResultOrStringError.result(null);
            } catch (Exception e) {
                return ResultOrStringError.error("Failed to change the node value : " + e.getMessage());
            }
        });
    }

    public synchronized ResultOrStringError<Void> changeCodeNodeCode(@Nonnull String workflowUUID, int nodeId, @Nonnull String newCode) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(newCode);

        return getNode(workflowUUID, nodeId, CodeNode.class).continueWith(node -> {
            node.setCode(newCode);
            return ResultOrStringError.result(null);
        });
    }

    public synchronized ResultOrStringError<Void> changeCodeNodeLanguage(@Nonnull String workflowUUID, int nodeId, @Nonnull String newLanguage) {
        Objects.requireNonNull(workflowUUID);
        Objects.requireNonNull(newLanguage);

        return getNode(workflowUUID, nodeId, CodeNode.class).continueWith(node -> {
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
}
