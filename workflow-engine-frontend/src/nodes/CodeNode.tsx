import { useCallback } from 'react';
import { NodeProps } from 'reactflow';
import BaseNode, { BaseNodeData } from "./BaseNode";
import { Box, TextField } from "@mui/material";

// const handleStyle = { left: 10 };
 
export type CodeNodeData = BaseNodeData & {
  initialValue: string | number | undefined;
}

export default function PrimitiveNode(props: NodeProps<CodeNodeData>) {
  const onChange = useCallback((evt: React.ChangeEvent<HTMLInputElement>) => {
    console.log("Primitive value changed to " + evt.target?.value);
  }, []);
 
  return (
    <BaseNode {...props}>
      <Box style={{alignSelf: "center"}}>Code</Box>
      <Box style={{display: "flex"}}>
        <TextField
          label="Value"
          defaultValue={props.data.initialValue}
          variant="filled"
          size="small"
          onChange={onChange}
          className="nodrag"
        />
        {/* <label htmlFor="text">Text:</label> */}
        {/* <input id="text" name="text" size={1} style={{flexGrow: 1}} onChange={onChange} className="nodrag" defaultValue={props.data.initialValue} /> */}
      </Box>
    </BaseNode>
  );
}