import { createContext, ReactNode, useState } from "react";
import { BaseNodeTypeNode } from "../../nodes/BaseNode";
import { Edge } from "@xyflow/react";
import { WorkflowType } from "../../types/Types";

type WorkflowDataContextType = {
  setNodes: (value: React.SetStateAction<BaseNodeTypeNode[]>) => void,
  nodes: BaseNodeTypeNode[],
  setEdges: (value: React.SetStateAction<Edge[]>) => void,
  edges: Edge[],
  setWorkflow: (value: React.SetStateAction<WorkflowType | undefined>) => void,
  workflow: WorkflowType | undefined,
  setWorkflows: (value: React.SetStateAction<WorkflowType[]>) => void,
  workflows: WorkflowType[]
}

export const WorkflowDataContext = createContext<WorkflowDataContextType>({
  setNodes: () => {},
  nodes: [],
  setEdges: () => {},
  edges: [],
  setWorkflow: () => {},
  workflow: undefined,
  setWorkflows: () => {},
  workflows: []
});

export default function WorkflowDataProvider(props: { children: ReactNode }) {
  const [nodes, setNodes] = useState<BaseNodeTypeNode[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [workflow, setWorkflow] = useState<WorkflowType>();
  const [workflows, setWorkflows] = useState<WorkflowType[]>([])

  return (
    <WorkflowDataContext.Provider value={{setNodes, nodes, setEdges, edges, workflow, setWorkflow, workflows, setWorkflows}}>
      {props.children}
    </WorkflowDataContext.Provider>
  )
}