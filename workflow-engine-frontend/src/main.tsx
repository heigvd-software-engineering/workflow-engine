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

const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />
  },
  {
    path: "/workflows",
    element: <WorkflowsPage />
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
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider router={router} />
    </ThemeProvider>
  </React.StrictMode>
);
