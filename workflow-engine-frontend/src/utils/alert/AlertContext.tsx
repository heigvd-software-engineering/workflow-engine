import { createContext } from "react";

export const AlertContext = createContext<{ alertSuccess: (message: string) => void; alertWarning: (message: string) => void; alertInfo: (message: string) => void; alertError: (message: string) => void; }>({
  alertSuccess(message) {
    console.log(message);
  },
  alertWarning(message) {
    console.warn(message);
  },
  alertInfo(message) {
    console.info(message);
  },
  alertError(message) {
    console.error(message);
  },
});