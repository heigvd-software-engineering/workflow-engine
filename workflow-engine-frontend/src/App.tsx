import { ThemeProvider } from "@emotion/react"
import { createTheme, CssBaseline } from "@mui/material"
import { SnackbarProvider } from "notistack"
import { createBrowserRouter, RouterProvider } from "react-router-dom"
import AlertProvider from "./utils/alert/AlertProvider"
import { ReactFlowProvider } from "@xyflow/react"
import { Documentation } from "./Documentation"
import HomePage from "./HomePage"
import NotFoundPage from "./NotFoundPage"
import WorkflowDataProvider from "./utils/workflowData/WorkflowDataProvider"
import WorkflowsPage from "./WorkflowsPage"

const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />
  },
  {
    path: "/workflows",
    element: 
    <ReactFlowProvider>
      <WorkflowDataProvider>
        <WorkflowsPage />
      </WorkflowDataProvider>
    </ReactFlowProvider>
  },
  {
    path: "/documentation",
    element: <Documentation />,
    children: [
      {
        path: ":type",
        element: <Documentation />
      }
    ]
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

export default function App() {
  return (
    <SnackbarProvider maxSnack={5} anchorOrigin={{horizontal: "right", vertical: "bottom"}}>
      <AlertProvider>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router} />
        </ThemeProvider>
      </AlertProvider>
    </SnackbarProvider>
  )
}