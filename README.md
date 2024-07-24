<div align="center">
  
  <h3 align="center">WorkflowEngine</h3>

  <p align="center">
    Build workflows effortlessly and without constraints
    <br />
    <a href="#️-installation"><strong>Installation »</strong></a>
    <br />
    <br />
    <img src="https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white" />
    <img src="https://img.shields.io/badge/Node.js-43853D?style=for-the-badge&logo=node.js&logoColor=white" />
    <img src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB" />
    <img src="https://img.shields.io/badge/Vite-FCEB28?style=for-the-badge&logo=vite&logoColor=646CFF" />
    <br />
    <img src="https://img.shields.io/badge/Java-C74634?style=for-the-badge&logo=java&logoColor=white" />
    <img src="https://img.shields.io/badge/Quarkus-888888?style=for-the-badge&logo=Quarkus&logoColor=4695EB" />
    <img src="https://img.shields.io/badge/GraalVM-F29111?style=for-the-badge" />
  </p>
</div>

## 💬 About this project
The goal of this project is to allow users to create workflow easily and without constraints. In the usual workflow executors, the user is usually constrained by the limited choice of tasks (nodes) available.

In this project, the usage of GraalVM polglot lets the programmer execute code in whichever language they want directly as a node. It is possible to have separate nodes in the workflow that have a different language. WorkflowExecutor will handle the type verification and the transfer from a language to another.

To create, modifiy and view the workflow and its status during the execution, a web UI has been developped. Everything can be controlled from this website.

## 🛠️ Installation
### 🖥️ Backend
Install the Java Development Kit 17: https://www.oracle.com/java/technologies/downloads/#jdk17-windows

### 🌐 Frontend
Install Node.js: https://nodejs.org/en/download/prebuilt-installer/current

In `📂 workflow_frontend`, run the following command to install the dependencies:
```sh
npm i
```

## 🚀 Running the project
### 🖥️ Backend
In `📂 workflow_backend`, run the following command to start the backend (at http://localhost:8080):
#### Prod mode
```sh
./mvnw compile quarkus:run
```
#### Dev mode
```sh
./mvnw compile quarkus:dev
```

### 🌐 Frontend
In `📂 workflow_frontend`, run the following command to start the frontend (at http://localhost:5173):
#### Prod mode
```sh
npm run dev
```
#### Dev mode
```sh
npm run prod
```

## 📃 License
Distributed under the Apache License, Version 2.0. See `LICENSE` for more information.