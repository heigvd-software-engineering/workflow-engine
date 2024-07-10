import { useCallback } from 'react';
import { Node, NodeProps } from '@xyflow/react';
import BaseNode, { BaseNodeData } from "./BaseNode";
import { TextField } from "@mui/material";

export type PrimitiveNodeData = BaseNodeData & {
  initialValue: string | number | undefined;
}

export type PrimitiveNodeTypeNode = Node<PrimitiveNodeData, "PrimitiveNode">;

export default function PrimitiveNode(props: NodeProps<PrimitiveNodeTypeNode>) {
  const onChange = useCallback((evt: React.ChangeEvent<HTMLInputElement>) => {
    console.log("Primitive value changed to " + evt.target?.value);
  }, []);
 
  return (
    <BaseNode {...props} title={props.data.node.outputs[0].type}>
      <TextField
        defaultValue={props.data.initialValue}
        label="Value"
        size="small"
        onChange={onChange}
        className="nodrag smaller"
      />
    </BaseNode>
  );
}