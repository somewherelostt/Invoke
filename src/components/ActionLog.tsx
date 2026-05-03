import { useAppStore } from "../stores/app-store";
import { TOOL_LABELS } from "../lib/types";

export function ActionLog() {
  const { actionLog } = useAppStore();

  if (actionLog.length === 0) {
    return (
      <div className="px-4 py-3 text-xs text-zinc-700 text-center">
        No actions yet. Start speaking.
      </div>
    );
  }

  return (
    <div className="divide-y divide-zinc-800/50">
      {actionLog.slice(0, 10).map((entry) => (
        <div key={entry.id} className="px-4 py-2 flex items-start gap-3">
          <span className="text-xs mt-0.5">
            {entry.intent ? TOOL_LABELS[entry.intent.tool] || "🔧" : "📝"}
          </span>
          <div className="flex-1 min-w-0">
            <p className="text-xs text-zinc-400 truncate">
              {entry.transcription}
            </p>
            {entry.error && (
              <p className="text-xs text-red-500 mt-0.5">{entry.error}</p>
            )}
          </div>
          <span className="text-[10px] text-zinc-700">
            {new Date(entry.timestamp).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </span>
        </div>
      ))}
    </div>
  );
}
