import "./css/index.css";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";

import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.tsx";

ReactDOM.createRoot(document.getElementById('root')!).render(
  import.meta.env.MODE == "production" ?
    <App />
    :
    <React.StrictMode>
      <App />
    </React.StrictMode>
);
