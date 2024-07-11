import { createContext, ReactNode, useMemo, useState } from "react";
import LevelMenu, { MenuData } from "../../components/LevelMenu";
import { BaseNodeTypeNode } from "../../nodes/BaseNode";
import { Edge, XYPosition } from "@xyflow/react";
import { $enum } from "ts-enum-util";
import { PrimitiveTypes } from "../../types/Types";

type ContextMenuVariant<T extends string, U extends object | never> = {
  name: T,
  data: U
};

export type ContextMenuMain = ContextMenuVariant<"main", never>;
export type ContextMenuNode = ContextMenuVariant<"node", BaseNodeTypeNode>;
export type ContextMenuEdge = ContextMenuVariant<"edge", Edge>;

export type ContextMenuVariants = ContextMenuMain | ContextMenuNode | ContextMenuEdge;

type ContextMenuContextType = {
  openMenu: (variant: ContextMenuVariants, position: XYPosition) => void,
  closeMenu: () => void
}

export const ContextMenuContext = createContext<ContextMenuContextType>({
  openMenu: () => {},
  closeMenu: () => {}
});

export default function ContextMenuProvider(props: { children: ReactNode, onSelect: (variant: ContextMenuVariants | undefined, position: XYPosition, choice: string) => void }) {
  const [contextMenuVariant, setContextMenuVariant] = useState<ContextMenuVariants | undefined>(undefined);
  const [contextMenuPosition, setContextMenuPosition] = useState<XYPosition>({x: 0, y: 0});

  const allPrimitives = useMemo(() => $enum(PrimitiveTypes).getKeys().map(p => {return {name: p}}), []);
  const menusNode: MenuData<string>[] = useMemo(() => [
    {name: "Code"}, 
    {
      name: "Primitive", 
      subMenu: allPrimitives
    }
  ], [allPrimitives]);

  const menusDelete: MenuData<string>[] = useMemo(() => [
    {name: "Delete"}
  ], []);

  const currentMenus = useMemo(() => {
    switch(contextMenuVariant?.name) {
      case "main":
        return menusNode;
      case "node":
        return menusDelete;
      case "edge":
        return menusDelete;
      case undefined:
        return [];
    }
  }, [contextMenuVariant, menusNode, menusDelete]);

  const openMenu = (variant: ContextMenuVariants, position: XYPosition) => {
    setContextMenuVariant(variant);
    setContextMenuPosition(position);
  }

  const closeMenu = () => {
    setContextMenuVariant(undefined);
  }

  return (
    <ContextMenuContext.Provider value={{openMenu, closeMenu}}>
      <>
        <LevelMenu
          options={currentMenus} 
          isVisible={contextMenuVariant != undefined} 
          position={contextMenuPosition} 
          onClose={closeMenu} 
          onSelect={(choice) => {
            props.onSelect(contextMenuVariant, contextMenuPosition, choice);
            closeMenu();
          }}
          setPostion={setContextMenuPosition} 
        />
        {props.children}
      </>
    </ContextMenuContext.Provider>
  )
}