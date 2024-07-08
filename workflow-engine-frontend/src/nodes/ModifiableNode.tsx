import { ReactNode, useCallback, useMemo, useState } from 'react';
import { NodeProps } from 'reactflow';
import BaseNode, { BaseNodeData } from "./BaseNode";
import { Box, Button, Checkbox, Dialog, DialogTitle, Divider, IconButton, Input, InputAdornment } from "@mui/material";
import { Add, Delete, Edit, Settings } from "@mui/icons-material";
import { Connector, IIsDeterministicChangeModifiableNode, ITimeoutChangeModifiableNode } from "../types/Types";
import { useAlert } from "../utils/alert/AlertUse";

export default function ModifiableNode(props: NodeProps<BaseNodeData> & { children: ReactNode, title: string }) {
  const [open, setOpen] = useState(false);
  const settings = useMemo(() => {
    return (
      <IconButton sx={{}} size="small" className="nodrag" onClick={() => setOpen(true)}>
        <Settings fontSize="inherit" />
      </IconButton>
    )
  }, []);
  console.log(props.data.node.timeout);

  const connectorsStyle = useCallback((connectors: Connector[]) =>
    <Box sx={{display: "flex", flexDirection: "column"}}>
      <Box>
        {connectors.map(c => 
          <Box key={c.id} sx={{display: "flex", alignItems: "center"}}>
            <Box>{c.name}: {c.type}</Box>
            <IconButton size="small" sx={{marginLeft: 0.5}}>
              <Edit fontSize="inherit" color="primary" />
            </IconButton>
            <IconButton size="small">
              <Delete fontSize="inherit" color="error" />
            </IconButton>
          </Box>
        )}
      </Box>
      <Box sx={{alignSelf: "center"}}>
        <IconButton size="small">
          <Add fontSize="inherit" color="success" />
        </IconButton>
      </Box>
    </Box>
  , []);
  
  const defaultIsDeterministic = useMemo(() => props.data.node.isDeterministic, [props.data.node.isDeterministic]);
  const [isDeterministic, setIsDeterministic] = useState(defaultIsDeterministic);
  const onIsDeterministicChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.checked;
    setIsDeterministic(value);
  }

  const defaultTimeout = useMemo(() => props.data.node.timeout, [props.data.node.timeout]);
  const [timeout, setTimeout] = useState(defaultTimeout);
  const onTimeoutChanged = (e: React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>) => {
    const value = Number(e.target.value);
    setTimeout(value);
  }

  const { alertError } = useAlert();

  const cancel = useCallback(() => {
    setIsDeterministic(defaultIsDeterministic);
    setTimeout(defaultTimeout);
  }, [defaultIsDeterministic, defaultTimeout])

  const sendValues = useCallback(() => {
    if (!Number.isInteger(timeout) || timeout <= 0) {
      alertError("The timeout value must be an integer greater than 0");
      return;
    }

    const dataIsDeterministic: IIsDeterministicChangeModifiableNode = {
      uuid: props.data.uuid,
      nodeId: props.data.node.id,
      action: "changeModifiableNode",
      subAction: "isDeterministic",
      isDeterministic: isDeterministic
    };
    props.data.sendToWebsocket(dataIsDeterministic);

    const dataTimeout: ITimeoutChangeModifiableNode = {
      uuid: props.data.uuid,
      nodeId: props.data.node.id,
      action: "changeModifiableNode",
      subAction: "timeout",
      timeout: timeout
    };
    props.data.sendToWebsocket(dataTimeout);
  }, [isDeterministic, timeout, alertError, props]);

  return (
    <>
      <Dialog open={open} onClose={() => setOpen(false)}>
        <DialogTitle sx={{paddingBottom: 0, textAlign: "center"}}>Modify node</DialogTitle>
        <Box sx={{margin: 1}}>
          <Box sx={{display: "flex", alignItems: "center"}}>
            <Box>Is deterministic :</Box>
            <Checkbox 
              checked={isDeterministic}
              onChange={onIsDeterministicChanged}
            />
          </Box>
          <Box sx={{display: "flex", alignItems: "center", marginBottom: 1}}>
            <Box sx={{marginRight: 1}}>Timeout :</Box>
            <Input
              type="number"
              value={timeout}
              onChange={onTimeoutChanged}
              endAdornment={<InputAdornment position="end">ms</InputAdornment>}
              sx={{width: "12ch"}}
              size="small"
              className="nodrag smaller"
            />
          </Box>
          <Box sx={{marginBottom: 1}}>
            <Button size="small" variant="contained" sx={{marginRight: 1}} onClick={sendValues}>
              Save
            </Button>
            <Button size="small" variant="contained" color="error" onClick={cancel}>
              Cancel
            </Button>
          </Box>
          <Box sx={{display: "grid", gridTemplateColumns: "1fr min-content 1fr"}}>
            <Box sx={{display: "flex", flexDirection: "column"}}>
              <Box sx={{textAlign: "center", textDecoration: "underline"}}>
                Inputs
              </Box>
              {connectorsStyle(props.data.node.inputs)}
            </Box>
            <Box sx={{margin: 1}}>
              <Divider orientation="vertical" />
            </Box>
            <Box sx={{display: "flex", flexDirection: "column"}}>
              <Box sx={{textAlign: "center", textDecoration: "underline"}}>
                Outputs
              </Box>
              {connectorsStyle(props.data.node.outputs)}
            </Box>
          </Box>
        </Box>
        {/* <Button onClick={() => {
          const data: ICreateWorkflow = {
            action: "createWorkflow",
            name: workflowName
          }
          sendJsonMessage(data)
          // console.log(workflowName);
          setOpen(false);
        }}>Add new workflow</Button> */}
      </Dialog>
      <BaseNode {...props} rightElement={settings}>
        {props.children}
      </BaseNode>
    </>
  );
}