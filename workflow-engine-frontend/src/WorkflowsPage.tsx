import { Edge, Node, NodePositionChange, addEdge, applyNodeChanges, useReactFlow, XYPosition } from "@xyflow/react";
import Layout from "./Layout";
import '@xyflow/react/dist/style.css';
import { PrimitiveNodeTypeNode } from "./nodes/PrimitiveNode";
import { useCallback, useMemo, useState } from "react";
import { Button, Dialog, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Select, SelectChangeEvent, TextField } from "@mui/material";
import { useAlert } from "./utils/alert/AlertUse";
import { ICreateCodeNode, ICreateNode, ICreatePrimitiveNode, ICreateWorkflow, IDisconnect, IExecuteWorkflow, IRemoveNode, ISwitchTo, PrimitiveTypes, WorkflowGeneralErrors, WorkflowNodeErrors, WorkflowNotification } from "./types/Types";
import { AddCircle, PlayArrow } from "@mui/icons-material";
import useWebSocket from "react-use-websocket";
import { $enum } from "ts-enum-util";
import { CodeNodeTypeNode } from "./nodes/CodeNode";
import { BaseNodeData, BaseNodeTypeNode } from "./nodes/BaseNode";
import ContextMenuProvider, { ContextMenuVariants } from "./utils/contextMenu/ContextMenuProvider";
import ReactFlowGraph from "./components/ReactFlowGraph";
import { useWorkflowData } from "./utils/workflowData/WorkflowDataUse";
import WorkflowSocketProvider from "./utils/workflowSocket/WorkflowSocketProvider";
import ErrorPopover from "./components/ErrorPopover";

