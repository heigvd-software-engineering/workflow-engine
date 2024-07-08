import ReactFlow, { Background, Connection, Controls, Edge, EdgeChange, Node, NodeChange, NodePositionChange, addEdge, applyNodeChanges, useReactFlow } from "reactflow";
import Layout from "./Layout";
import 'reactflow/dist/style.css';
import PrimitiveNode, { PrimitiveNodeData } from "./nodes/PrimitiveNode";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Dialog, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Select, SelectChangeEvent, TextField } from "@mui/material";
import { useAlert } from "./utils/alert/AlertUse";
import { ICreateCodeNode, ICreateNode, ICreatePrimitiveNode, ICreateWorkflow, IMoveNode, ISwitchTo, PrimitiveTypes, WorkflowNotification, WorkflowType } from "./types/Types";
import { AddCircle } from "@mui/icons-material";
import CreateNodeMenu, { MenuData } from "./components/CreateNodeMenu";
import useWebSocket, { ReadyState } from "react-use-websocket";
import { $enum } from "ts-enum-util";
import CodeNode, { CodeNodeData } from "./nodes/CodeNode";
import { BaseNodeData } from "./nodes/BaseNode";

const nodeTypes = { PrimitiveNode: PrimitiveNode, CodeNode: CodeNode };

export default function WorkflowsPage() {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowType[]>([])
  const [workflow, setWorkflow] = useState<WorkflowType>();
  const { alertSuccess, alertError, alertInfo } = useAlert();

  const { sendJsonMessage, lastJsonMessage, readyState } = useWebSocket<WorkflowNotification>("ws://localhost:8080/workflow", { share: false, shouldReconnect: () => false })

  useEffect(() => {
    switch (readyState) {
      case ReadyState.OPEN: {
        alertSuccess("Connected to the websocket"); 
        break;
      }
      case ReadyState.CLOSED: {
        alertInfo("Websocket connexion closed");
        break;
      }
    }
  }, [readyState, alertSuccess, alertInfo]);

  useEffect(() => {
    if (lastJsonMessage == undefined) {
      return;
    }
    const notification = lastJsonMessage;
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
          const foundNode = currentNodes.find(n => n.id == notification.node.id.toString());
          const recvNode = notification.node;
          const wasCreated = foundNode == undefined;
          let nodeF: Node<BaseNodeData>;
          if (workflow == undefined) {
            alertError("No workflow selected !");
            return currentNodes;
          }
          if (wasCreated) {
            nodeF = {
              id: recvNode.id.toString(),
              position: {
                x: 0,
                y: 0
              },
              data: {
                node: recvNode,
                uuid: workflow.uuid,
                sendToWebsocket(data) {
                  sendJsonMessage(data);
                },
                event: new CustomEvent<string>("onDataChanged")
              }
            };
          } else {
            nodeF = foundNode;
          }
  
          nodeF.data.uuid = workflow.uuid;
          nodeF.data.sendToWebsocket = sendJsonMessage;

          nodeF.type = recvNode.nodeType;
          switch (recvNode.nodeType) {
            case "PrimitiveNode": {
              (nodeF.data as PrimitiveNodeData).initialValue = recvNode.value;
              break;
            }
            case "CodeNode": {
              (nodeF.data as CodeNodeData).initialCode = recvNode.code;
              break;
            }
          }
          setEdges(edgesCurrent => {
            const removedAll = edgesCurrent.filter(e => e.sourceNode?.id != nodeF.id || e.targetNode?.id != nodeF.id);
            
            const inEdges = recvNode.inputs.flatMap(i => {
              if (i.connectedTo != undefined) {
                return addEdge({source: recvNode.id.toString(), sourceHandle: i.id.toString(), target: i.connectedTo.nodeId.toString(), targetHandle: i.connectedTo.connectorId.toString()}, removedAll);
              }
              return removedAll;
            });
  
            const outEdges = recvNode.outputs.flatMap(i => {
              let ccEdges = [...inEdges];
              i.connectedTo.forEach(cc => {
                ccEdges = addEdge({source: recvNode.id.toString(), sourceHandle: i.id.toString(), target: cc.nodeId.toString(), targetHandle: cc.connectorId.toString()}, ccEdges);
              })
              return ccEdges;
            });
  
            return outEdges;
          });
          nodeF.data.node = recvNode;
          window.dispatchEvent(nodeF.data.event);
  
          if (wasCreated) {
            return [...currentNodes, nodeF];
          }
          return currentNodes;
        });
        break;
      }
      case "nodeRemoved": {
        setEdges(edgesCurrent => edgesCurrent.filter(e => e.sourceNode?.id != notification.nodeId.toString() || e.targetNode?.id != notification.nodeId.toString()));
        setNodes(nodesCurrent => nodesCurrent.filter(n => n.id != notification.nodeId.toString()));
        break;
      }
      case "nodeState": {
        setNodes(nodesCurrent => {
          const foundNode = nodesCurrent.find(n => n.id == notification.nodeState.nodeId.toString());
          if (foundNode != undefined) {
            const posChanged: NodePositionChange = {
              id: foundNode.id,
              type: "position",
              dragging: false,
              position: {x: notification.nodeState.posX, y:notification.nodeState.posY},
            };
            return applyNodeChanges([posChanged], nodesCurrent);
          }
          return nodesCurrent;
        })
        
        break;
      }
      case "workflowState": {

        break;
      }
      case "error": {
        alertError(notification.error);
        break;
      }
    }
  }, 
  //Here we want to have only lastJsonMessage as dependency because otherwise we would handle the same message multiple times
  [lastJsonMessage]);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => {
      changes.forEach(change => {
        if (change.type == "position") {
          if (workflow != undefined) {
            const realPos = change.position;
            if (realPos == undefined) {
              return;
            }

            const data: IMoveNode = {
              uuid: workflow.uuid,
              action: "moveNode",
              nodeId: Number(change.id),
              posX: realPos.x,
              posY: realPos.y,
            };
            sendJsonMessage(data);
            // console.log(change.id + " changed position " + lastPosition.x + ":" + lastPosition.y +  " !");
          } else {
            alertError("No workflow selected !");
          }
        }
        if (change.type == "remove") {
          console.log(change.id + " has been removed !");
        }
      });
      // setNodes((nds) => applyNodeChanges(changes, nds))
    },
    [alertError, sendJsonMessage, workflow]
  );

  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) => {
      changes.forEach(change => {
        if (change.type == "remove") {
          console.log("Edge removed " + change.id);
        }
      });
      // setEdges((eds) => applyEdgeChanges(changes, eds))
    },
    []
  );
  const onConnect = useCallback(
    (connection: Connection) => {
      console.log("Edge added from " + connection.source + ":" + connection.sourceHandle + " to " + connection.target + ":" + connection.targetHandle);
      // setEdges((eds) => addEdge(connection, eds));
    },
    []
  );

  const handleChange = useCallback((event: SelectChangeEvent<string>) => {
    const data: ISwitchTo = {
      action: "switchTo",
      uuid: event.target.value
    }
    sendJsonMessage(data);
    // setWorkflow(event.target.value);
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
            // console.log(workflowName);
            setOpen(false);
          }}>Add new workflow</Button>
        </Dialog>
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
  }, [workflows, workflow, open, workflowName, handleChange, sendJsonMessage])

  const allPrimitives = $enum(PrimitiveTypes).getKeys().map(p => {return {name: p}});

  const menus: MenuData[] = [
    {name: "Code"}, 
    {
      name: "Primitive", 
      subMenu: allPrimitives
    }
  ]

  const { screenToFlowPosition } = useReactFlow();
  const [menuPosition, setMenuPosition] = useState({x: 0, y: 0});
  const [menuVisible, setMenuVisible] = useState(false);
  const onSelect = useCallback((chosen: string) => {
    if (workflow == undefined) {
      alertError("No workflow selected !");
      return;
    }

    const realPos = screenToFlowPosition(menuPosition);

    const dataNode: ICreateNode = {
      uuid: workflow.uuid,
      action: "createNode",
      posX: realPos.x,
      posY: realPos.y
    }
    switch (chosen) {
      case "Code": {
        const dataCode: ICreateCodeNode = {
          ...dataNode,
          type: "code"
        };
        sendJsonMessage(dataCode);
        break;
      }
      default: {
        const chosenType = $enum(PrimitiveTypes).getKeys().find(pt => pt == chosen);
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

    setMenuVisible(false);
  }, [workflow, menuPosition, alertError, sendJsonMessage, screenToFlowPosition]);

  return (
    <Layout farEnd={farEnd}>
      <CreateNodeMenu options={menus} isVisible={menuVisible} position={menuPosition} setVisible={setMenuVisible} onSelect={onSelect} />
      <ReactFlow 
          style={{height: "auto"}} 
          nodeTypes={nodeTypes} 
          nodes={nodes} 
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeDragThreshold={1}
          onContextMenu={(ev) => {
            setMenuVisible(true);
            setMenuPosition({x: ev.clientX, y: ev.clientY});
            ev.preventDefault();
          }}
        >
        <Background />
        <Controls />
      </ReactFlow>
    </Layout>
  )
}