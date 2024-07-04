import "./css/BaseNode.css"

import { Handle, NodeProps, Position } from "reactflow";
import { NodeType } from "../types/Types";
import { ReactNode } from "react";
import { Box, Divider, SxProps } from "@mui/material";
import { Theme } from "@emotion/react";

export type BaseNodeData = {
  node: NodeType;
}

const InputOutputStyle: SxProps<Theme> = {
  display: "flex",
  flexDirection: "row"
}

const OutputStyle: SxProps<Theme> = {
  ...InputOutputStyle,
  alignSelf: "end"
}

export default function BaseNode(props: NodeProps<BaseNodeData> & { children: ReactNode }) {
  return (
    <Box className="base-node" sx={{display: "flex", flexDirection: "column"}}>
      {props.children}
      <Box sx={{display: "flex", flexDirection: "row", marginTop: 1, justifyContent: "space-between"}}>
        <Box sx={{display: "flex", flexDirection: "column"}}>
          {props.data.node.inputs.map(i =>
            <Box key={i.id} sx={InputOutputStyle}>
              <Handle type="target" position={Position.Left} id={i.id.toString()} />
              <span>{i.name}</span>
            </Box>
          )}
        </Box>
        <Divider sx={{backgroundColor: "black", marginX: 1}} orientation="vertical" flexItem />
        <Box sx={{display: "flex", flexDirection: "column"}}>
          {props.data.node.outputs.map(o =>
            <Box key={o.id} sx={OutputStyle}>
              <span>{o.name}</span>
              <Handle type="source" position={Position.Right} id={o.id.toString()} />
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
}