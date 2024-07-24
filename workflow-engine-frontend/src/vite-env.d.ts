/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly IS_DEV: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}