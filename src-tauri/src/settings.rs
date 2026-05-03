use serde::{Deserialize, Serialize};

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
            hotkey: "Alt+Space".to_string(),
            whisper_model: "tiny".to_string(),
            llm_endpoint: "http://localhost:11434".to_string(),
            llm_model: "qwen3:0.6b".to_string(),
            composio_api_key: String::new(),
            auto_paste: true,
            confirm_actions: true,
        }
    }
}
