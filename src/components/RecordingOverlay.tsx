import { useAppStore } from "../stores/app-store";
import { TOOL_LABELS } from "../lib/types";

export function RecordingOverlay() {
  const { stage, currentTranscription, statusMessage, actionLog } = useAppStore();
  const lastAction = actionLog[0];

  if (stage === "idle" && !currentTranscription && !lastAction?.result) {
    return (
      <div className="text-center text-zinc-600 px-8">
        <div className="text-4xl mb-3">🔮</div>
        <p className="text-sm">Press <kbd className="px-1.5 py-0.5 bg-zinc-800 rounded text-xs text-zinc-400">Alt+Space</kbd> or tap below</p>
        <p className="text-xs mt-1 text-zinc-700">Speak. Actions are invoked.</p>
      </div>
    );
  }

  return (
    <div className="text-center px-8 max-w-sm">
      {stage === "recording" && (
        <div className="flex items-center justify-center gap-2 mb-4">
          <div className="w-3 h-3 rounded-full bg-red-500 animate-pulse" />
          <span className="text-red-400 text-sm font-medium">Listening</span>
        </div>
      )}
      {stage === "processing" && (
        <div className="flex items-center justify-center gap-2 mb-4">
          <div className="w-3 h-3 rounded-full bg-violet-500 animate-pulse" />
          <span className="text-violet-400 text-sm font-medium">Transcribing...</span>
        </div>
      )}
      {stage === "agent_running" && (
        <div className="flex items-center justify-center gap-2 mb-4">
          <div className="w-3 h-3 rounded-full bg-amber-500 animate-pulse" />
          <span className="text-amber-400 text-sm font-medium">{statusMessage}</span>
        </div>
      )}
      {currentTranscription && (
        <p className="text-zinc-300 text-lg leading-relaxed mb-3">
          "{currentTranscription}"
        </p>
      )}
      {/* Show last action result */}
      {stage === "idle" && lastAction?.intent && lastAction?.result && (
        <div className="mt-2 p-3 rounded-lg bg-zinc-900 border border-zinc-800 text-left">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs">{TOOL_LABELS[lastAction.intent.tool] || "🔧"}</span>
            <span className="text-xs text-zinc-500">{lastAction.intent.tool}</span>
            {lastAction.intent.confidence > 0 && (
              <span className="text-[10px] text-zinc-600">
                {Math.round(lastAction.intent.confidence * 100)}%
              </span>
            )}
          </div>
          <p className="text-xs text-zinc-400">{lastAction.result}</p>
        </div>
      )}
    </div>
  );
}
