import { useCallback } from 'react';
import { NodeProps } from 'reactflow';
import BaseNode, { BaseNodeData } from "./BaseNode";
import { TextField } from "@mui/material";

export type PrimitiveNodeData = BaseNodeData & {
  initialValue: string | number | undefined;
}

export default function PrimitiveNode(props: NodeProps<PrimitiveNodeData>) {
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