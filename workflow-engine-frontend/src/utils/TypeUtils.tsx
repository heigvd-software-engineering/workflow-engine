import { $enum } from "ts-enum-util";
import { Languages, PrimitiveTypes, TypesNames } from "../types/Types";
import { MenuData } from "../components/LevelMenu";

export type AvailableTypeNames = keyof typeof TypesNames | keyof typeof PrimitiveTypes;

export function availableTypeNamesFromString(str: string): AvailableTypeNames | undefined {
  let key: AvailableTypeNames | undefined = $enum(TypesNames).asKeyOrDefault(str, undefined);
  if (key == undefined) {
    key = $enum(PrimitiveTypes).asKeyOrDefault(str, undefined)
  }
  return key;
}

export type WType = {
  name?: AvailableTypeNames,
  parameters: (WType | undefined)[]
}

export const UNDEF_TYPE: WType = {
  name: undefined,
  parameters: []
}

export function newType(type: AvailableTypeNames): WType {
  return {name: type, parameters: Array(numOfParamsFor(type)).fill(UNDEF_TYPE)}
}

export function newTypeNoParam(type: AvailableTypeNames): WType {
  return {name: type, parameters: []}
}

export function typeFromString(type: string): WType | undefined {
  const types = type.split(" ");

  function buildStructure(words: string[]): WType | undefined {
    if (words.length === 0) {
      return undefined;
    }
    
    const name = words.shift();
    if (name == undefined || name == "") {
      return undefined;
    }

    if (name == "undefined") {
      return UNDEF_TYPE;
    }

    if (name == "Primitive") {
      const param = buildStructure(words);
      return { name: param?.name, parameters: [] }
    }

    const realName = availableTypeNamesFromString(name);
    if (realName == undefined) {
      return UNDEF_TYPE;
    }
    
    const parameters: WType[] = [];
    for (let i = 0; i < numOfParamsFor(realName); ++i) {
      const param = buildStructure(words);
      if (param == undefined) {
        return undefined;
      }
      parameters.push(param);
    }
    
    return { name: realName, parameters };
  }

  return buildStructure(types);
}

export function stringFromType(type: WType | undefined): string {
  if (type == undefined) {
    return "undefined";
  }

  let name = type.name;
  if ($enum(PrimitiveTypes).isKey(type.name)) {
    name = "Primitive " + type.name;
  }

  return name + (type.parameters.length != 0 ? " " + type.parameters.map(t => stringFromType(t)).join(" ") : "");
}

export type GrammarLanguageName = "js" | "python" | "";

export function getGrammarLanguageName(languageName: keyof typeof Languages): GrammarLanguageName {
  switch(languageName) {
    case "JS":
      return "js";
    // case "Python":
    //   return "python";
    default:
      console.error(languageName + "not found");
  }
  return "";
}

function canBeUsedAsT(name?: AvailableTypeNames): boolean {
  if (name == undefined) {
    return false;
  }
  switch(name) {
    case "Flow":
      return false;
    case "File":
      return false;
    default: 
      return true;
  }
}

function numOfParamsFor(name: AvailableTypeNames) {
  switch(name) {
    case "Map": return 2;
    case "Collection": return 1;
    default: return 0;
  }
}

function getAllChildren(): MenuData<AvailableTypeNames>[] {
  return $enum(TypesNames).getKeys().map(t => {
    return { name: t } as MenuData<AvailableTypeNames>
  }).concat([{
    name: "Primitive",
    subMenu: $enum(PrimitiveTypes).getKeys().map(t => {
      return { name : t }
    })
  }]);
}

function checkIfCanBeUsed(data: MenuData<AvailableTypeNames>): boolean {
  if (data.subMenu == undefined) {
    return canBeUsedAsT(data.name);
  } else {
    return data.subMenu.every(other => checkIfCanBeUsed(other));
  }
}

export function availableChildren(name: AvailableTypeNames | undefined): MenuData<AvailableTypeNames>[] {
  switch(name) {
    case "Map":
    case "Collection":
      return getAllChildren().filter(checkIfCanBeUsed);
    default: 
      return getAllChildren();
  }
}