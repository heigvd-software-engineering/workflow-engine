//Base
export type WorkflowNotification = 
  NClear
  | NAllWorkflows 
  | NWorkflowError
  | NNodeModified
  | NNodeRemoved
  | NNewWorkflow
  | NWorkflowRemoved
  | NNodeState
  | NWorkflowState
  ;

export type NClear = {
  notificationType: "clear";
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
  id: number;
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