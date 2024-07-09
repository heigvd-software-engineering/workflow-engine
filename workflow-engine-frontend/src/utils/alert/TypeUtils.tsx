import { $enum } from "ts-enum-util";
import { numOfParamsFor, PrimitiveTypes, TypesNames } from "../../types/Types";

export type AvailableTypeNames = keyof typeof TypesNames | keyof typeof PrimitiveTypes;

export function availableTypeNamesFromString(str: string): AvailableTypeNames | undefined {
  let key: AvailableTypeNames | undefined = $enum(TypesNames).asKeyOrDefault(str, undefined);
  if (key == undefined) {
    key = $enum(PrimitiveTypes).asKeyOrDefault(str, undefined)
  }
  return key;
}

export type Type = {
  name?: AvailableTypeNames,
  parameters: (Type | undefined)[]
}
export const UNDEF_TYPE: Type = {
  name: undefined,
  parameters: []
}

export function newType(type: AvailableTypeNames): Type {
  return {name: type, parameters: Array(numOfParamsFor(type)).fill(UNDEF_TYPE)}
}

export function newTypeNoParam(type: AvailableTypeNames): Type {
  return {name: type, parameters: []}
}

export function typeFromString(type: string): Type | undefined {
  const types = type.split(" ");

  function buildStructure(words: string[]): Type | undefined {
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

    const realName = availableTypeNamesFromString(name);
    if (realName == undefined) {
      return UNDEF_TYPE;
    }
    
    const parameters: Type[] = [];
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

export function stringFromType(type: Type | undefined): string {
  if (type == undefined) {
    return "undefined";
  }
  return type.name + (type.parameters.length != 0 ? " " + type.parameters.map(t => stringFromType(t)).join(" ") : "");
}