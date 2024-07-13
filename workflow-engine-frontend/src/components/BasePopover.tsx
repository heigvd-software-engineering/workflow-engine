import { Box, Popover } from "@mui/material";
import { ReactNode, useCallback, useState } from "react";

export default function BasePopover(props: { children: ReactNode, popover: ReactNode, placement?: "top" | "right" | "bottom" | "left" }) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const handlePopoverOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handlePopoverClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  const inverseX = useCallback((placement: "right" | "left") => {
    if (placement == "left") {
      return "right";
    }
    return "left";
  }, []);

  const inverseY = useCallback((placement: "top" | "bottom") => {
    if (placement == "bottom") {
      return "top";
    }
    return "bottom";
  }, []);

  const placement = props.placement ?? "top";

  return (
    <>
      <Box onMouseEnter={handlePopoverOpen} onMouseLeave={handlePopoverClose}>
        {props.children}
      </Box>
      <Popover
        sx={{
          pointerEvents: 'none',
        }}
        open={open}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: placement == "right" || placement == "left" ? "center" : placement,
          horizontal: placement == "top" || placement == "bottom" ? "center" : placement,
        }}
        transformOrigin={{
          vertical: placement == "right" || placement == "left" ? "center" : inverseY(placement),
          horizontal: placement == "top" || placement == "bottom" ? "center" : inverseX(placement),
        }}
        onClose={handlePopoverClose}
        disableRestoreFocus
      >
        <Box sx={{padding: 1}}>
          {props.popover}
        </Box>
      </Popover>
    </>
  )
}