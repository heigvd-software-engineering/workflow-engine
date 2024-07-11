import "./css/BaseNode.css"

import { Handle, NodeProps, Position, useUpdateNodeInternals } from "@xyflow/react";
import { NodeType, WorkflowInstruction } from "../types/Types";
import { ReactNode, useEffect } from "react";
import { Box, Divider, SxProps } from "@mui/material";
import { Theme } from "@emotion/react";
import { CodeNodeTypeNode } from "./CodeNode";
import { PrimitiveNodeTypeNode } from "./PrimitiveNode";

export type BaseNodeData = {
  node: NodeType;
  uuid: string;
  sendToWebsocket: (data: WorkflowInstruction) => void;
}

export type BaseNodeTypeNode = CodeNodeTypeNode | PrimitiveNodeTypeNode;

const InputOutputStyle: SxProps<Theme> = {
  display: "flex",
  flexDirection: "row"
}

const OutputStyle: SxProps<Theme> = {
  ...InputOutputStyle,
  alignSelf: "end"
}

const flexStyle: SxProps<Theme> = {
  flex: 1,
  display: "flex",
}

export const BaseNode = function BaseNode(props: NodeProps<BaseNodeTypeNode> & { children: ReactNode, title: string, leftElement?: ReactNode, rightElement?: ReactNode }) {
  const atLeastOneInput = props.data.node.inputs.length != 0;
  const atLeastOneOutput = props.data.node.outputs.length != 0;

  const updateNodeInternals = useUpdateNodeInternals();

  useEffect(() => {
    //Needed when we add handles programmatically
    //Source: https://reactflow.dev/learn/troubleshooting#couldnt-create-edge-for-sourcetarget-handle-id-some-id-edge-id-some-id
    updateNodeInternals(props.data.node.id.toString());
  //Here the inputs and outputs are not directely used but are necessary to trigger the updateNodeInternals
  //eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.data.node.outputs, props.data.node.inputs]);

  return (
    <Box className="base-node" sx={{display: "flex", flexDirection: "column"}}>
      <Box sx={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 0.5}}>
        <Box sx={{...flexStyle, marginRight: "auto", justifyContent: "start"}}>{props.leftElement}</Box>
        <Box sx={{...flexStyle, justifyContent: "center", whiteSpace: "nowrap"}}>{props.title}</Box>
        <Box sx={{...flexStyle, marginLeft: "auto", justifyContent: "end"}}>{props.rightElement}</Box>
      </Box>
      {props.children}
      <Box sx={{display: "flex", flexDirection: "row", marginTop: 1, justifyContent: "space-between"}}>
        {atLeastOneInput &&
          <Box sx={{...flexStyle, flexDirection: "column", marginRight: "auto"}}>
            {props.data.node.inputs.map(i =>
              <Box key={i.id} sx={InputOutputStyle}>
                <Handle type="target" className={i.type == "Flow" ? "flow" : ""} position={Position.Left} id={i.id.toString()} />
                <span className="noWrap">{i.name}</span>
              </Box>
            )}
          </Box>
        }
        {(atLeastOneInput && atLeastOneOutput) && 
          <Divider sx={{...flexStyle, flex:0, backgroundColor: "black", marginX: 1}} orientation="vertical" flexItem />
        }
        {atLeastOneOutput && 
          <Box sx={{...flexStyle, flexDirection: "column", marginLeft: "auto"}}>
            {props.data.node.outputs.map(o =>
              <Box key={o.id} sx={OutputStyle}>
                <span className="noWrap">{o.name}</span>
                <Handle type="source" className={o.type == "Flow" ? "flow" : ""} position={Position.Right} id={o.id.toString()} />
              </Box>
            )}
          </Box>
        }
      </Box>
    </Box>
  );
};