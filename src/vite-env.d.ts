/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_INVOKE_LLM_ENDPOINT?: string;
  readonly VITE_INVOKE_LLM_MODEL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
