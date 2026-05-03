import { useEffect } from "react";
import { invoke } from "@tauri-apps/api/core";
import { listen } from "@tauri-apps/api/event";
import { useAppStore } from "./stores/app-store";
import { Onboarding } from "./components/Onboarding";
import { RecordingOverlay } from "./components/RecordingOverlay";
import { Settings } from "./components/Settings";
import { StatusIndicator } from "./components/StatusIndicator";
import { ActionLog } from "./components/ActionLog";
import { InvokeButton } from "./components/InvokeButton";

function App() {
  const { stage, settingsOpen, onboardingDone, toggleSettings, setStage, completeOnboarding } = useAppStore();

  useEffect(() => {
    // Load settings on mount
    invoke("get_settings").catch(() => {});
  }, []);

  // Poll status
  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        const status = await invoke("get_status");
        if (status && typeof status === "object" && "stage" in status) {
          const s = status as { stage: string; message: string };
          useAppStore.getState().setStage(s.stage as any);
          useAppStore.getState().setStatusMessage(s.message);
        }
      } catch {}
    }, 500);
    return () => clearInterval(interval);
  }, []);

  // Listen for global shortcut toggle-recording event
  useEffect(() => {
    const unlisten = listen("toggle-recording", () => {
      const currentStage = useAppStore.getState().stage;
      if (currentStage === "idle") {
        invoke("start_recording").then(() => setStage("recording")).catch(() => {});
      } else if (currentStage === "recording") {
        // Trigger stop via the button handler
        setStage("processing");
        invoke("stop_recording")
          .then((result) => {
            const r = result as Record<string, unknown>;
            if (r.error) {
              useAppStore.getState().addLogEntry({
                id: Date.now().toString(),
                timestamp: Date.now(),
                transcription: "",
                error: r.error as string,
                stage: "idle",
              });
            } else {
              const text = (r.transcription as string) || "";
              useAppStore.getState().setCurrentTranscription(text);
              useAppStore.getState().addLogEntry({
                id: Date.now().toString(),
                timestamp: Date.now(),
                transcription: text,
                intent: r.intent as any,
                result: (r.execution as any)?.message,
                stage: "idle",
              });
            }
          })
          .catch(() => {})
          .finally(() => setStage("idle"));
      }
    });

    return () => { unlisten.then(fn => fn()); };
  }, [setStage]);

  // Show onboarding if not done
  if (!onboardingDone) {
    return (
      <div className="h-screen bg-zinc-950 text-zinc-100">
        <Onboarding />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen bg-zinc-950 text-zinc-100">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-zinc-800">
        <div className="flex items-center gap-2">
          <span className="text-lg">🔮</span>
          <h1 className="text-sm font-semibold tracking-wide text-zinc-300">
            INVOKE
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <StatusIndicator />
          <button
            onClick={toggleSettings}
            className="p-1.5 rounded-md hover:bg-zinc-800 transition-colors text-zinc-500 hover:text-zinc-300"
          >
            ⚙️
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-hidden">
        {settingsOpen ? (
          <Settings />
        ) : (
          <div className="flex flex-col h-full">
            {/* Recording overlay area */}
            <div className="flex-1 flex items-center justify-center">
              <RecordingOverlay />
            </div>

            {/* Invoke button */}
            <div className="px-6 pb-4">
              <InvokeButton />
            </div>

            {/* Action log */}
            <div className="border-t border-zinc-800 max-h-48 overflow-y-auto">
              <ActionLog />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
