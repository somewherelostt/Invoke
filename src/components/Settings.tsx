import { useState, useEffect } from "react";
import { invoke } from "@tauri-apps/api/core";
import { useAppStore } from "../stores/app-store";
import type { AppSettings } from "../lib/types";

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
    children: React.ReactNode;
  }) => (
    <div className="space-y-1">
      <label className="text-xs font-medium text-zinc-500 uppercase tracking-wider">
        {label}
      </label>
      {children}
    </div>
  );

  return (
    <div className="p-4 space-y-4 overflow-y-auto max-h-[calc(100vh-52px)]">
      <h2 className="text-sm font-semibold text-zinc-300">⚙️ Settings</h2>

      <Field label="Hotkey">
        <input
          className="w-full bg-zinc-900 border border-zinc-800 rounded-md px-3 py-2 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none"
          value={local.hotkey}
          onChange={(e) => setLocal({ ...local, hotkey: e.target.value })}
        />
      </Field>

      <Field label="Whisper Model">
        <select
          className="w-full bg-zinc-900 border border-zinc-800 rounded-md px-3 py-2 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none"
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
          className="w-full bg-zinc-900 border border-zinc-800 rounded-md px-3 py-2 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none"
          value={local.llm_endpoint}
          onChange={(e) => setLocal({ ...local, llm_endpoint: e.target.value })}
        />
      </Field>

      <Field label="LLM Model">
        <input
          className="w-full bg-zinc-900 border border-zinc-800 rounded-md px-3 py-2 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none"
          value={local.llm_model}
          onChange={(e) => setLocal({ ...local, llm_model: e.target.value })}
        />
      </Field>

      <Field label="Composio API Key">
        <input
          type="password"
          className="w-full bg-zinc-900 border border-zinc-800 rounded-md px-3 py-2 text-sm text-zinc-300 focus:border-violet-500 focus:outline-none"
          value={local.composio_api_key}
          onChange={(e) =>
            setLocal({ ...local, composio_api_key: e.target.value })
          }
          placeholder="comp_..."
        />
      </Field>

      <div className="flex items-center justify-between">
        <span className="text-xs text-zinc-500">Auto-paste result</span>
        <input
          type="checkbox"
          checked={local.auto_paste}
          onChange={(e) =>
            setLocal({ ...local, auto_paste: e.target.checked })
          }
          className="accent-violet-600"
        />
      </div>

      <div className="flex items-center justify-between">
        <span className="text-xs text-zinc-500">Confirm before actions</span>
        <input
          type="checkbox"
          checked={local.confirm_actions}
          onChange={(e) =>
            setLocal({ ...local, confirm_actions: e.target.checked })
          }
          className="accent-violet-600"
        />
      </div>

      <button
        onClick={handleSave}
        className="w-full py-2 bg-violet-600 hover:bg-violet-700 rounded-md text-sm font-medium transition-colors"
      >
        Save Settings
      </button>
    </div>
  );
}
