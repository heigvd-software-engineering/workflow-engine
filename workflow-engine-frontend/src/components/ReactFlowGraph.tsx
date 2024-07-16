import { applyNodeChanges, Background, Connection, Controls, NodeChange, ReactFlow } from "@xyflow/react";
import { useContextMenu } from "../utils/contextMenu/ContextMenuUse";
import { ContextMenuEdge, ContextMenuMain, ContextMenuNode } from "../utils/contextMenu/ContextMenuProvider";
import { PrimitiveNode } from "../nodes/PrimitiveNode";
import { CodeNode } from "../nodes/CodeNode";
import { useCallback, useMemo } from "react";
import { useWorkflowData } from "../utils/workflowData/WorkflowDataUse";
import { useAlert } from "../utils/alert/AlertUse";
import { IConnect, IMoveNode } from "../types/Types";
import { useWorkflowSocket } from "../utils/workflowSocket/WebsocketUse";
import { NoDataNode } from "../nodes/NoDataNode";

export default function ReactFlowGraph() {
  const nodeTypes = useMemo(() => ({ PrimitiveNode: PrimitiveNode, CodeNode: CodeNode, FileNode: NoDataNode("File") }), []);

  const { sendMessage } = useWorkflowSocket();
  const { openMenu } = useContextMenu();
  const { alertError } = useAlert();
  const { setNodes, nodes, edges, workflow } = useWorkflowData();

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => {
      changes.forEach(change => {
        switch (change.type) {
          case "position": {
            if (workflow == undefined) {
              alertError("No workflow selected !");
              return;
            }
            
            const realPos = change.position;
            if (realPos == undefined) {
              return;
            }
  
            if (Number.isNaN(realPos.x) || Number.isNaN(realPos.y)) {
              return;
            }
  
            const data: IMoveNode = {
              uuid: workflow.uuid,
              action: "moveNode",
              nodeId: Number(change.id),
              posX: realPos.x,
              posY: realPos.y,
            };
            sendMessage(data);
            break;
          }
          case "dimensions": {
            setNodes((nds) => applyNodeChanges([change], nds));
            break;
          }
        }
      });
    },
    [alertError, sendMessage, workflow, setNodes]
  );

  const onConnect = useCallback(
    (connection: Connection) => {
      if (workflow == undefined) {
        alertError("No workflow selected !");
        return;
      }

      const data: IConnect = {
        action: "connect",
        uuid: workflow.uuid,
        fromNodeId: Number(connection.source),
        fromConnectorId: Number(connection.sourceHandle),
        toNodeId: Number(connection.target),
        toConnectorId: Number(connection.targetHandle)
      }
      sendMessage(data);
    },
    [alertError, workflow, sendMessage]
  );

  return (
    <ReactFlow
      style={{height: "auto"}} 
      nodeTypes={nodeTypes} 
      nodes={nodes} 
      edges={edges}
      onNodesChange={onNodesChange}
      onConnect={onConnect}
      nodeDragThreshold={1}
      onContextMenu={(ev) => {
        openMenu({name: "main"} as ContextMenuMain, {x: ev.clientX, y: ev.clientY});

        ev.preventDefault();
      }}
      onNodeContextMenu={(ev, node) => {
        openMenu({name: "node", data: node} as ContextMenuNode, {x: ev.clientX, y: ev.clientY});

        ev.preventDefault();
        ev.stopPropagation();
      }}
      onEdgeContextMenu={(ev, edge) => {
        openMenu({name: "edge", data: edge} as ContextMenuEdge, {x: ev.clientX, y: ev.clientY});

        ev.preventDefault();
        ev.stopPropagation();
      }}
    >
      <Background />
      <Controls />
    </ReactFlow>
  )
}