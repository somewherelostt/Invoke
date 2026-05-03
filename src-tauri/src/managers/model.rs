use anyhow::Result;
use log::info;

/// Model download and management.
/// Handles downloading Whisper models and LLM model files.
pub struct ModelManager {
    models_dir: String,
}

impl ModelManager {
    pub fn new(models_dir: &str) -> Self {
        Self {
            models_dir: models_dir.to_string(),
        }
    }

    pub fn ensure_model(&self, name: &str) -> Result<String> {
        info!("📦 Ensuring model exists: {}", name);
        // TODO: Check if model exists, download if not
        let path = format!("{}/{}.gguf", self.models_dir, name);
        Ok(path)
    }

    pub fn list_models(&self) -> Vec<String> {
        // TODO: Scan models directory
        vec!["tiny".to_string(), "base".to_string(), "small".to_string()]
    }
}