export default function WorkflowsPage() {
  const [workflowGeneralErrors, setWorkflowGeneralErrors] = useState<WorkflowGeneralErrors[]>([]);

  const { screenToFlowPosition } = useReactFlow();
  const { setNodes, setEdges, setWorkflows, setWorkflow, workflow, workflows } = useWorkflowData();
  const { alertSuccess, alertError, alertInfo } = useAlert();
  const { sendJsonMessage } = useWebSocket<WorkflowNotification>("ws://localhost:8080/workflow", { share: false, shouldReconnect: () => false, 
    onOpen: () => {
      alertSuccess("Connected to the websocket"); 
    },
    onClose: () => {
      alertInfo("Websocket connexion closed");
    },
    onMessage: (lastJsonMessage) => {
      const notification: WorkflowNotification = JSON.parse(lastJsonMessage.data);
      switch (notification.notificationType) {
        case "switchedTo": {
          setWorkflows(workflowsCurrent => {
            setWorkflow(workflowsCurrent.find(w => w.uuid == notification.uuid));
            return workflowsCurrent;
          })
          setNodes([]);
          setEdges([]);
          break;
        }
        case "workflows": {
          setWorkflows(notification.workflows)
          break;
        }
        case "newWorkflow": {
          setWorkflows(w => [...w, notification.workflow])
          break;
        }
        case "deletedWorkflow": {
          setWorkflows(w => w.filter(rem => rem.uuid != notification.workflowUUID))
          break;
        }
        case "node": {
          setNodes(currentNodes => {
            if (workflow == undefined) {
              alertError("No workflow selected !");
              return currentNodes;
            }
  
            const foundNodeIdx = currentNodes.findIndex(n => n.id == notification.node.id.toString());
            let wasCreated = true;
            let oldNode: BaseNodeTypeNode | undefined = undefined;
            if (foundNodeIdx != -1) {
              oldNode = {...currentNodes[foundNodeIdx]};
              currentNodes = currentNodes.filter((_, i) => i != foundNodeIdx);
              wasCreated = false;
            }
            const recvNode = notification.node;
            let nodeF: Node<BaseNodeData>;
            if (wasCreated) {
              nodeF = {
                id: recvNode.id.toString(),
                position: {
                  x: -999999,
                  y: -999999
                },
                data: {
                  node: recvNode,
                  uuid: workflow.uuid,
                  sendToWebsocket: sendJsonMessage,
                  errors: [],
                  execErrors: []
                }
              };
            } else {
              nodeF = oldNode!;
              nodeF.data = {
                ...nodeF.data
              };
              nodeF.data.uuid = workflow.uuid;
              nodeF.data.sendToWebsocket = sendJsonMessage;
            }
  
            nodeF.type = recvNode.nodeType;
            switch (recvNode.nodeType) {
              case "PrimitiveNode": {
                const primitiveNode = nodeF as PrimitiveNodeTypeNode;
                primitiveNode.data.initialValue = recvNode.value;
                break;
              }
              case "CodeNode": {
                const codeNode = nodeF as CodeNodeTypeNode;
                codeNode.data.initialCode = recvNode.code;
                codeNode.data.initialLanguage = recvNode.language;
                break;
              }
            }
  
            setEdges(edgesCurrent => {
              const removedAll = edgesCurrent.filter(e => e.source != nodeF.id && e.target != nodeF.id);
  
              const inEdges = recvNode.inputs.reduce<Edge[]>((acc, i) => {
                if (i.connectedTo != undefined) {
                  return addEdge({target: recvNode.id.toString(), targetHandle: i.id.toString(), source: i.connectedTo.nodeId.toString(), sourceHandle: i.connectedTo.connectorId.toString(), animated: true}, acc);
                }
                return acc;
              }, removedAll);
    
              const outEdges = recvNode.outputs.reduce<Edge[]>((acc, i) => {
                return i.connectedTo.reduce<Edge[]>((acc2, cc) => {
                  return addEdge({source: recvNode.id.toString(), sourceHandle: i.id.toString(), target: cc.nodeId.toString(), targetHandle: cc.connectorId.toString(), animated: true}, acc2);
                }, acc);
              }, inEdges);
    
              return outEdges;
            });
            nodeF.data.node = recvNode;
    
            return [...currentNodes, nodeF as BaseNodeTypeNode];
          });
          break;
        }
        case "nodeRemoved": {
          setNodes(nodesCurrent => nodesCurrent.filter(n => n.id != notification.nodeId.toString()));
          break;
        }
        case "nodeState": {
          setNodes(nodesCurrent => {
            const foundNode = nodesCurrent.find(n => n.id == notification.nodeState.nodeId.toString());
            if (foundNode != undefined) {
              foundNode.data.execErrors = [];
              if (notification.nodeState.state == "FAILED") {
                if (notification.nodeState.execErrors) {
                  foundNode.data.execErrors = notification.nodeState.execErrors;
                }
              }

              foundNode.data = {
                ...foundNode.data
              }
              const posChanged: NodePositionChange = {
                id: foundNode.id,
                type: "position",
                dragging: false,
                position: {x: notification.nodeState.posX, y:notification.nodeState.posY},
              };
              return applyNodeChanges([posChanged], nodesCurrent);
            }
            return nodesCurrent;
          });
          break;
        }
        case "workflowState": {
          const nodesErrors: WorkflowNodeErrors[] = notification.workflowState.errors?.filter(e => e.type == "node") ?? [];
          const nodesErrorsGroupedById = nodesErrors.reduce<Partial<Record<number, WorkflowNodeErrors[]>>>((acc, curr) => {
            if (acc[curr.nodeId] == undefined) {
              acc[curr.nodeId] = [];
            }
            acc[curr.nodeId]!.push(curr);
            return acc;
          }, {});
          setNodes(nodesCurrent => {
            return nodesCurrent.map(n => {
              const data = nodesErrorsGroupedById[n.data.node.id];
              n.data = {
                ...n.data,
              };
              n.data.errors = data ?? [];
              return {...n};
            });
          });

          const generalErrors: WorkflowGeneralErrors[] = notification.workflowState.errors?.filter(e => e.type == "general") ?? [];
          setWorkflowGeneralErrors(generalErrors);
          break;
        }
        case "error": {
          alertError(notification.error);
          break;
        }
      }
    }
  })

  const handleChange = useCallback((event: SelectChangeEvent<string>) => {
    const data: ISwitchTo = {
      action: "switchTo",
      uuid: event.target.value
    }
    sendJsonMessage(data);
  }, [sendJsonMessage]);

  const [open, setOpen] = useState(false);
  const [workflowName, setWorkflowName] = useState("");
  const farEnd = useMemo(() => {
    return (
      <>
        <Dialog open={open} onClose={() => setOpen(false)}>
          <DialogTitle sx={{paddingBottom: 0}}>Add new workflow</DialogTitle>
          <TextField 
            label="Workflow name"
            value={workflowName} 
            onChange={(ev) => setWorkflowName(ev.target.value)}
            size="small"
            sx={{margin: 1}}
            />
          <Button onClick={() => {
            const data: ICreateWorkflow = {
              action: "createWorkflow",
              name: workflowName
            }
            sendJsonMessage(data)
            setOpen(false);
          }}>Add new workflow</Button>
        </Dialog>
        <ErrorPopover errors={workflowGeneralErrors.map(e => e.error)} size="medium" />
        <IconButton size="medium" sx={{marginX: 1}} onClick={() => {
          if (workflow == undefined) {
            alertError("No workflow selected !");
            return;
          }

          const data: IExecuteWorkflow = {
            action: "executeWorkflow",
            uuid: workflow?.uuid
          }
          sendJsonMessage(data);
        }}>
          <PlayArrow fontSize="inherit" color="success" />
        </IconButton>
        <FormControl sx={{minWidth: 110, maxWidth: 200}} size="small">
          <InputLabel id="workflow-select">Wokflow</InputLabel>
          <Select
            labelId="workflow-select-label"
            id="workflow-select"
            value={workflow?.uuid ?? ""}
            label="Workflow"
            onChange={handleChange}
          >
            <MenuItem value="">None</MenuItem>
            {workflows.map(workflow => 
              <MenuItem value={workflow.uuid} key={workflow.uuid}>{workflow.name}</MenuItem>
            )}
          </Select>
        </FormControl>
        <IconButton onClick={() => {
          setWorkflowName("");
          setOpen(true);
        }}>
          <AddCircle />
        </IconButton>
      </>
    )
  }, [workflows, workflow, open, workflowName, workflowGeneralErrors, handleChange, sendJsonMessage, alertError])

  const onSelect = useCallback((variant: ContextMenuVariants | undefined, position: XYPosition, choice: string) => {
    if (variant == undefined) {
      return;
    }
    switch(variant.name) {
      case "main": {
        if (workflow == undefined) {
          alertError("No workflow selected !");
          return;
        }
    
        const realPos = screenToFlowPosition(position);
    
        const dataNode: ICreateNode = {
          uuid: workflow.uuid,
          action: "createNode",
          posX: realPos.x,
          posY: realPos.y
        }
        switch (choice) {
          case "Code": {
            const dataCode: ICreateCodeNode = {
              ...dataNode,
              type: "code"
            };
            sendJsonMessage(dataCode);
            break;
          }
          default: {
            const chosenType = $enum(PrimitiveTypes).getKeys().find(pt => pt == choice);
            if (chosenType != undefined) {
              const dataPrimitive: ICreatePrimitiveNode = {
                ...dataNode,
                type: "primitive",
                primitive: "Primitive " + chosenType
              };
              sendJsonMessage(dataPrimitive);
            }
          }
        }
        break;
      }
      case "edge": {
        if (choice == "Delete") {
          if (workflow == undefined) {
            alertError("No workflow selected !");
            return;
          }
          const data: IDisconnect = {
            action: "disconnect",
            uuid: workflow.uuid,
            nodeId: Number(variant.data.target),
            connectorId: Number(variant.data.targetHandle)
          }
          sendJsonMessage(data);
        }
        break;
      }
      case "node": {
        if (choice == "Delete") {
          if (workflow == undefined) {
            alertError("No workflow selected !");
            return;
          }
          const data: IRemoveNode = {
            action: "removeNode",
            uuid: workflow.uuid,
            nodeId: Number(variant.data.id)
          }
          sendJsonMessage(data);
        }
      }
    }

    console.log(variant?.name + " & " + variant?.data + " => " + choice);
  }, [workflow, alertError, screenToFlowPosition, sendJsonMessage]);

  return (
    <Layout farEnd={farEnd}>
      <ContextMenuProvider onSelect={onSelect}>
        <WorkflowSocketProvider sendMessage={sendJsonMessage}>
          <ReactFlowGraph />
        </WorkflowSocketProvider>
      </ContextMenuProvider>
    </Layout>
  )
}