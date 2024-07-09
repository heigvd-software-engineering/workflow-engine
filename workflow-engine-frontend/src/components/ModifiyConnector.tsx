import { Box, Button, Dialog, DialogTitle, Input } from "@mui/material";
import { Connector } from "../types/Types";
import { BaseNodeData } from "../nodes/BaseNode";
import { useEffect, useState } from "react";
import ModifyType from "./ModifyType";

export default function ModifyConnector(props: BaseNodeData & { isOpen: boolean, onClose: () => void, connector?: Connector }) {
  const [name, setName] = useState("")
  const [type, setType] = useState("")

  useEffect(() => {
    setName(props.connector?.name ?? "")
    setType(props.connector?.type ?? "")
  }, [props.connector?.name, props.connector?.type])
  
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
          <ModifyType type={type} setType={setType} />
        </Box>
        <Box sx={{marginBottom: 1, display: "flex", justifyContent: "center"}}>
          <Button size="small" variant="contained" sx={{marginRight: 1}}>
            Save
          </Button>
          <Button size="small" variant="contained" color="error">
            Cancel
          </Button>
        </Box>
      </Box>
    </Dialog>
  )
}