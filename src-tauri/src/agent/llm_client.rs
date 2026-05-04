use anyhow::Result;
use log::info;
use serde::{Deserialize, Serialize};

use super::types::CLASSIFICATION_PROMPT;

/// Classified intent from the LLM — this is the unified type used everywhere
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClassifiedIntent {
    pub tool: String,
    pub parameters: serde_json::Value,
    pub confidence: f32,
}

/// OpenAI-compatible LLM client for local Qwen 3 0.6B via Ollama/llama.cpp
pub struct LlmClient {
    endpoint: String,
    model: String,
    client: reqwest::Client,
}

#[derive(Serialize)]
struct ChatRequest {
    model: String,
    messages: Vec<ChatMessage>,
    stream: bool,
    temperature: f32,
}

#[derive(Serialize, Deserialize, Clone)]
struct ChatMessage {
    role: String,
    content: String,
}

#[derive(Deserialize)]
struct ChatResponse {
    message: ChatMessage,
}

impl LlmClient {
    pub fn new(endpoint: &str, model: &str) -> Self {
        Self {
            endpoint: endpoint.to_string(),
            model: model.to_string(),
            client: reqwest::Client::builder()
                .no_proxy()
                .build()
                .expect("failed to build LLM HTTP client"),
        }
    }

    /// Classify user intent from transcribed text
    pub async fn classify_intent(&self, transcription: &str) -> Result<ClassifiedIntent> {
        info!("🧠 Classifying intent: {}", transcription);

        if let Some(intent) = classify_rule_based(transcription) {
            info!("🧠 Rule classified: tool={}, confidence={}", intent.tool, intent.confidence);
            return Ok(intent);
        }

        let url = format!("{}/api/chat", self.endpoint);

        let request = ChatRequest {
            model: self.model.clone(),
            messages: vec![
                ChatMessage {
                    role: "system".to_string(),
                    content: CLASSIFICATION_PROMPT.to_string(),
                },
                ChatMessage {
                    role: "user".to_string(),
                    content: transcription.to_string(),
                },
            ],
            stream: false,
            temperature: 0.1,
        };

        let response = self.client
            .post(&url)
            .json(&request)
            .send()
            .await?;

        if !response.status().is_success() {
            anyhow::bail!("LLM request failed: {}", response.status());
        }

        let chat_response: ChatResponse = response.json().await?;
        let content = chat_response.message.content.trim().to_string();

        // Parse JSON from response (handle markdown code blocks + /think blocks from Qwen 3)
        let json_str = extract_json(&content);

        let intent: ClassifiedIntent = serde_json::from_str(json_str)
            .map_err(|e| anyhow::anyhow!("Failed to parse LLM output as intent: {} - raw: {}", e, json_str))?;

        info!("🧠 Classified: tool={}, confidence={}", intent.tool, intent.confidence);
        Ok(intent)
    }

    /// Create a fallback DICTATE intent when classification fails
    pub fn fallback_dictate(text: &str) -> ClassifiedIntent {
        ClassifiedIntent {
            tool: "DICTATE".to_string(),
            parameters: serde_json::json!({"text": text}),
            confidence: 0.0,
        }
    }

    /// Polish text for dictation mode
    pub async fn polish_text(&self, raw_text: &str) -> Result<String> {
        info!("✨ Polishing text: {}", raw_text);

        let url = format!("{}/api/chat", self.endpoint);

        let request = ChatRequest {
            model: self.model.clone(),
            messages: vec![
                ChatMessage {
                    role: "system".to_string(),
                    content: "Remove filler words (um, uh, like), fix grammar, add proper punctuation. Output ONLY the cleaned text.".to_string(),
                },
                ChatMessage {
                    role: "user".to_string(),
                    content: raw_text.to_string(),
                },
            ],
            stream: false,
            temperature: 0.3,
        };

        let response = self.client
            .post(&url)
            .json(&request)
            .send()
            .await?;

        let chat_response: ChatResponse = response.json().await?;
        Ok(chat_response.message.content.trim().to_string())
    }
}

fn classify_rule_based(text: &str) -> Option<ClassifiedIntent> {
    let normalized = text.to_lowercase();
    let wants_search = [
        "search the web",
        "web search",
        "search online",
        "look up",
        "google",
        "find online",
        "latest ",
    ]
    .iter()
    .any(|needle| normalized.contains(needle));

    if !wants_search {
        return None;
    }

    let query = text
        .trim()
        .trim_start_matches(|c: char| c.is_whitespace())
        .to_string();

    let query = regex_strip_prefix(&query, &[
        "search the web for ",
        "web search ",
        "search online for ",
        "look up ",
        "google ",
        "find online ",
    ]);

    Some(ClassifiedIntent {
        tool: "COMPOSIO_SEARCH_WEB".to_string(),
        parameters: serde_json::json!({ "query": if query.is_empty() { text } else { &query } }),
        confidence: 1.0,
    })
}

fn regex_strip_prefix(text: &str, prefixes: &[&str]) -> String {
    let lower = text.to_lowercase();
    for prefix in prefixes {
        if lower.starts_with(prefix) {
            return text[prefix.len()..].trim().to_string();
        }
    }
    text.trim().to_string()
}

/// Extract JSON from LLM output, handling:
/// - ```json ... ``` blocks
/// - Qwen 3 <think/>...</think/> blocks
/// - Raw JSON objects
fn extract_json(content: &str) -> &str {
    // First, strip Qwen 3 thinking blocks
    let without_think = if let Some(end) = content.find("</think]") {
        &content[end + 8..]
    } else if let Some(end) = content.find("<think/>") {
        &content[end + 8..]
    } else {
        content
    }.trim();

    // Then strip markdown code blocks
    let without_md = without_think
        .trim_start_matches("```json")
        .trim_start_matches("```")
        .trim_end_matches("```")
        .trim();

    // Try to find a JSON object
    if let Some(start) = without_md.find('{') {
        if let Some(end) = without_md.rfind('}') {
            return &without_md[start..=end];
        }
    }

    without_md
}
