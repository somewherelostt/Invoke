import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useAppStore } from "../stores/app-store";

const STEPS = [
  {
    icon: "🎤",
    title: "Allow Microphone",
    desc: "INVOKE needs microphone access to hear your voice commands.",
  },
  {
    icon: "🧠",
    title: "Local AI",
    desc: "Whisper (61M) transcribes your voice. Qwen 3 0.6B classifies your intent. All running locally — zero cloud dependency.",
  },
  {
    icon: "⚡",
    title: "1000+ Actions",
    desc: "Connect Gmail, GitHub, Slack, Calendar, Notion via Composio. Say \"Email John about the report\" → done.",
  },
  {
    icon: "⌨️",
    title: "Global Hotkey",
    desc: "Press Alt+Space anywhere to start recording. No need to switch windows.",
  },
];

export function Onboarding() {
  const [step, setStep] = useState(0);
  const { toggleSettings } = useAppStore();

  const handleNext = () => {
    if (step < STEPS.length - 1) {
      setStep(step + 1);
    }
  };

  const handleFinish = async () => {
    // Check if services are running
    try {
      await invoke("get_settings");
    } catch {}
    // Mark onboarding complete (in real app, save to store)
    useAppStore.setState({ settingsOpen: false });
  };

  const s = STEPS[step];

  return (
    <div className="flex flex-col items-center justify-center h-full px-8 max-w-sm mx-auto">
      {/* Progress dots */}
      <div className="flex gap-2 mb-8">
        {STEPS.map((_, i) => (
          <div
            key={i}
            className={`w-2 h-2 rounded-full transition-colors ${
              i === step ? "bg-violet-500" : i < step ? "bg-violet-800" : "bg-zinc-700"
            }`}
          />
        ))}
      </div>

      {/* Step content */}
      <div className="text-center">
        <div className="text-5xl mb-4">{s.icon}</div>
        <h2 className="text-lg font-semibold text-zinc-200 mb-2">{s.title}</h2>
        <p className="text-sm text-zinc-400 leading-relaxed">{s.desc}</p>
      </div>

      {/* Actions */}
      <div className="mt-8 w-full space-y-3">
        {step === STEPS.length - 1 ? (
          <>
            <button
              onClick={handleFinish}
              className="w-full py-3 bg-violet-600 hover:bg-violet-700 rounded-xl text-sm font-medium transition-colors"
            >
              🚀 Get Started
            </button>
            <button
              onClick={() => {
                handleFinish();
                toggleSettings();
              }}
              className="w-full py-2 bg-zinc-800 hover:bg-zinc-700 rounded-xl text-xs text-zinc-400 transition-colors"
            >
              Connect Composio first (optional)
            </button>
          </>
        ) : (
          <button
            onClick={handleNext}
            className="w-full py-3 bg-violet-600 hover:bg-violet-700 rounded-xl text-sm font-medium transition-colors"
          >
            Next →
          </button>
        )}
      </div>

      {/* Skip */}
      {step < STEPS.length - 1 && (
        <button
          onClick={handleFinish}
          className="mt-4 text-xs text-zinc-600 hover:text-zinc-400 transition-colors"
        >
          Skip setup
        </button>
      )}
    </div>
  );
}
