import { invoke } from "@tauri-apps/api/core";
import { Mic2, Square } from "lucide-react";
import type { ClassifiedIntent } from "../lib/types";
import { useAppStore } from "../stores/app-store";

interface ExecutionResult {
  success: boolean;
  simulated: boolean;
  tool: string;
  message: string;
  data: Record<string, unknown>;
}

export function InvokeButton() {
  const { stage, setStage, setCurrentTranscription, addLogEntry } = useAppStore();

  const isRecording = stage === "recording";
  const isProcessing = stage === "processing" || stage === "agent_running";

  const handleClick = async () => {
    try {
      if (isRecording) {
        setStage("processing");
        const result = await invoke("stop_recording") as Record<string, unknown>;

        if (result.error) {
          addLogEntry({
            id: Date.now().toString(),
            timestamp: Date.now(),
            transcription: "",
            error: result.error as string,
            stage: "idle",
          });
          setStage("idle");
          return;
        }

        const text = (result.transcription as string) || "";
        setCurrentTranscription(text);

        const intent = result.intent as ClassifiedIntent | undefined;
        const execution = result.execution as ExecutionResult | undefined;

        addLogEntry({
          id: Date.now().toString(),
          timestamp: Date.now(),
          transcription: text,
          intent,
          result: execution?.message || (result.message as string) || "Done",
          stage: "idle",
        });

        setStage("idle");
      } else if (!isProcessing) {
        await invoke("start_recording");
        setStage("recording");
      }
    } catch (err) {
      console.error("Invoke error:", err);
      setStage("idle");
    }
  };

  const label = isRecording
    ? "Stop recording"
    : isProcessing
      ? "Processing..."
      : "Tap to speak";

  return (
    <button
      onClick={handleClick}
      disabled={isProcessing}
      className={`invoke-dock-button ${isRecording ? "is-recording" : ""} ${isProcessing ? "is-processing" : ""}`}
    >
      <span className="flex min-w-0 flex-1 items-center gap-3">
        <span className="text-[#66516a]">{label}</span>
      </span>
      <span className="invoke-dock-mic">
        {isRecording ? <Square size={17} fill="currentColor" /> : <Mic2 size={20} strokeWidth={1.8} />}
      </span>
    </button>
  );
}
