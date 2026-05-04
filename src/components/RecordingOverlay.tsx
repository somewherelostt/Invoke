import { Keyboard, Mic2, Sparkle, Wand2 } from "lucide-react";
import { TOOL_LABELS } from "../lib/types";
import { useAppStore } from "../stores/app-store";

export function RecordingOverlay() {
  const { stage, currentTranscription, statusMessage, actionLog } = useAppStore();
  const lastAction = actionLog[0];

  if (stage === "idle" && !currentTranscription && !lastAction?.result) {
    return (
      <div className="w-full max-w-[390px] px-1">
        <div className="mb-8">
          <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-white/60 bg-white/40 px-3 py-1.5 text-xs text-[#66516a]">
            <Sparkle size={13} strokeWidth={1.7} />
            Local voice actions
          </div>
          <h2 className="font-serif text-[44px] leading-[0.95] text-[#351e38]">
            Hi there,
          </h2>
          <p className="mt-3 max-w-[30ch] text-[15px] leading-6 text-[#66516a]">
            Speak once. Invoke writes, searches, formats, and runs actions across your desktop.
          </p>
        </div>

        <div className="grid grid-cols-2 gap-2">
          <div className="desktop-tile">
            <Wand2 size={20} strokeWidth={1.7} />
            <span>Clean up text</span>
          </div>
          <div className="desktop-tile">
            <Mic2 size={20} strokeWidth={1.7} />
            <span>Dictate anywhere</span>
          </div>
        </div>

        <div className="mt-3 flex items-center gap-2 rounded-full border border-white/65 bg-white/45 px-4 py-3 text-sm text-[#66516a]">
          <Keyboard size={16} strokeWidth={1.8} />
          Press <kbd className="rounded-md bg-[#351e38]/10 px-1.5 py-0.5 text-xs text-[#351e38]">Alt+Space</kbd>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-sm px-2 text-center">
      <div className={`music-bubble mx-auto mb-5 ${stage === "recording" ? "is-recording" : ""}`}>
        <div className="music-bars" aria-hidden="true">
          {Array.from({ length: 9 }).map((_, index) => (
            <span key={index} style={{ ["--i" as string]: index }} />
          ))}
        </div>
      </div>

      <div className="mb-4 text-sm font-medium text-[#66516a]">
        {stage === "recording" && "Listening..."}
        {stage === "processing" && "Transcribing..."}
        {stage === "agent_running" && statusMessage}
      </div>

      {currentTranscription && (
        <p className="mb-3 text-lg leading-relaxed text-[#351e38]">
          "{currentTranscription}"
        </p>
      )}

      {stage === "idle" && lastAction?.intent && lastAction?.result && (
        <div className="mt-2 rounded-2xl border border-white/60 bg-white/45 p-4 text-left">
          <div className="mb-1 flex items-center gap-2">
            <span className="text-xs text-[#66516a]">{TOOL_LABELS[lastAction.intent.tool] || "Action"}</span>
            {lastAction.intent.confidence > 0 && (
              <span className="text-[10px] text-[#8f7a93]">
                {Math.round(lastAction.intent.confidence * 100)}%
              </span>
            )}
          </div>
          <p className="text-xs text-[#66516a]">{lastAction.result}</p>
        </div>
      )}
    </div>
  );
}
