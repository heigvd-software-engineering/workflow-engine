import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { Node, NodeProps } from '@xyflow/react';
import { BaseNode, BaseNodeData } from "./BaseNode";
import { Box, Button, TextField } from "@mui/material";
import { IChangePrimitiveNode, IValueChangePrimitiveNode } from "../types/Types";
import { useAlert } from "../utils/alert/AlertUse";

export type PrimitiveNodeData = BaseNodeData & {
  initialValue: string | number | undefined;
}

export type PrimitiveNodeTypeNode = Node<PrimitiveNodeData, "PrimitiveNode">;

export const PrimitiveNode = memo(function PrimitiveNode(props: NodeProps<PrimitiveNodeTypeNode>) { 
  const { alertError } = useAlert();
  
  const defaultValue = useMemo(() => props.data.initialValue, [props.data.initialValue]);
  const [value, setValue] = useState(defaultValue);

  const cancel = useCallback(() => {
    setValue(defaultValue);
  }, [defaultValue]);

  useEffect(() => {
    cancel();
  }, [cancel]);

  const sendValues = useCallback(() => {
    if (value == undefined) {
      alertError("The value is undefined !");
      return;
    }

    const baseData: IChangePrimitiveNode = {
      action: "changePrimitiveNode",
      uuid: props.data.uuid,
      nodeId: props.data.node.id
    };

    const dataValue: IValueChangePrimitiveNode = {
      ...baseData,
      subAction: "value",
      value: value
    };
    props.data.sendToWebsocket(dataValue);
  }, [value, props.data, alertError]);
 
  const hasChanged = useMemo(() => defaultValue != value, [defaultValue, value]);
 
  return (
    <BaseNode {...props} title={props.data.node.outputs[0].type}>
      <Box sx={{margin: 1, display: "flex", flexDirection: "column", alignItems: "center"}}>
        <TextField
          value={value}
          onChange={ev => setValue(ev.target.value)}
          label="Value"
          size="small"
          className="nodrag smaller"
        />
        <Box sx={{marginTop: 1, display: "flex", justifyContent: "center"}}>
          <Button size="small" variant="contained" sx={{marginRight: 1}} onClick={sendValues} disabled={!hasChanged}>
            Save
          </Button>
          <Button size="small" variant="contained" color="error" onClick={cancel} disabled={!hasChanged}>
            Cancel
          </Button>
        </Box>
      </Box>
    </BaseNode>
  );
}, (prev, next) => JSON.stringify(prev.data) == JSON.stringify(next.data));