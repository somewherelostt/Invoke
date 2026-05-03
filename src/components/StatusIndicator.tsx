import { useAppStore } from "../stores/app-store";

const STAGE_COLORS: Record<string, string> = {
  idle: "bg-zinc-600",
  recording: "bg-red-500 animate-pulse",
  processing: "bg-violet-500 animate-pulse",
  agent_running: "bg-green-500 animate-pulse",
};

export function StatusIndicator() {
  const { stage } = useAppStore();
  const dotClass = "w-2 h-2 rounded-full " + (STAGE_COLORS[stage] || "bg-zinc-600");

  return (
    <div className="flex items-center gap-1.5">
      <div className={dotClass} />
      <span className="text-xs text-zinc-500 capitalize">{stage.replace("_", " ")}</span>
    </div>
  );
}
