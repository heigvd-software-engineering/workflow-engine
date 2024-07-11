import { useContext } from "react";
import { WorkflowDataContext } from "./WorkflowDataProvider";

export const useWorkflowData = () => {
  return useContext(WorkflowDataContext);
};