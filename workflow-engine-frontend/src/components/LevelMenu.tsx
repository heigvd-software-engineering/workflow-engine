import { ArrowDropDown, ArrowRight } from "@mui/icons-material"
import { lighten, ListItemIcon, ListItemText, Menu, MenuItem, useTheme } from "@mui/material"
import { XYPosition } from "@xyflow/react"
import { useState } from "react"

export type MenuData<T extends string> = {
  name: T,
  subMenu?: never
} | {
  name: string,
  subMenu: MenuData<T>[]
}

type Props<T extends string> = {
  options: MenuData<T>[],
  onSelect: (chosen: T) => void,
  isVisible: boolean,
  onClose: () => void,
  position: XYPosition,
  setPostion: (position: XYPosition) => void
}

const iconWidthPx = 20;
const iconMarginRightPx = 5;
const totalPx = iconWidthPx + iconMarginRightPx;

const toPx = (px: number) => px + "px";

const iconSyle = {width: toPx(iconWidthPx), marginRight: toPx(iconMarginRightPx)}

function BasicMenu<T extends string>(props: {data: MenuData<T>[], level: number, onSelect: (chosen: T) => void, onHeightChanged: () => void}) {
  return props.data.map(o => 
    o.subMenu == undefined ?
      <MenuItem sx={{marginLeft: toPx(props.level * totalPx)}} key={o.name} onClick={() => props.onSelect(o.name)}>
        <ListItemText>{o.name}</ListItemText>
      </MenuItem>
      :
      <SubMenu key={o.name} data={o} level={props.level + 1} onSelect={props.onSelect} onHeightChanged={props.onHeightChanged}></SubMenu>
  )
}

function SubMenu<T extends string>(props: {data: MenuData<T>, level: number, onSelect: (chosen: T) => void, onHeightChanged: () => void}) {
  const [isOpen, setOpen] = useState(false);

  return (
    <>
      <MenuItem sx={{marginLeft: toPx((props.level - 1) * totalPx)}} onClick={() => {
        setOpen(!isOpen);
        props.onHeightChanged();
      }}>
        <ListItemIcon>
          {isOpen ? 
            <ArrowDropDown sx={iconSyle} />
            :
            <ArrowRight sx={iconSyle} />  
          }
        </ListItemIcon>
        <ListItemText>{props.data.name}</ListItemText>
      </MenuItem>
      {isOpen && props.data.subMenu?.map(o => <BasicMenu key={o.name} data={[o]} level={props.level} onSelect={props.onSelect} onHeightChanged={props.onHeightChanged}></BasicMenu>)}
    </>
  )
}

export default function LevelMenu<T extends string>(props: Props<T>) {
  const theme = useTheme();
  const baseColor = theme.palette.background.paper;
  const thumbColor = lighten(baseColor, 10 * 0.026);
  const scrollbarColor = lighten(baseColor, 2 * 0.026);

  return (
    <Menu open={props.isVisible} onClose={props.onClose} anchorReference="anchorPosition" anchorPosition={{left: props.position.x, top: props.position.y}} sx={{scrollbarColor: thumbColor + " " + scrollbarColor}}>
      <BasicMenu data={props.options} level={0} onSelect={props.onSelect} onHeightChanged={() => {
        //Forces the refresh of the position => updates the position of the menu to avoid going out of the screen
        props.setPostion({x: props.position.x, y: props.position.y});
      }} />
    </Menu>
  )
}