import ReactFlow, { Background, Connection, Controls, Edge, EdgeChange, Node, NodeChange, XYPosition, addEdge } from "reactflow";
import Layout from "./Layout";
import 'reactflow/dist/style.css';
import PrimitiveNode from "./nodes/PrimitiveNode";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Dialog, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Select, SelectChangeEvent, TextField } from "@mui/material";
import { useAlert } from "./utils/alert/AlertUse";
import { WorkflowNotification, WorkflowType } from "./types/Types";
import { AddCircle } from "@mui/icons-material";

const nodeTypes = { PrimitiveNode: PrimitiveNode };

const initialNodes: Node[] = [
  { id: 'node-1', type: 'PrimitiveNode', position: { x: 0, y: 0 }, data: { initialValue: 123 } },
  { id: 'node-2', type: 'PrimitiveNode', position: { x: 300, y: 0 }, data: { initialValue: 455 } },
];
const initialEdges: Edge[] = [
  
]

export default function WorkflowsPage() {
  const [nodes, setNodes] = useState<Node[]>(initialNodes);
  const [edges, setEdges] = useState<Edge[]>(initialEdges);
  const [lastPosition, setLastPosition] = useState<XYPosition>();
  const [workflows, setWorkflows] = useState<WorkflowType[]>([])
  const [workflow, setWorkflow] = useState<WorkflowType>();
  const { alertSuccess, alertError, alertInfo } = useAlert();

  const [websocket, setWebsocket] = useState<WebSocket>();
  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8080/workflow");
    ws.onopen = () => {
      alertSuccess("Connected to the websocket");
      setWebsocket(ws);
    }
    ws.onerror = () => {
      alertError("Error")
      setWebsocket(undefined);
    }
    ws.onmessage = (ev) => {
      const notification: WorkflowNotification = JSON.parse(ev.data);
      switch (notification.notificationType) {
        case "clear": {
          setWorkflow(undefined);
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
          let foundNode = nodes.find(n => n.id == notification.node.id.toString());
          const recvNode = notification.node;
          const wasFound = foundNode != undefined;
          if (foundNode == undefined) {
            foundNode = {
              id: recvNode.id.toString(),
              position: {
                x: 0,
                y: 0
              },
              data: { }
            };
          }

          switch (recvNode.nodeType) {
            case "PrimitiveNode": {
              foundNode.type = "PrimitiveNode";
              foundNode.data.initialValue = recvNode.value;
              break;
            }
            case "CodeNode": {
              break;
            }
          }
          setEdges(edgesCurrent => {
            const removedAll = edgesCurrent.filter(e => e.sourceNode?.id != foundNode.id || e.targetNode?.id != foundNode.id);
            
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

          if (!wasFound) {
            setNodes(nodesCurrent => [...nodesCurrent, foundNode]);
          }
          break;
        }
        case "nodeRemoved": {
          setEdges(edgesCurrent => edgesCurrent.filter(e => e.sourceNode?.id != notification.nodeId.toString() || e.targetNode?.id != notification.nodeId.toString()));
          setNodes(nodesCurrent => nodesCurrent.filter(n => n.id != notification.nodeId.toString()));
          break;
        }
        case "nodeState": {
          const foundNode = nodes.find(n => n.id == notification.nodeState.id.toString());
          if (foundNode != undefined) {
            foundNode.position.x = notification.nodeState.posX;
            foundNode.position.y = notification.nodeState.posY;
          }

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
    }
    ws.onclose = () => {
      setWebsocket(undefined);
      alertInfo("Disconnected from the websocket")
    }
    return () => {
      ws.close();
    }
  }, [nodes, alertError, alertSuccess, alertInfo, setWebsocket, setWorkflows, setEdges, setNodes]);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => {
      changes.forEach(change => {
        if (change.type == "position") {
          if (!change.dragging && lastPosition != undefined) {
            console.log(change.id + " changed position " + lastPosition?.x + ":" + lastPosition?.y +  " !");
            setLastPosition(undefined);
          } else {
            setLastPosition(change.position);
          }
        }
        if (change.type == "remove") {
          console.log(change.id + " has been removed !");
        }
      });
      // setNodes((nds) => applyNodeChanges(changes, nds))
    },
    [setLastPosition, lastPosition]
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
    setWorkflow(workflows.find(w => w.uuid == event.target.value));
    // setWorkflow(event.target.value);
  }, [workflows, setWorkflow]);

  const [open, setOpen] = useState(false);
  const [workflowName, setWorkflowName] = useState("");
  const farEnd = useMemo(() => {
    return (
      <>
        <Dialog open={open} onClose={() => setOpen(false)}>
          <DialogTitle>Add new workflow</DialogTitle>
          <TextField label="Workflow name" value={workflowName} onChange={(ev) => setWorkflowName(ev.target.value)} />
          <Button onClick={() => {
            // console.log(workflowName);
            setOpen(false);
          }}>Add new workflow</Button>
        </Dialog>
        <FormControl sx={{minWidth: 110, maxWidth: 200}}>
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
  }, [workflows, workflow, handleChange, open, setOpen, workflowName, setWorkflowName])

  return (
    <Layout farEnd={farEnd}>
      <ReactFlow 
          style={{height: "auto"}} 
          nodeTypes={nodeTypes} 
          nodes={nodes} 
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeDragThreshold={1}
        >
        <Background />
        <Controls />
      </ReactFlow>
    </Layout>
  )
}