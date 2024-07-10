import { useCallback, useMemo, useState } from "react"
import { AvailableTypeNames, newType, stringFromType, Type, typeFromString, UNDEF_TYPE } from "../utils/alert/TypeUtils"
import { Box, IconButton } from "@mui/material";
import { Add, Close } from "@mui/icons-material";
import { availableChildren } from "../types/Types";
import LevelMenu from "./LevelMenu";

function TypeSelector(props: { stack: number[], type?: Type, openChooseTypeMenu: (event: React.MouseEvent<HTMLElement>, currentStack: number[]) => void, setTypeStack: (stack: number[], type?: Type) => void, editMode: boolean }) {
  if (props.type == undefined || props.type == UNDEF_TYPE) {
    if (!props.editMode) {
      return (<></>);
    }
    
    return (
      <IconButton size="small" onClick={(e) => props.openChooseTypeMenu(e, props.stack)}>
        <Add fontSize="inherit" color="success" />
      </IconButton>
    )
  }
  return (
    <Box sx={{display: "flex", alignItems: "center"}}>
      {
        props.editMode && <IconButton size="small" onClick={() => props.setTypeStack(props.stack, undefined)}>
          <Close fontSize="inherit" color="error" />
        </IconButton>
      }
      {props.type.name}
      {
        props.type.parameters.length != 0 && 
        <>
          &lt;
          {props.type.parameters.map((p, i) => (
            <Box key={i} sx={{display: "flex"}}>
              <TypeSelector type={p} stack={[...props.stack, i]} setTypeStack={props.setTypeStack} openChooseTypeMenu={props.openChooseTypeMenu} editMode={props.editMode} />
              {(i != props.type!.parameters.length - 1) && <span>,&nbsp;</span>}
            </Box>
          ))}
          &gt;
        </>
      }
    </Box>
  )
}

export default function ModifyType(props: { type: string, setType: (type: string) => void, editMode: boolean }) {
  const realType = useMemo(() => typeFromString(props.type), [props.type]);

  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState({x: 0, y: 0});
  const [currentStack, setCurrentStack] = useState<number[]>([]);

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

  const setTypeStack = useCallback((stack: number[], type?: Type) => {
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
    setOpen(false);
  }, [currentStack, setTypeStack]);

  return (
    <>
      <TypeSelector type={realType} stack={[]} setTypeStack={setTypeStack} openChooseTypeMenu={(e, currentStack) => {
        setCurrentStack(currentStack);
        setPosition({x: e.clientX, y: e.clientY});
        setOpen(true);
      }} editMode={props.editMode} />

      {props.editMode && <LevelMenu 
        isVisible={open} 
        onClose={() => setOpen(false)} 
        position={position} 
        setPostion={setPosition} 
        options={availableChildren(getAt(currentStack.slice(0, -1))?.name)}
        onSelect={(e) => chooseAndClose(e)}
      />}
    </>
  )
}