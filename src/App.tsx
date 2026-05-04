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
import { Settings2, Sparkle } from "lucide-react";

function App() {
  const { settingsOpen, onboardingDone, toggleSettings, setStage } = useAppStore();

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
      <div className="h-[100dvh] overflow-hidden desktop-shell text-[#351e38]">
        <Onboarding />
      </div>
    );
  }

  return (
    <div className="flex h-[100dvh] flex-col overflow-hidden desktop-shell text-[#351e38]">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4">
        <div className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-full border border-white/70 bg-white/45 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]">
            <Sparkle size={16} strokeWidth={1.7} />
          </div>
          <h1 className="font-serif text-xl font-normal text-[#351e38]">
            Invoke
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <StatusIndicator />
          <button
            onClick={toggleSettings}
            aria-label={settingsOpen ? "Close settings" : "Open settings"}
            className="rounded-full border border-white/60 bg-white/45 p-2 text-[#66516a] transition hover:bg-white/70 active:scale-[0.98]"
          >
            <Settings2 size={16} strokeWidth={1.8} />
          </button>
        </div>
      </div>

      {/* Main Content */}
      <div className="min-h-0 flex-1 overflow-hidden px-5 pb-5">
        {settingsOpen ? (
          <Settings />
        ) : (
          <div className="flex h-full flex-col">
            {/* Recording overlay area */}
            <div className="flex min-h-0 flex-1 items-center justify-center">
              <RecordingOverlay />
            </div>

            {/* Invoke button */}
            <div className="pb-4">
              <InvokeButton />
            </div>

            {/* Action log */}
            <div className="max-h-48 overflow-y-auto rounded-[24px] border border-white/60 bg-white/35">
              <ActionLog />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
