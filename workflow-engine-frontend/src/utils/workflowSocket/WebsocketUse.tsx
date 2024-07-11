import { useContext } from "react";
import { WorkflowSocketContext } from "./WorkflowSocketProvider";

export const useWorkflowSocket = () => {
  return useContext(WorkflowSocketContext);
};