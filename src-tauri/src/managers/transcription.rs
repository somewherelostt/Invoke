use anyhow::Result;
use log::info;
use reqwest::Client;
use serde::Deserialize;

const DEFAULT_WHISPER_URL: &str = "http://127.0.0.1:8394";

#[derive(Debug, Deserialize)]
struct TranscriptionResponse {
    text: String,
    language: String,
    duration: f64,
}

/// Transcription manager — calls local Whisper server.
pub struct TranscriptionManager {
    server_url: String,
    client: Client,
}

impl TranscriptionManager {
    pub fn new(server_url: &str) -> Self {
        Self {
            server_url: if server_url.is_empty() {
                DEFAULT_WHISPER_URL.to_string()
            } else {
                server_url.to_string()
            },
            client: Client::new(),
        }
    }

    /// Check if the Whisper server is healthy
    pub async fn health_check(&self) -> bool {
        let url = format!("{}/health", self.server_url);
        match self.client.get(&url).timeout(std::time::Duration::from_secs(2)).send().await {
            Ok(resp) => resp.status().is_success(),
            Err(_) => false,
        }
    }

    /// Send WAV bytes to Whisper server for transcription
    pub async fn transcribe(&self, wav_bytes: &[u8]) -> Result<String> {
        let url = format!("{}/transcribe", self.server_url);

        let part = reqwest::multipart::Part::bytes(wav_bytes.to_vec())
            .file_name("recording.wav")
            .mime_str("audio/wav")?;

        let form = reqwest::multipart::Form::new()
            .part("file", part)
            .text("language", "en");

        let response = self.client
            .post(&url)
            .multipart(form)
            .timeout(std::time::Duration::from_secs(30))
            .send()
            .await?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            anyhow::bail!("Whisper server error {}: {}", status, body);
        }

        let result: TranscriptionResponse = response.json().await?;
        info!("🔊 Transcribed ({:.1}s, {}): {}", result.duration, result.language, result.text);

        Ok(result.text)
    }

    pub fn update_url(&mut self, url: &str) {
        self.server_url = if url.is_empty() {
            DEFAULT_WHISPER_URL.to_string()
        } else {
            url.to_string()
        };
    }
}
