import ReactFlow, { Background, Connection, Controls, Edge, EdgeChange, Node, NodeChange, addEdge, applyEdgeChanges, applyNodeChanges } from "reactflow";
import Layout from "./Layout";
import 'reactflow/dist/style.css';
import PrimitiveNode from "./nodes/PrimitiveNode";
import { useCallback, useState } from "react";

const nodeTypes = { primitive: PrimitiveNode };

const initialNodes: Node[] = [
  { id: 'node-1', type: 'primitive', position: { x: 0, y: 0 }, data: { initialValue: 123 } },
  { id: 'node-2', type: 'primitive', position: { x: 300, y: 0 }, data: { initialValue: 455 } },
];
const initialEdges: Edge[] = [

]

export default function WorkflowsPage() {
  const [nodes, setNodes] = useState(initialNodes);
  const [edges, setEdges] = useState<Edge[]>(initialEdges);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => {
      changes.forEach(change => {
        if (change.type == "position") {
          if (!change.dragging) {
            console.log(change.id + " changed position !");
          }
        }
        if (change.type == "remove") {
          console.log(change.id + " has been removed !");
        }
      });
      setNodes((nds) => applyNodeChanges(changes, nds))
    },
    [setNodes]
  );
  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) => {
      changes.forEach(change => {
        if (change.type == "remove") {
          console.log("Edge removed " + change.id);
        }
      });
      setEdges((eds) => applyEdgeChanges(changes, eds))
    },
    [setEdges]
  );
  const onConnect = useCallback(
    (connection: Connection) => {
      console.log("Edge added from " + connection.source + ":" + connection.sourceHandle + " to " + connection.target + ":" + connection.targetHandle);
      setEdges((eds) => addEdge(connection, eds));
    },
    [setEdges]
  );

  const farEnd = (
    <>
      <p>Custom</p>
    </>
  )

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