use serde::{Deserialize, Serialize};
use std::{env, fs, path::PathBuf};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppSettings {
    pub hotkey: String,
    pub whisper_model: String,
    pub llm_endpoint: String,
    pub llm_model: String,
    pub composio_api_key: String,
    pub auto_paste: bool,
    pub confirm_actions: bool,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            hotkey: setting("INVOKE_HOTKEY", "Alt+Space"),
            whisper_model: setting("INVOKE_WHISPER_MODEL", "tiny"),
            llm_endpoint: setting("INVOKE_LLM_ENDPOINT", "http://localhost:11434"),
            llm_model: setting("INVOKE_LLM_MODEL", "qwen3:0.6b"),
            composio_api_key: setting("INVOKE_COMPOSIO_API_KEY", ""),
            auto_paste: true,
            confirm_actions: true,
        }
    }
}

fn setting(key: &str, fallback: &str) -> String {
    env::var(key)
        .ok()
        .filter(|value| !value.trim().is_empty())
        .or_else(|| dotenv_value(key))
        .unwrap_or_else(|| fallback.to_string())
}

fn dotenv_value(key: &str) -> Option<String> {
    dotenv_paths()
        .into_iter()
        .filter_map(|path| fs::read_to_string(path).ok())
        .flat_map(|contents| {
            contents
                .lines()
                .map(str::trim)
                .filter(|line| !line.is_empty() && !line.starts_with('#'))
                .filter_map(|line| line.split_once('='))
                .map(|(name, value)| (name.trim().to_string(), clean_env_value(value)))
                .collect::<Vec<_>>()
        })
        .find_map(|(name, value)| (name == key && !value.trim().is_empty()).then_some(value))
}

fn dotenv_paths() -> Vec<PathBuf> {
    let mut paths = Vec::new();
    if let Ok(current_dir) = env::current_dir() {
        paths.push(current_dir.join(".env.local"));
        paths.push(current_dir.join(".env"));
    }
    paths
}

fn clean_env_value(value: &str) -> String {
    value
        .trim()
        .trim_matches('"')
        .trim_matches('\'')
        .to_string()
}
