import { Warning } from "@mui/icons-material";
import { Box, Popover } from "@mui/material";
import { useState } from "react";

export default function ErrorPopover(props: { errors: string[], size?: 'inherit' | 'large' | 'medium' | 'small' }) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const handlePopoverOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handlePopoverClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  return (
    props.errors.length != 0 && <>
      <Box onMouseEnter={handlePopoverOpen} onMouseLeave={handlePopoverClose}>
        <Warning sx={{verticalAlign: "middle"}} fontSize={props.size ?? "small"} color="error" />
      </Box>
      <Popover
        sx={{
          pointerEvents: 'none',
        }}
        open={open}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        onClose={handlePopoverClose}
        disableRestoreFocus
      >
        <Box sx={{padding: 1}}>
          {props.errors.map(err => (
            <Box key={err}>{err}</Box>
          ))}
        </Box>
      </Popover>
    </>
  )
}