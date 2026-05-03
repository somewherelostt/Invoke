import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { Stage, AppSettings, ActionLogEntry } from "../lib/types";

interface AppState {
  // Connection status
  stage: Stage;
  statusMessage: string;

  // Settings
  settings: AppSettings;
  settingsOpen: boolean;

  // Action log
  actionLog: ActionLogEntry[];

  // Transcription
  currentTranscription: string;

  // Onboarding
  onboardingDone: boolean;

  // Actions
  setStage: (stage: Stage) => void;
  setStatusMessage: (msg: string) => void;
  updateSettings: (settings: Partial<AppSettings>) => void;
  toggleSettings: () => void;
  addLogEntry: (entry: ActionLogEntry) => void;
  setCurrentTranscription: (text: string) => void;
  completeOnboarding: () => void;
  reset: () => void;
}

const defaultSettings: AppSettings = {
  hotkey: "Alt+Space",
  whisper_model: "tiny",
  llm_endpoint: "http://localhost:11434",
  llm_model: "qwen3:0.6b",
  composio_api_key: "",
  auto_paste: true,
  confirm_actions: true,
};

export const useAppStore = create<AppState>()(
  immer((set) => ({
    stage: "idle",
    statusMessage: "Ready",
    settings: defaultSettings,
    settingsOpen: false,
    actionLog: [],
    currentTranscription: "",
    onboardingDone: false,

    setStage: (stage) =>
      set((state) => {
        state.stage = stage;
        state.statusMessage =
          stage === "idle"
            ? "Ready"
            : stage === "recording"
              ? "Listening..."
              : stage === "processing"
                ? "Processing..."
                : "Executing action...";
      }),

    setStatusMessage: (msg) =>
      set((state) => {
        state.statusMessage = msg;
      }),

    updateSettings: (partial) =>
      set((state) => {
        Object.assign(state.settings, partial);
      }),

    toggleSettings: () =>
      set((state) => {
        state.settingsOpen = !state.settingsOpen;
      }),

    addLogEntry: (entry) =>
      set((state) => {
        state.actionLog.unshift(entry);
        if (state.actionLog.length > 50) state.actionLog.pop();
      }),

    setCurrentTranscription: (text) =>
      set((state) => {
        state.currentTranscription = text;
      }),

    completeOnboarding: () =>
      set((state) => {
        state.onboardingDone = true;
      }),

    reset: () =>
      set((state) => {
        state.stage = "idle";
        state.statusMessage = "Ready";
        state.currentTranscription = "";
      }),
  }))
);
