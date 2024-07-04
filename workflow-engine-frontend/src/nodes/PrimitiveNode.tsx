import { useCallback } from 'react';
import { NodeProps } from 'reactflow';
import BaseNode, { BaseNodeData } from "./BaseNode";
import { Box } from "@mui/material";

// const handleStyle = { left: 10 };
 
export type PrimitiveNodeData = BaseNodeData & {
  initialValue: string | number | undefined;
}

export default function PrimitiveNode(props: NodeProps<PrimitiveNodeData>) {
  const onChange = useCallback((evt: React.ChangeEvent<HTMLInputElement>) => {
    console.log("Primitive value changed to " + evt.target?.value);
  }, []);
 
  return (
    <BaseNode {...props}>
      <Box style={{alignSelf: "center"}}>{props.data.node.outputs[1].type}</Box>
      <Box style={{display: "flex"}}>
        <label htmlFor="text">Text:</label>
        <input id="text" name="text" size={1} style={{flexGrow: 1}} onChange={onChange} className="nodrag" defaultValue={props.data.initialValue} />
      </Box>
    </BaseNode>
  );
}