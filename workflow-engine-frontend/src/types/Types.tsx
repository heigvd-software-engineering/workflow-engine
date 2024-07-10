//
// Other
//

import { $enum } from "ts-enum-util";
import { AvailableTypeNames } from "../utils/alert/TypeUtils";
import { MenuData } from "../components/LevelMenu";

export enum PrimitiveTypes {
  Integer,
  String,
  Boolean,
  Byte,
  Short,
  Long,
  Float,
  Double,
  Character
}

export enum TypesNames {
  Map,
  Collection,
  Flow
}

function canBeUsedAsT(name?: AvailableTypeNames): boolean {
  if (name == undefined) {
    return false;
  }
  switch(name) {
    case "Flow":
      return false;
    default: 
      return true;
  }
}

export function numOfParamsFor(name: AvailableTypeNames) {
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

//
// Sending data
//
export type WorkflowInstruction = 
  ICreateWorkflow
  | IExecuteWorkflow
  | IRemoveWorkflow
  | IStopWorkflow
  | ISwitchTo
  | ICreateNode
  | IRemoveNode
  | IMoveNode
  | ICreateConnector
  | IRemoveConnector
  | IChangeConnector
  | IChangeModifiableNode
  | IChangePrimitiveNode
  | IChangeCodeNode
  | IConnect
  | IDisconnect
  ;

type WorkflowUUID = {
  uuid: string;
}

type NodeID = WorkflowUUID & {
  nodeId: number;
}

type ConnectorID = NodeID & {
  connectorId: number;
  isInput: boolean;
}

export type ICreateWorkflow = {
  action: "createWorkflow";
  name: string;
}

export type IExecuteWorkflow = WorkflowUUID & {
  action: "executeWorkflow";
}

export type IRemoveWorkflow = WorkflowUUID & {
  action: "removeWorkflow";
}

export type IStopWorkflow = WorkflowUUID & {
  action: "stopWorkflow";
}

export type ISwitchTo = WorkflowUUID & {
  action: "switchTo";
}

export type IRemoveNode = NodeID & {
  action: "removeNode";
}

export type IMoveNode = NodeID & {
  action: "moveNode";
  nodeId: number;
  posX: number;
  posY: number;
}

export type ICreateConnector = NodeID & {
  action: "createConnector";
  isInput: boolean;
  name: string;
  type: string;
}

export type IRemoveConnector = ConnectorID & {
  action: "removeConnector";
}

// createNode

export type ICreateNode = WorkflowUUID & {
  action: "createNode";
  posX: number;
  posY: number;
}

export type ICreateCodeNode = ICreateNode & {
  type: "code";
}

export type ICreatePrimitiveNode = ICreateNode & {
  type: "primitive";
  primitive: string;
}

// changeConnector

export type IChangeConnector = ConnectorID & {
  action: "changeConnector";
}

export type ITypeChangeConnector = IChangeConnector & {
  subAction: "type";
  newType: string;
}

export type INameChangeConnector = IChangeConnector & {
  subAction: "name";
  newName: string;
}

// changeModifiableNode

export type IChangeModifiableNode = NodeID & {
  action: "changeModifiableNode";
}

export type IIsDeterministicChangeModifiableNode = IChangeModifiableNode & {
  subAction: "isDeterministic";
  isDeterministic: boolean;
}

export type ITimeoutChangeModifiableNode = IChangeModifiableNode & {
  subAction: "timeout";
  timeout: number;
}

// changePrimitiveNode

export type IChangePrimitiveNode = NodeID & {
  action: "changePrimitiveNode";
}

export type IValueChangePrimitiveNode = IChangePrimitiveNode & {
  subAction: "value";
  value: number | string | boolean;
}

// changeCodeNode

export type IChangeCodeNode = NodeID & {
  action: "changeCodeNode";
}

export type ICodeChangeCodeNode = IChangeCodeNode & {
  subAction: "code";
  code: string;
}

export type ILanguageChangeCodeNode = IChangeCodeNode & {
  subAction: "language";
  language: string;
}

// Connexions

export type IConnect = WorkflowUUID & {
  action: "connect",
  fromNodeId: number;
  toNodeId: number;
  fromConnectorId: number;
  toConnectorId: number;
}

export type IDisconnect = NodeID & {
  action: "disconnect",
  connectorId: number;
}

//
// Receiving data
//

//Base
export type WorkflowNotification = 
  NSwitchedTo
  | NAllWorkflows 
  | NWorkflowError
  | NNodeModified
  | NNodeRemoved
  | NNewWorkflow
  | NWorkflowRemoved
  | NNodeState
  | NWorkflowState
  ;

export type NSwitchedTo = {
  notificationType: "switchedTo";
  uuid: string;
}

export type NAllWorkflows = {
  notificationType: "workflows";
  workflows: WorkflowType[];
}

export type NWorkflowError = {
  notificationType: "error";
  error: string;
}

export type NNodeModified = {
  notificationType: "node";
  node: NodeType;
}

export type NNodeRemoved = {
  notificationType: "nodeRemoved";
  nodeId: number;
}

export type NNewWorkflow = {
  notificationType: "newWorkflow";
  workflow: WorkflowType;
}

export type NWorkflowRemoved = {
  notificationType: "deletedWorkflow";
  workflowUUID: string;
}

export type NNodeState = {
  notificationType: "nodeState";
  nodeState: NodeStateType;
}

export type NWorkflowState = {
  notificationType: "workflowState";
  workflowState: WorkflowStateType;
}

//Types
export type WorkflowType = {
  uuid: string;
  name: string;
}

export type NodeType = CodeNodeType | PrimitiveNodeType;

export type NodeBaseType = {
  id: number;
  isDeterministic: boolean;
  timeout: number;
  inputs: InputConnector[];
  outputs: OutputConnector[];
}

export type CodeNodeType = NodeBaseType & {
  nodeType: "CodeNode";
  code: string;
  language: string;
}

export type PrimitiveNodeType = NodeBaseType & {
  nodeType: "PrimitiveNode";
  value: number | string;
}

export type Connector = {
  id: number;
  name: string;
  type: string;
  isReadOnly: boolean;
  isOptional: boolean;
}

export type InputConnector = Connector & {
  connectedTo?: ConnectedTo;
}

export type OutputConnector = Connector & {
  connectedTo: ConnectedTo[];
}

export type ConnectedTo = {
  nodeId: number;
  connectorId: number;
}

export type NodeRemovedType = {
  nodeId: number;
}

export type State = "IDLE" | "RUNNING" | "FAILED" | "FINISHED";

export type NodeStateType = {
  nodeId: number;
  state: State;
  hasBeenModified: boolean;
  posX: number;
  posY: number;
  errors?: NodeErrorType[];
}

export type NodeErrorType = {
  inputId: number;
  message: string;
}

export type WorkflowStateType = {
  state: State;
  errors?: WorkflowError[];
}

//Errors
export type WorkflowError = 
  CycleDetectedError 
  | EmptyGraphError 
  | NotConnectedGraphError 
  | WorkflowCancelledError
  | ErroredInputConnectorError
  | ExecutionTimeoutError
  | FailedExecutionError
  | IncompatibleTypesError
  | InputNotConnectedError
  | MissingOutputValueError
  | NameAlreadyUsedError
  ;

export type ErrorMessage = {
  error: string;
}

export type CycleDetectedError = ErrorMessage & { errorType: "CycleDetected" }
export type EmptyGraphError = ErrorMessage & { errorType: "EmptyGraph" }
export type NotConnectedGraphError = ErrorMessage & { errorType: "NotConnectedGraph" }
export type WorkflowCancelledError = ErrorMessage & { errorType: "WorkflowCancelled" }

export type InputConnectorError = {
  connectorType: "input"
  inputConnectorId: number;
}

export type OutputConnectorError = {
  connectorType: "output"
  outputConnectorId: number;
}

export type ConnectorError = InputConnectorError | OutputConnectorError;

export type WorkflowNodeError = {
  nodeId: number;
}

export type ErroredInputConnectorError = WorkflowNodeError & ErrorMessage & InputConnectorError & { errorType: "ErroredInputConnector" }
export type ExecutionTimeoutError = WorkflowNodeError & ErrorMessage & { errorType: "ExecutionTimeout" }
export type FailedExecutionError = WorkflowNodeError & ErrorMessage & { errorType: "FailedExecution" }
export type IncompatibleTypesError = WorkflowNodeError & ErrorMessage & InputConnectorError & OutputConnectorError & { errorType: "IncompatibleTypes" }
export type InputNotConnectedError = WorkflowNodeError & ErrorMessage & InputConnectorError & { errorType: "InputNotConnected" }
export type MissingOutputValueError = WorkflowNodeError & ErrorMessage & OutputConnectorError & { errorType: "MissingOutputValue" }
export type NameAlreadyUsedError = WorkflowNodeError & ErrorMessage & ConnectorError & { errorType: "NameAlreadyUsed" }
export type UnmodifiableConnectorError = WorkflowNodeError & ErrorMessage & ConnectorError & { errorType: "UnmodifiableConnector" }
export type WrongTypeError = WorkflowNodeError & ErrorMessage & OutputConnectorError & { errorType: "WrongType" }