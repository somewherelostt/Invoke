import { FileText } from "lucide-react";
import { TOOL_LABELS } from "../lib/types";
import { useAppStore } from "../stores/app-store";

export function ActionLog() {
  const { actionLog } = useAppStore();

  if (actionLog.length === 0) {
    return (
      <div className="px-4 py-4 text-center text-xs text-[#8f7a93]">
        No actions yet. Start speaking.
      </div>
    );
  }

  return (
    <div className="divide-y divide-[#e6d3ea]/70">
      {actionLog.slice(0, 10).map((entry) => (
        <div key={entry.id} className="flex items-start gap-3 px-4 py-3">
          <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/55 text-[#4b294f]">
            <FileText size={14} strokeWidth={1.8} />
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs text-[#66516a]">
              {entry.transcription || TOOL_LABELS[entry.intent?.tool || ""] || "Action"}
            </p>
            {entry.error && (
              <p className="mt-0.5 text-xs text-[#9b3048]">{entry.error}</p>
            )}
          </div>
          <span className="text-[10px] text-[#8f7a93]">
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
