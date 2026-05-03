import { invoke } from "@tauri-apps/api/core";
import { useAppStore } from "../stores/app-store";
import type { ClassifiedIntent } from "../lib/types";

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

        // Extract intent and execution from result
        const intent = result.intent as ClassifiedIntent | undefined;
        const execution = result.execution as ExecutionResult | undefined;

        addLogEntry({
          id: Date.now().toString(),
          timestamp: Date.now(),
          transcription: text,
          intent: intent,
          result: execution?.message || "Done",
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

  let buttonClass = "w-full py-3 rounded-xl font-medium text-sm tracking-wide transition-all duration-200";
  let label = "🔮 Tap to Speak";

  if (isRecording) {
    buttonClass += " bg-red-600 hover:bg-red-700 text-white animate-pulse";
    label = "🔴 Recording... Tap to Stop";
  } else if (isProcessing) {
    buttonClass += " bg-violet-900/50 text-violet-300 cursor-wait";
    label = "⏳ Processing...";
  } else {
    buttonClass += " bg-violet-600 hover:bg-violet-700 text-white active:scale-[0.98]";
  }

  return (
    <button onClick={handleClick} disabled={isProcessing} className={buttonClass}>
      {label}
    </button>
  );
}
