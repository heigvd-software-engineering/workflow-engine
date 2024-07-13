import { Warning } from "@mui/icons-material";
import { Box } from "@mui/material";
import BasePopover from "./BasePopover";

export default function ErrorPopover(props: { errors: string[], size?: 'inherit' | 'large' | 'medium' | 'small' }) {
  const popover = props.errors.map(err => (
    <Box key={err}>{err}</Box>
  ))
  
  return (
    props.errors.length != 0 && 
      <BasePopover popover={popover}>
        <Warning sx={{verticalAlign: "middle"}} fontSize={props.size ?? "small"} color="error" />
      </BasePopover>
  )
}