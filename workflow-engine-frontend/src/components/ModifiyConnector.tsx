import { Box, Button, Dialog, DialogTitle, Input } from "@mui/material";
import { Connector, IChangeConnector, ICreateConnector, INameChangeConnector, ITypeChangeConnector } from "../types/Types";
import { BaseNodeData } from "../nodes/BaseNode";
import { useCallback, useEffect, useMemo, useState } from "react";
import ModifyType from "./ModifyType";
import { useAlert } from "../utils/alert/AlertUse";

export default function ModifyConnector(props: BaseNodeData & { isOpen: boolean, setIsOpen: (isOpen: boolean) => void, onClose: () => void, connector?: Connector, isInput: boolean }) {
  const { alertError } = useAlert();
  
  const defaultName = useMemo(() => props.connector?.name ?? "", [props.connector?.name]);
  const defaultType = useMemo(() => props.connector?.type ?? "undefined", [props.connector?.type]);

  const [name, setName] = useState(defaultName);
  const [type, setType] = useState(defaultType);

  const cancel = useCallback(() => {
    setName(defaultName);
    setType(defaultType);
  }, [defaultName, defaultType]);

  useEffect(() => {
    cancel();
  }, [cancel]);

  const sendValues = useCallback(() => {
    if (type.includes("undefined")) {
      alertError("The type provided is not valid !");
      return;
    }

    if (props.connector == undefined) {
      const data: ICreateConnector = {
        action: "createConnector",
        uuid: props.uuid,
        nodeId: props.node.id,
        isInput: props.isInput,
        name: name,
        type: type
      };

      props.sendToWebsocket(data);
      props.setIsOpen(false);
    } else {
      const baseData: IChangeConnector = {
        action: "changeConnector",
        uuid: props.uuid,
        nodeId: props.node.id,
        isInput: props.isInput,
        connectorId: props.connector.id
      };

      const dataName: INameChangeConnector = {
        ...baseData,
        subAction: "name",
        newName: name
      };
      props.sendToWebsocket(dataName);

      const dataType: ITypeChangeConnector = {
        ...baseData,
        subAction: "type",
        newType: type
      };
      props.sendToWebsocket(dataType);
    }
  }, [name, type, props, alertError]);
  
  const hasChanged = useMemo(() => defaultName != name || defaultType != type, [defaultName, name, defaultType, type]);

  return (
    <Dialog open={props.isOpen} onClose={props.onClose}>
      <DialogTitle sx={{paddingBottom: 0, textAlign: "center"}}>{props.connector == undefined ? "Create" : "Edit"} connector</DialogTitle>
      <Box sx={{margin: 1}}>
        <Box sx={{display: "flex", alignItems: "center", marginBottom: 1}}>
          <Box sx={{marginRight: 1}}>Name :</Box>
          <Input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            sx={{width: "12ch"}}
            size="small"
            className="nodrag smaller"
          />
        </Box>
        <Box sx={{display: "flex", alignItems: "center", marginBottom: 1}}>
          <Box sx={{marginRight: 1}}>Type :</Box>
          <ModifyType type={type} setType={setType} editMode={true} />
        </Box>
        <Box sx={{marginBottom: 1, display: "flex", justifyContent: "center"}}>
          <Button size="small" variant="contained" sx={{marginRight: 1}} onClick={sendValues} disabled={!hasChanged}>
            {props.connector == undefined ? "Create" : "Save"}
          </Button>
          <Button size="small" variant="contained" color="error" onClick={cancel} disabled={!hasChanged}>
            Cancel
          </Button>
        </Box>
      </Box>
    </Dialog>
  )
}