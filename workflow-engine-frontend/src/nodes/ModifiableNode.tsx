import { ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { NodeProps } from '@xyflow/react';
import { BaseNode, BaseNodeTypeNode } from "./BaseNode";
import { Box, Button, Checkbox, Dialog, DialogTitle, Divider, IconButton, Input, InputAdornment } from "@mui/material";
import { Add, Delete, Edit, Settings } from "@mui/icons-material";
import { Connector, IChangeModifiableNode, IIsDeterministicChangeModifiableNode, IRemoveConnector, ITimeoutChangeModifiableNode } from "../types/Types";
import { useAlert } from "../utils/alert/AlertUse";
import ModifyConnector from "../components/ModifiyConnector";
import ModifyType from "../components/ModifyType";

export const ModifiableNode = function ModifiableNode(props: NodeProps<BaseNodeTypeNode> & { children: ReactNode, title: string }) {
  const { alertError } = useAlert();
  const [open, setOpen] = useState(false);
  const settings = useMemo(() => {
    return (
      <IconButton sx={{}} size="small" className="nodrag" onClick={() => setOpen(true)}>
        <Settings fontSize="inherit" />
      </IconButton>
    )
  }, []);

  const [toModifyConnector, setToModifyConnector] = useState<Connector | undefined>(undefined);
  const [toModifyIsInput, setToModifyIsInput] = useState(false);
  const [isModifyOpen, setIsModifyOpen] = useState(false);
  const openEditConnector = useCallback((isInput: boolean, connectorId?: number) => {
    setToModifyIsInput(isInput);
    if (connectorId == undefined) {
      setToModifyConnector(undefined);
    } else {
      const connector = (isInput ? props.data.node.inputs : props.data.node.outputs).find(c => c.id == connectorId)
      setToModifyConnector(connector);
    }
    setIsModifyOpen(true);
  }, [props.data.node.inputs, props.data.node.outputs]);

  useEffect(() => {
    if (isModifyOpen && toModifyConnector != undefined) {
      let connectors: Connector[] = [];
      if (toModifyIsInput) {
        connectors = props.data.node.inputs;
      } else {
        connectors = props.data.node.outputs;
      }

      const currentConn = connectors.find(c => c.id == toModifyConnector.id);
      if (currentConn == undefined) {
        //Closes the edit menu if the connector currently modified is removed 
        setIsModifyOpen(false);
      } else {
        //Updates the current connector if needed
        if (currentConn != toModifyConnector) {
          setToModifyConnector(currentConn);
        }
      }
    }
  }, [isModifyOpen, toModifyIsInput, toModifyConnector, props.data.node.inputs, props.data.node.outputs]);

  const deleteConnector = useCallback((isInput: boolean, c: Connector) => {
    const data: IRemoveConnector = {
      action: "removeConnector",
      uuid: props.data.uuid,
      nodeId: props.data.node.id,
      connectorId: c.id,
      isInput: isInput
    };
    props.data.sendToWebsocket(data);
  }, [props])

  const connectorsStyle = useCallback((isInput: boolean, connectors: Connector[]) =>
    <Box sx={{display: "flex", flexDirection: "column"}}>
      <Box>
        {connectors.map(c => 
          <Box key={c.id} sx={{display: "flex", alignItems: "center"}}>
            <Box>{c.name}:&nbsp;</Box>
            <ModifyType type={c.type} setType={() => {}} editMode={false} />
            {!c.isReadOnly && <>
              <IconButton size="small" sx={{marginLeft: 0.5}} onClick={() => openEditConnector(isInput, c.id)}>
                <Edit fontSize="inherit" color="primary" />
              </IconButton>
              <IconButton size="small" onClick={() => deleteConnector(isInput, c)}>
                <Delete fontSize="inherit" color="error" />
              </IconButton>
            </>}
          </Box>
        )}
      </Box>
      <Box sx={{alignSelf: "center"}}>
        <IconButton size="small" onClick={() => openEditConnector(isInput, undefined)}>
          <Add fontSize="inherit" color="success" />
        </IconButton>
      </Box>
    </Box>
  , [openEditConnector, deleteConnector]);
  
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

  const cancel = useCallback(() => {
    setIsDeterministic(defaultIsDeterministic);
    setTimeout(defaultTimeout);
  }, [defaultIsDeterministic, defaultTimeout])

  //When the cancel function is updated it means that defaultIsDeterministic or defaultTimeout has changed => we want to update the values shown in the UI
  useEffect(() => {
    cancel();
  }, [cancel]);

  const sendValues = useCallback(() => {
    if (!Number.isInteger(timeout) || timeout <= 0) {
      alertError("The timeout value must be an integer greater than 0");
      return;
    }

    const baseData: IChangeModifiableNode = {
      uuid: props.data.uuid,
      nodeId: props.data.node.id,
      action: "changeModifiableNode"
    };

    const dataIsDeterministic: IIsDeterministicChangeModifiableNode = {
      ...baseData,
      subAction: "isDeterministic",
      isDeterministic: isDeterministic
    };
    props.data.sendToWebsocket(dataIsDeterministic);

    const dataTimeout: ITimeoutChangeModifiableNode = {
      ...baseData,
      subAction: "timeout",
      timeout: timeout
    };
    props.data.sendToWebsocket(dataTimeout);
  }, [isDeterministic, timeout, alertError, props]);

  const hasChanged = useMemo(() => defaultIsDeterministic != isDeterministic || defaultTimeout != timeout, [defaultIsDeterministic, isDeterministic, defaultTimeout, timeout]);
  
  return (
    <>
      <Dialog open={open} onClose={() => setOpen(false)}>
        <DialogTitle sx={{paddingBottom: 0, textAlign: "center"}}>Modify node</DialogTitle>
        <Box sx={{margin: 1, display: "flex", flexDirection: "column", alignItems: "center"}}>
          <Box sx={{display: "flex", flexDirection: "column"}}>
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
            <Box sx={{marginBottom: 1, display: "flex", justifyContent: "center"}}>
              <Button size="small" variant="contained" sx={{marginRight: 1}} onClick={sendValues} disabled={!hasChanged}>
                Save
              </Button>
              <Button size="small" variant="contained" color="error" onClick={cancel} disabled={!hasChanged}>
                Cancel
              </Button>
            </Box>
          </Box>
          <Box sx={{display: "grid", gridTemplateColumns: "1fr min-content 1fr"}}>
            <Box sx={{display: "flex", flexDirection: "column"}}>
              <Box sx={{textAlign: "center", textDecoration: "underline"}}>
                Inputs
              </Box>
              {connectorsStyle(true, props.data.node.inputs)}
            </Box>
            <Box sx={{margin: 1}}>
              <Divider orientation="vertical" />
            </Box>
            <Box sx={{display: "flex", flexDirection: "column"}}>
              <Box sx={{textAlign: "center", textDecoration: "underline"}}>
                Outputs
              </Box>
              {connectorsStyle(false, props.data.node.outputs)}
            </Box>
          </Box>
        </Box>
      </Dialog>
      <ModifyConnector {...props.data} isOpen={isModifyOpen} setIsOpen={setIsModifyOpen} onClose={() => setIsModifyOpen(false)} connector={toModifyConnector} isInput={toModifyIsInput} />
      <BaseNode {...props} rightElement={settings}>
        {props.children}
      </BaseNode>
    </>
  );
};