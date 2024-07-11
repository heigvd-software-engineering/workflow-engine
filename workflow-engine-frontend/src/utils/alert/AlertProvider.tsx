import { useSnackbar, VariantType } from "notistack";
import { createContext, ReactNode } from "react";

export const AlertContext = createContext({
  alertSuccess(message: string) {
    console.log(message);
  },
  alertWarning(message: string) {
    console.warn(message);
  },
  alertInfo(message: string) {
    console.info(message);
  },
  alertError(message: string) {
    console.error(message);
  },
});

export default function AlertProvider(props: { children: ReactNode }) {
  const { enqueueSnackbar } = useSnackbar();
  
  const alertBase = (variant: VariantType) => (message: string) => enqueueSnackbar(message, { variant: variant });
  const alertSuccess = alertBase("success");
  const alertWarning = alertBase("warning");
  const alertInfo = alertBase("info");
  const alertError = alertBase("error");

  return (
    <AlertContext.Provider value={{alertSuccess, alertWarning, alertInfo, alertError}}>
      {props.children}
    </AlertContext.Provider>
  )
}