import { useSnackbar } from "notistack";
import { ReactNode } from "react";
import { AlertContext } from "./AlertContext";

type Props = {
  children: ReactNode
}

export default function AlertProvider(props: Props) {
  const { enqueueSnackbar } = useSnackbar();
  
  const alertSuccess = (message: string) => enqueueSnackbar(message, { variant: "success" });
  const alertWarning = (message: string) => enqueueSnackbar(message, { variant: "warning" });
  const alertInfo = (message: string) => enqueueSnackbar(message, { variant: "info" });
  const alertError = (message: string) => enqueueSnackbar(message, { variant: "error" });

  return (
    <AlertContext.Provider value={{alertSuccess, alertWarning, alertInfo, alertError}}>
      {props.children}
    </AlertContext.Provider>
  )
}