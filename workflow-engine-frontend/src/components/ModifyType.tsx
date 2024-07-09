import { useCallback, useMemo, useState } from "react"
import { AvailableTypeNames, newType, stringFromType, Type, typeFromString, UNDEF_TYPE } from "../utils/alert/TypeUtils"
import { IconButton, Menu, MenuItem } from "@mui/material";
import { Add } from "@mui/icons-material";
import { availableChildren } from "../types/Types";

function TypeSelector(props: { stack: number[], type?: Type, openChooseTypeMenu: (event: React.MouseEvent<HTMLElement>, currentStack: number[]) => void }) {
  if (props.type == undefined || props.type == UNDEF_TYPE) {
    return (
      <IconButton size="small" onClick={(e) => props.openChooseTypeMenu(e, props.stack)}>
        <Add fontSize="inherit" />
      </IconButton>
    )
  }
  return (
    <span>
      {props.type.name}
      {
        props.type.parameters.length != 0 && 
        <>
          &lt;
          {props.type.parameters.map((p, i) => (
            <span key={i}>
              <TypeSelector type={p} stack={[...props.stack, i]} openChooseTypeMenu={props.openChooseTypeMenu} />
              {(i != props.type!.parameters.length - 1) && <span>, </span>}
            </span>
          ))}
          &gt;
        </>
      }
    </span>
  )
}

export default function ModifyType(props: { type: string, setType: (type: string) => void }) {
  const realType = useMemo(() => typeFromString(props.type), [props.type]);

  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const [currentStack, setCurrentStack] = useState<number[]>([]);
  const open = Boolean(anchorEl);

  const close = useCallback(() => {
    setAnchorEl(null);
  }, []);

  const getAt = useCallback((stack: number[]) => {
    const copyRealType: Type | undefined = realType == undefined ? undefined : JSON.parse(JSON.stringify(realType));
    if (stack.length == 0) {
      return copyRealType;
    } else {
      let loopCopy = copyRealType;
      for (let i = 0; i < stack.length - 1; ++i) {
        loopCopy = loopCopy?.parameters[stack[i]];
      }
      if (loopCopy == undefined) {
        return;
      }
      return loopCopy.parameters[stack[stack.length - 1]];
    }
  }, [realType]);

  const setTypeStack = useCallback((stack: number[], type: Type) => {
    let copyRealType: Type | undefined = realType == undefined ? undefined : JSON.parse(JSON.stringify(realType));
    if (stack.length == 0) {
      copyRealType = type;
    } else {
      let loopCopy = copyRealType;
      for (let i = 0; i < stack.length - 1; ++i) {
        loopCopy = loopCopy?.parameters[stack[i]];
      }
      if (loopCopy == undefined) {
        return;
      }
      loopCopy.parameters[stack[stack.length - 1]] = type;
    }
    props.setType(stringFromType(copyRealType));
  }, [props, realType]);

  const chooseAndClose = useCallback((value: AvailableTypeNames) => {
    setTypeStack(currentStack, newType(value))
    close();
  }, [currentStack, setTypeStack, close]);

  return (
    <>
      <TypeSelector type={realType} stack={[]} openChooseTypeMenu={(e, currentStack) => {
        setCurrentStack(currentStack);
        setAnchorEl(e.currentTarget);
      }} />

      <Menu open={open} onClose={close} anchorEl={anchorEl}>
        {availableChildren(getAt(currentStack.slice(0, -1))?.name).map(entry => (
          <MenuItem key={entry} onClick={() => chooseAndClose(entry)}>{entry}</MenuItem>
        ))}
      </Menu>
    </>
  )
}