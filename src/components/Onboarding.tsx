import { invoke } from "@tauri-apps/api/core";
import { Brain, Keyboard, Mic2, Plug, Sparkle } from "lucide-react";
import { useState } from "react";
import { useAppStore } from "../stores/app-store";

const STEPS = [
  {
    icon: Mic2,
    title: "Allow microphone",
    desc: "Invoke needs microphone access to hear your voice commands.",
  },
  {
    icon: Brain,
    title: "Local AI",
    desc: "Whisper transcribes your voice. Qwen classifies your intent. Your default path stays local.",
  },
  {
    icon: Plug,
    title: "Connect actions",
    desc: "Add Composio for Gmail, GitHub, Slack, Calendar, Notion, and more.",
  },
  {
    icon: Keyboard,
    title: "Global hotkey",
    desc: "Press Alt+Space anywhere to record without switching windows.",
  },
];

export function Onboarding() {
  const [step, setStep] = useState(0);
  const { toggleSettings, completeOnboarding } = useAppStore();
  const s = STEPS[step];
  const Icon = s.icon;

  const handleNext = () => {
    if (step < STEPS.length - 1) {
      setStep(step + 1);
    }
  };

  const handleFinish = async () => {
    try {
      await invoke("get_settings");
    } catch {}
    useAppStore.setState({ settingsOpen: false });
    completeOnboarding();
  };

  return (
    <div className="mx-auto flex h-full max-w-[420px] flex-col justify-center px-8">
      <div className="mb-8 flex items-center gap-2">
        <div className="flex h-10 w-10 items-center justify-center rounded-full border border-white/70 bg-white/45">
          <Sparkle size={17} strokeWidth={1.7} />
        </div>
        <span className="font-serif text-2xl text-[#351e38]">Invoke</span>
      </div>

      <div className="mb-8 flex gap-2">
        {STEPS.map((_, i) => (
          <div
            key={i}
            className={`h-1.5 flex-1 rounded-full transition-colors ${
              i <= step ? "bg-[#4b294f]" : "bg-white/55"
            }`}
          />
        ))}
      </div>

      <div>
        <div className="mb-5 flex h-16 w-16 items-center justify-center rounded-[24px] border border-white/70 bg-white/45 text-[#4b294f]">
          <Icon size={28} strokeWidth={1.6} />
        </div>
        <h2 className="font-serif text-[38px] leading-none text-[#351e38]">{s.title}</h2>
        <p className="mt-4 text-[15px] leading-6 text-[#66516a]">{s.desc}</p>
      </div>

      <div className="mt-9 space-y-3">
        {step === STEPS.length - 1 ? (
          <>
            <button onClick={handleFinish} className="primary-soft-button">
              Get started
            </button>
            <button
              onClick={() => {
                handleFinish();
                toggleSettings();
              }}
              className="secondary-soft-button"
            >
              Connect Composio first
            </button>
          </>
        ) : (
          <button onClick={handleNext} className="primary-soft-button">
            Next
          </button>
        )}
      </div>

      {step < STEPS.length - 1 && (
        <button
          onClick={handleFinish}
          className="mt-4 text-xs text-[#8f7a93] transition hover:text-[#351e38]"
        >
          Skip setup
        </button>
      )}
    </div>
  );
}
