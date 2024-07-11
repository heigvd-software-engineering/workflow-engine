import { createContext, ReactNode } from "react";
import { WorkflowInstruction } from "../../types/Types";
import { SendJsonMessage } from "react-use-websocket/dist/lib/types";

type WorkflowSocketContext = {
  sendMessage: (instruction: WorkflowInstruction) => void
}

export const WorkflowSocketContext = createContext<WorkflowSocketContext>({
  sendMessage: () => {}
});

export default function WorkflowSocketProvider(props: { children: ReactNode, sendMessage: SendJsonMessage }) {
  return (
    <WorkflowSocketContext.Provider value={{sendMessage: props.sendMessage}}>
      {props.children}
    </WorkflowSocketContext.Provider>
  )
}