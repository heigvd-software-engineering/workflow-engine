import { ArrowDropDown, ArrowRight } from "@mui/icons-material"
import { lighten, ListItemIcon, ListItemText, MenuItem, MenuList, Paper, useTheme } from "@mui/material"
import { useState } from "react"

export type MenuData = {
  name: string,
  subMenu?: MenuData[]
}

type Props = {
  options: MenuData[],
  onSelect: (chosen: string) => void,
  isVisible: boolean,
  setVisible: (visible: boolean) => void
  position: {x: number, y: number}
}

const iconWidthPx = 20;
const iconMarginRightPx = 5;
const totalPx = iconWidthPx + iconMarginRightPx;

const toPx = (px: number) => px + "px";

const iconSyle = {width: toPx(iconWidthPx), marginRight: toPx(iconMarginRightPx)}

function BasicMenu(props: {data: MenuData[], level: number, onSelect: (chosen: string) => void}) {
  return props.data.map(o => 
    o.subMenu == undefined ?
      <MenuItem sx={{marginLeft: toPx(props.level * totalPx)}} key={o.name} onClick={() => props.onSelect(o.name)}>
        <ListItemText>{o.name}</ListItemText>
      </MenuItem>
      :
      <SubMenu key={o.name} data={o} level={props.level + 1} onSelect={props.onSelect}></SubMenu>
  )
}

function SubMenu(props: {data: MenuData, level: number, onSelect: (chosen: string) => void}) {
  const [isOpen, setOpen] = useState(false);
  return (
    <>
      <MenuItem sx={{marginLeft: toPx((props.level - 1) * totalPx)}} onClick={() => setOpen(!isOpen)}>
        <ListItemIcon>
          {isOpen ? 
            <ArrowDropDown sx={iconSyle} />
            :
            <ArrowRight sx={iconSyle} />  
          }
        </ListItemIcon>
        <ListItemText>{props.data.name}</ListItemText>
      </MenuItem>
      {isOpen && props.data.subMenu?.map(o => <BasicMenu key={o.name} data={[o]} level={props.level} onSelect={props.onSelect}></BasicMenu>)}
    </>
  )
}

export default function CreateNodeMenu(props: Props) {
  const theme = useTheme();
  const baseColor = theme.palette.background.paper;
  const thumbColor = lighten(baseColor, 10 * 0.026);
  const scrollbarColor = lighten(baseColor, 2 * 0.026);

  const autoFocus = (element: HTMLElement | null) => element?.focus();
  return (
    props.isVisible && <Paper className="noFocusOutline" {...{tabIndex: -1}} ref={autoFocus} sx={{maxWidth: "100%", width: "fit-content", position: "absolute", zIndex: 1, left: props.position.x, top: props.position.y}} onBlur={(event) => {
      //Prevents closing the menu when focusing on child elements
      if (!event.currentTarget.contains(event.relatedTarget)) {
        props.setVisible(false);
      }
    }}>
      <MenuList dense sx={{maxHeight: "250px", overflowY: "scroll", scrollbarWidth: "thin", scrollbarColor: thumbColor + " " + scrollbarColor}}>
        <BasicMenu data={props.options} level={0} onSelect={props.onSelect}></BasicMenu>
      </MenuList>
    </Paper>
  )
}