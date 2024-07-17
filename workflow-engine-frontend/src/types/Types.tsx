//
// Other
//

import Prism from "prismjs";
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const _ = Prism;

// import "prismjs/components/prism-python";
export enum Languages {
  JS,
  // Python
}

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
  Object,
  File,
  Flow
}

//
// Sending data
//
export type WorkflowInstruction = 
  ICreateWorkflow
  | IExecuteWorkflow
  | ISaveWorkflow
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

export type ISaveWorkflow = WorkflowUUID & {
  action: "saveWorkflow";
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

export type ICreateFileNode = ICreateNode & {
  type: "file";
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
  language: keyof typeof Languages;
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
  | NLogChanged
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

export type NLogChanged = {
  notificationType: "logChanged";
  log: string;
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

export type NodeType = CodeNodeType | PrimitiveNodeType | FileNodeType;

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
  language: keyof typeof Languages;
}

export type PrimitiveNodeType = NodeBaseType & {
  nodeType: "PrimitiveNode";
  value: number | string;
}

export type FileNodeType = NodeBaseType & {
  nodeType: "FileNode";
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
  execErrors?: WorkflowNodeErrors[];
}

export type WorkflowStateType = {
  state: State;
  errors?: WorkflowError[];
}

//Errors
export type WorkflowError = WorkflowGeneralErrors | WorkflowNodeErrors;

export type ErrorMessage = {
  error: string;
}

export type WorkflowGeneralError = ErrorMessage & {
  type: "general";
}

export type WorkflowGeneralErrors = 
  CycleDetectedError 
  | EmptyGraphError 
  | NotConnectedGraphError 
  | WorkflowCancelledError
  ;

export type CycleDetectedError = WorkflowGeneralError & { errorType: "CycleDetected" }
export type EmptyGraphError = WorkflowGeneralError & { errorType: "EmptyGraph" }
export type NotConnectedGraphError = WorkflowGeneralError & { errorType: "NotConnectedGraph" }
export type WorkflowCancelledError = WorkflowGeneralError & { errorType: "WorkflowCancelled" }

export type InputConnectorError = {
  connectorType: "input";
}

export type OutputConnectorError = {
  connectorType: "output";
}

export type ConnectorError = (InputConnectorError | OutputConnectorError) & {
  connectorId: number;
};

export type WorkflowNodeError = ErrorMessage & {
  type: "node";
  nodeId: number;
}
export type WorkflowNodeErrors = 
  ErroredInputConnectorError
  | ExecutionTimeoutError
  | FailedExecutionError
  | IncompatibleTypesError
  | InputNotConnectedError
  | MissingOutputValueError
  | NameAlreadyUsedError
  | UnmodifiableConnectorError
  | WrongTypeError
  ;

export type ErroredInputConnectorError = WorkflowNodeError & InputConnectorError & { errorType: "ErroredInputConnector" }
export type ExecutionTimeoutError = WorkflowNodeError & { errorType: "ExecutionTimeout" }
export type FailedExecutionError = WorkflowNodeError & { errorType: "FailedExecution" }
export type IncompatibleTypesError = WorkflowNodeError & InputConnectorError & { errorType: "IncompatibleTypes" }
export type InputNotConnectedError = WorkflowNodeError & InputConnectorError & { errorType: "InputNotConnected" }
export type MissingOutputValueError = WorkflowNodeError & OutputConnectorError & { errorType: "MissingOutputValue" }
export type NameAlreadyUsedError = WorkflowNodeError & ConnectorError & { errorType: "NameAlreadyUsed" }
export type UnmodifiableConnectorError = WorkflowNodeError & ConnectorError & { errorType: "UnmodifiableConnector" }
export type WrongTypeError = WorkflowNodeError & OutputConnectorError & { errorType: "WrongType" }

//
// Documentation
//

export type DocClassValue = TypedValue<"class", DocumentClass>;
export type DocListValue = TypedValue<"list", ClassList>;
export type DocErrorValue = TypedValue<"error", string>;

export type DocResponse = DocClassValue | DocListValue | DocErrorValue;

export type TypedValue<T extends string, U> = {
  type: T,
  value: U
};

//Class list
export type ClassList = string[];

//Document
export type DocumentClass = {
  name: string;
  comment: string;
  methods: DocumentMethod[];
}

export type DocumentMethod = {
  name: string;
  type: string;
  comment: string;
  params: DocumentParameter[];
}

export type DocumentParameter = {
  name: string;
  type: string;
}