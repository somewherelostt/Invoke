import { useAppStore } from "../stores/app-store";

const STAGE_COLORS: Record<string, string> = {
  idle: "bg-[#8f7a93]",
  recording: "bg-[#b85c72] animate-pulse",
  processing: "bg-[#4b294f] animate-pulse",
  agent_running: "bg-[#40745b] animate-pulse",
};

export function StatusIndicator() {
  const { stage } = useAppStore();
  const dotClass = "h-2 w-2 rounded-full " + (STAGE_COLORS[stage] || "bg-[#8f7a93]");

  return (
    <div className="flex items-center gap-1.5 rounded-full border border-white/55 bg-white/40 px-3 py-2">
      <div className={dotClass} />
      <span className="text-xs capitalize text-[#66516a]">{stage.replace("_", " ")}</span>
    </div>
  );
}
