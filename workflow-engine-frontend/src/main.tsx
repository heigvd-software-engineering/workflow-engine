import "./css/index.css";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";

import React from "react";
import ReactDOM from "react-dom/client";
import HomePage from "./HomePage.tsx";
import { RouterProvider, createBrowserRouter } from "react-router-dom";
import { CssBaseline, ThemeProvider, createTheme } from "@mui/material";
import NotFoundPage from "./NotFoundPage.tsx";
import WorkflowsPage from "./WorkflowsPage.tsx";
import { SnackbarProvider } from "notistack";
import AlertProvider from "./utils/alert/AlertProvider.tsx";
import { ReactFlowProvider } from "reactflow";

const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />
  },
  {
    path: "/workflows",
    element: 
    <ReactFlowProvider>
      <WorkflowsPage />
    </ReactFlowProvider>
  }, 
  {
    path: "*",
    element: <NotFoundPage />
  }
]);

const theme = createTheme({
  palette: {
    mode: 'dark',
  }
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <SnackbarProvider maxSnack={5} anchorOrigin={{horizontal: "right", vertical: "bottom"}}>
      <AlertProvider>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router} />
        </ThemeProvider>
      </AlertProvider>
    </SnackbarProvider>
  </React.StrictMode>
);
