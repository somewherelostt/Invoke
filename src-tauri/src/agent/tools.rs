use anyhow::Result;
use log::{info, error};
use serde_json::Value;

use super::llm_client::ClassifiedIntent;

/// Tool executor — routes classified intents to Composio or local actions
pub struct ToolExecutor {
    composio_api_key: String,
    client: reqwest::Client,
    base_url: String,
}

impl ToolExecutor {
    pub fn new(composio_api_key: &str) -> Self {
        Self {
            composio_api_key: composio_api_key.to_string(),
            client: reqwest::Client::builder()
                .no_proxy()
                .build()
                .expect("failed to build Composio HTTP client"),
            base_url: "https://backend.composio.dev/api/v3.1".to_string(),
        }
    }

    pub fn update_api_key(&mut self, key: &str) {
        self.composio_api_key = key.to_string();
    }

    pub fn has_api_key(&self) -> bool {
        !self.composio_api_key.is_empty()
    }

    /// Execute a classified intent
    pub async fn execute(&self, intent: &ClassifiedIntent) -> Result<ToolResult> {
        info!("⚡ Executing tool: {} (confidence: {:.2})", intent.tool, intent.confidence);

        // Low confidence → don't execute, just dictate
        if intent.confidence < 0.3 {
            info!("⚠️ Low confidence ({:.2}), falling back to DICTATE", intent.confidence);
            return self.execute_dictate(intent);
        }

        match intent.tool.as_str() {
            "DICTATE" => self.execute_dictate(intent),
            _ => {
                if self.has_api_key() {
                    self.execute_composio(intent).await
                } else {
                    info!("⚠️ No Composio API key — simulating execution");
                    Ok(ToolResult {
                        success: true,
                        simulated: true,
                        tool: intent.tool.clone(),
                        message: format!("Would execute: {} (no API key)", intent.tool),
                        data: intent.parameters.clone(),
                    })
                }
            }
        }
    }

    /// Dictate mode — return text for clipboard
    fn execute_dictate(&self, intent: &ClassifiedIntent) -> Result<ToolResult> {
        let text = intent.parameters
            .get("text")
            .and_then(|v| v.as_str())
            .unwrap_or("")
            .to_string();

        Ok(ToolResult {
            success: true,
            simulated: false,
            tool: "DICTATE".to_string(),
            message: text.clone(),
            data: serde_json::json!({"text": text}),
        })
    }

    /// Execute via Composio API
    async fn execute_composio(&self, intent: &ClassifiedIntent) -> Result<ToolResult> {
        let action_name = self.map_tool_to_composio(&intent.tool);
        info!("🔧 Executing via Composio: {} → {}", intent.tool, action_name);

        let url = format!("{}/tools/execute/{}", self.base_url, action_name);
        let params = self.normalize_params(intent);

        let body = serde_json::json!({
            "arguments": params
        });

        let response = self.client
            .post(&url)
            .header("x-api-key", &self.composio_api_key)
            .header("Content-Type", "application/json")
            .json(&body)
            .send()
            .await;

        match response {
            Ok(resp) => {
                let status = resp.status();
                let resp_body: Value = resp.json().await.unwrap_or(serde_json::json!({}));

                if status.is_success() {
                    info!("✅ Composio success: {}", intent.tool);
                    Ok(ToolResult {
                        success: true,
                        simulated: false,
                        tool: intent.tool.clone(),
                        message: extract_composio_message(&resp_body)
                            .unwrap_or_else(|| format!("✅ {} executed successfully", intent.tool)),
                        data: resp_body,
                    })
                } else {
                    error!("❌ Composio error {}: {:?}", status, resp_body);
                    Ok(ToolResult {
                        success: false,
                        simulated: false,
                        tool: intent.tool.clone(),
                        message: format!("❌ {} failed: {}", intent.tool, status),
                        data: resp_body,
                    })
                }
            }
            Err(e) => {
                error!("❌ Composio request failed: {}", e);
                anyhow::bail!("Composio request failed: {}", e);
            }
        }
    }

    /// Map our tool names to Composio action names
    fn map_tool_to_composio(&self, tool: &str) -> String {
        match tool {
            "GMAIL_SEND_EMAIL" => "GMAIL_SEND_EMAIL",
            "GITHUB_CREATE_ISSUE" => "GITHUB_CREATE_AN_ISSUE",
            "GITHUB_CREATE_AN_ISSUE" => "GITHUB_CREATE_AN_ISSUE",
            "SLACK_SEND_MESSAGE" => "SLACK_SEND_MESSAGE",
            "GOOGLECALENDAR_EVENTS_LIST" => "GOOGLECALENDAR_FIND_EVENTS",
            "NOTION_CREATE_PAGE" => "NOTION_CREATE_A_PAGE",
            "WEB_SEARCH" => "COMPOSIO_SEARCH_WEB",
            "COMPOSIO_SEARCH_WEB" => "COMPOSIO_SEARCH_WEB",
            _ => tool,
        }.to_string()
    }

    /// Normalize LLM-extracted parameters to Composio's expected format
    fn normalize_params(&self, intent: &ClassifiedIntent) -> Value {
        let mut params = intent.parameters.clone();

        if let Some(map) = params.as_object_mut() {
            match intent.tool.as_str() {
                "GMAIL_SEND_EMAIL" => {
                    if !map.contains_key("recipient_email") {
                        if let Some(recipient) = map.get("recipient").and_then(|v| v.as_str()) {
                            map.insert("recipient_email".to_string(), Value::String(recipient.to_string()));
                        } else if let Some(to) = map.get("to").and_then(|v| v.as_str()) {
                            map.insert("recipient_email".to_string(), Value::String(to.to_string()));
                        }
                    }
                }
                "SLACK_SEND_MESSAGE" => {
                    if !map.contains_key("channel") {
                        if let Some(ch) = map.get("channel_name").and_then(|v| v.as_str()) {
                            map.insert("channel".to_string(), Value::String(ch.to_string()));
                        }
                    }
                    if let Some(msg) = map.get("message").and_then(|v| v.as_str()) {
                        map.insert("text".to_string(), Value::String(msg.to_string()));
                    }
                }
                "GITHUB_CREATE_ISSUE" | "GITHUB_CREATE_AN_ISSUE" => {
                    if !map.contains_key("body") {
                        if let Some(desc) = map.get("description").and_then(|v| v.as_str()) {
                            map.insert("body".to_string(), Value::String(desc.to_string()));
                        }
                    }
                }
                "WEB_SEARCH" | "COMPOSIO_SEARCH_WEB" => {
                    if !map.contains_key("query") {
                        if let Some(q) = map.get("text").and_then(|v| v.as_str()) {
                            map.insert("query".to_string(), Value::String(q.to_string()));
                        }
                    }
                }
                _ => {}
            }
        }

        params
    }
}

fn extract_composio_message(value: &Value) -> Option<String> {
    let data = value.get("data")?;
    data.get("answer")
        .or_else(|| data.get("result"))
        .or_else(|| data.get("text"))
        .and_then(|v| v.as_str())
        .map(|s| s.to_string())
}

/// Result from tool execution
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct ToolResult {
    pub success: bool,
    pub simulated: bool,
    pub tool: String,
    pub message: String,
    pub data: Value,
}
