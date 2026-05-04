import { useState, useEffect } from "react";
import type { ReactNode } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useAppStore } from "../stores/app-store";

export function Settings() {
  const { settings, updateSettings } = useAppStore();
  const [local, setLocal] = useState(settings);

  useEffect(() => {
    setLocal(settings);
  }, [settings]);

  const handleSave = async () => {
    updateSettings(local);
    try {
      await invoke("update_settings", { newSettings: local });
    } catch (err) {
      console.error("Failed to save settings:", err);
    }
  };

  const Field = ({
    label,
    children,
  }: {
    label: string;
    children: ReactNode;
  }) => (
    <div className="space-y-2">
      <label className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#7b627e]">
        {label}
      </label>
      {children}
    </div>
  );

  return (
    <div className="max-h-[calc(100vh-92px)] space-y-4 overflow-y-auto rounded-[28px] border border-white/65 bg-white/34 p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.8)] backdrop-blur-xl">
      <div>
        <h2 className="font-serif text-2xl font-normal text-[#351e38]">Settings</h2>
        <p className="mt-1 text-sm text-[#765d79]">
          Keep the voice pipeline close to where you work.
        </p>
      </div>

      <Field label="Hotkey">
        <input
          className="settings-input"
          value={local.hotkey}
          onChange={(e) => setLocal({ ...local, hotkey: e.target.value })}
        />
      </Field>

      <Field label="Whisper Model">
        <select
          className="settings-input"
          value={local.whisper_model}
          onChange={(e) => setLocal({ ...local, whisper_model: e.target.value })}
        >
          <option value="tiny">Tiny (61M)</option>
          <option value="base">Base (74M)</option>
          <option value="small">Small (244M)</option>
        </select>
      </Field>

      <Field label="LLM Endpoint">
        <input
          className="settings-input"
          value={local.llm_endpoint}
          onChange={(e) => setLocal({ ...local, llm_endpoint: e.target.value })}
        />
      </Field>

      <Field label="LLM Model">
        <input
          className="settings-input"
          value={local.llm_model}
          onChange={(e) => setLocal({ ...local, llm_model: e.target.value })}
        />
      </Field>

      <Field label="Composio API Key">
        <input
          type="password"
          className="settings-input"
          value={local.composio_api_key}
          onChange={(e) =>
            setLocal({ ...local, composio_api_key: e.target.value })
          }
          placeholder="comp_..."
        />
      </Field>

      <div className="flex items-center justify-between rounded-2xl border border-white/55 bg-white/30 px-3 py-3">
        <span className="text-sm text-[#5d4661]">Auto-paste result</span>
        <input
          type="checkbox"
          checked={local.auto_paste}
          onChange={(e) =>
            setLocal({ ...local, auto_paste: e.target.checked })
          }
          className="accent-[#3f2542]"
        />
      </div>

      <div className="flex items-center justify-between rounded-2xl border border-white/55 bg-white/30 px-3 py-3">
        <span className="text-sm text-[#5d4661]">Confirm before actions</span>
        <input
          type="checkbox"
          checked={local.confirm_actions}
          onChange={(e) =>
            setLocal({ ...local, confirm_actions: e.target.checked })
          }
          className="accent-[#3f2542]"
        />
      </div>

      <button
        onClick={handleSave}
        className="primary-soft-button w-full"
      >
        Save Settings
      </button>
    </div>
  );
}
