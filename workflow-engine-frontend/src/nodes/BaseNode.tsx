import "./css/BaseNode.css"

import { Handle, NodeProps, Position, useUpdateNodeInternals } from "@xyflow/react";
import { Connector, ConnectorError, NodeType, State, WorkflowInstruction, WorkflowNodeErrors } from "../types/Types";
import { ReactNode, useCallback, useEffect, useMemo } from "react";
import { Box, Divider, SxProps } from "@mui/material";
import { Theme } from "@emotion/react";
import { CodeNodeTypeNode } from "./CodeNode";
import { PrimitiveNodeTypeNode } from "./PrimitiveNode";
import ErrorPopover from "../components/ErrorPopover";
import StateIcon from "../components/StateIcon";
import BasePopover from "../components/BasePopover";
import ModifyType from "../components/ModifyType";
import { NoDataNodeTypeNode } from "./NoDataNode";

export type BaseNodeData = {
  node: NodeType;
  uuid: string;
  sendToWebsocket: (data: WorkflowInstruction) => void;
  errors: WorkflowNodeErrors[];
  execErrors: WorkflowNodeErrors[];
  state?: State;
}

export type BaseNodeTypeNode = CodeNodeTypeNode | PrimitiveNodeTypeNode | NoDataNodeTypeNode;

const flexStyle: SxProps<Theme> = {
  flex: 1,
  display: "flex",
}

export function BaseNode(props: NodeProps<BaseNodeTypeNode> & { children?: ReactNode, title: string, rightElement?: ReactNode }) {
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

  const [nodeErrors, connectorError] = useMemo(() => 
    props.data.errors.concat(props.data.execErrors).reduce<[WorkflowNodeErrors[], (WorkflowNodeErrors & ConnectorError)[]]>((acc, e) => {
      const ce = e as (WorkflowNodeErrors & ConnectorError);
      if (ce.connectorId == undefined || ce.connectorType == undefined) {
        return [acc[0].concat([e]), acc[1]]
      } else {
        return [acc[0], acc[1].concat([ce])]
      }
    }, [[], []])
  , [props.data.errors, props.data.execErrors]);

  const connStyle = useCallback((c: Connector, isInput: boolean) => {
    const filtered = connectorError.filter(e => {
      if (e.connectorId != c.id) {
        return false;
      }
      switch (e.connectorType) {
        case "input":
          return isInput;
        case "output":
          return !isInput;
      }
    }, []);

    return (
      <Box key={c.id} sx={{display: "flex", flexDirection: isInput ? "row" : "row-reverse", alignItems: "center"}}>
        <Handle type={isInput ? "target" : "source"} className={c.type == "Flow" ? "flow" : ""} position={isInput ? Position.Left : Position.Right} id={c.id.toString()} />
        <Box className="noWrap">
          <BasePopover popover={<ModifyType type={c.type} setType={() => {}} editMode={false} />}>
            {c.name}
          </BasePopover>
        </Box>
        <ErrorPopover errors={filtered.map(e => e.error)} />
      </Box>
    )
  }, [connectorError]);

  return (
    <Box className="base-node" sx={{display: "flex", flexDirection: "column"}}>
      <Box sx={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 0.5}}>
        <Box sx={{...flexStyle, marginRight: "auto", justifyContent: "start"}}>
          {props.data.state && <StateIcon state={props.data.state} />}
          <ErrorPopover errors={nodeErrors.map(e => e.error)} />
        </Box>
        <Box sx={{...flexStyle, justifyContent: "center", whiteSpace: "nowrap"}}>{props.title}</Box>
        <Box sx={{...flexStyle, marginLeft: "auto", justifyContent: "end"}}>{props.rightElement}</Box>
      </Box>
      {props.children}
      <Box sx={{display: "flex", flexDirection: "row", marginTop: 1, justifyContent: "space-between"}}>
        {atLeastOneInput &&
          <Box sx={{...flexStyle, flexDirection: "column", marginRight: "auto"}}>
            {props.data.node.inputs.map(i => connStyle(i, true))}
          </Box>
        }
        {(atLeastOneInput && atLeastOneOutput) && 
          <Divider sx={{...flexStyle, flex:0, backgroundColor: "black", marginX: 1}} orientation="vertical" flexItem />
        }
        {atLeastOneOutput && 
          <Box sx={{...flexStyle, flexDirection: "column", marginLeft: "auto"}}>
            {props.data.node.outputs.map(o => connStyle(o, false))}
          </Box>
        }
      </Box>
    </Box>
  );
}