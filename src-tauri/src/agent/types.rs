use serde::{Deserialize, Serialize};

/// Re-export ClassifiedIntent from llm_client (single source of truth)
pub use crate::agent::llm_client::ClassifiedIntent;

/// Agent event emitted during processing
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum AgentEvent {
    Transcription { text: String },
    Classification { intent: ClassifiedIntent },
    ToolExecuting { tool: String },
    ToolResult { tool: String, success: bool, result: serde_json::Value },
    Error { message: String },
    Done { message: String },
}

/// Supported tools for intent classification
pub const SUPPORTED_TOOLS: &[&str] = &[
    "DICTATE",
    "GMAIL_SEND_EMAIL",
    "GITHUB_CREATE_AN_ISSUE",
    "SLACK_SEND_MESSAGE",
    "GOOGLECALENDAR_EVENTS_LIST",
    "NOTION_CREATE_PAGE",
    "COMPOSIO_SEARCH_WEB",
];

/// System prompt for Qwen 3 0.6B intent classification
pub const CLASSIFICATION_PROMPT: &str = r#"You are an intent classifier. Given transcribed speech, output ONLY a JSON object with:
- "tool": one of [DICTATE, GMAIL_SEND_EMAIL, GITHUB_CREATE_AN_ISSUE, SLACK_SEND_MESSAGE, GOOGLECALENDAR_EVENTS_LIST, NOTION_CREATE_PAGE, COMPOSIO_SEARCH_WEB]
- "parameters": relevant parameters as JSON
- "confidence": 0.0 to 1.0

Examples:
"Email Sarah the meeting notes" -> {"tool":"GMAIL_SEND_EMAIL","parameters":{"to":"Sarah","body":"meeting notes"},"confidence":0.9}
"Create a bug: login broken" -> {"tool":"GITHUB_CREATE_ISSUE","parameters":{"title":"login broken","labels":["bug"]},"confidence":0.85}
"Hello world" -> {"tool":"DICTATE","parameters":{"text":"Hello world"},"confidence":0.95}
"Slack the team: deploy done" -> {"tool":"SLACK_SEND_MESSAGE","parameters":{"channel":"team","message":"deploy done"},"confidence":0.88}
"What's on my calendar today" -> {"tool":"GOOGLECALENDAR_EVENTS_LIST","parameters":{"timeMin":"today"},"confidence":0.82}
"Search for Rust Tauri tutorials" -> {"tool":"COMPOSIO_SEARCH_WEB","parameters":{"query":"Rust Tauri tutorials"},"confidence":0.9}
"Search the web for OpenAI official website" -> {"tool":"COMPOSIO_SEARCH_WEB","parameters":{"query":"OpenAI official website"},"confidence":1.0}

Output ONLY the JSON object, nothing else."#;
