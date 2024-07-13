import { AccessTime, Done, HighlightOff, Loop } from "@mui/icons-material";
import { State } from "../types/Types";
import { SxProps } from "@mui/material";
import { Box, Theme } from "@mui/system";
import { useMemo } from "react";

export default function StateIcon(props: { state: State, size?: 'inherit' | 'large' | 'medium' | 'small' }) {
  const icon = useMemo(() => {
    const fontSize = props.size ?? "small";
    const sx: SxProps<Theme> = {verticalAlign: "middle"}
    switch (props.state) {
      case "IDLE":
        return <AccessTime sx={sx} fontSize={fontSize} color="warning" />
      case "RUNNING":
        return <Loop sx={sx} fontSize={fontSize} color="primary" />
      case "FAILED":
        return <HighlightOff sx={sx} fontSize={fontSize} color="error" />
      case "FINISHED":
        return <Done sx={sx} fontSize={fontSize} color="success" />
    }
  }, [props.state, props.size]);

  return (
    <Box>
      {icon}
    </Box>
  )
}