use log::{info, warn};
use crate::agent::llm_client::ClassifiedIntent;
use crate::agent::types::SUPPORTED_TOOLS;

/// Validation result
pub struct ValidatedIntent {
    pub intent: ClassifiedIntent,
    pub warnings: Vec<String>,
    pub retries_used: u32,
}

/// Validate and sanitize a classified intent
pub fn validate_intent(intent: ClassifiedIntent) -> ValidatedIntent {
    let mut warnings = Vec::new();
    let mut intent = intent;

    // 1. Validate tool name
    let valid_tool = SUPPORTED_TOOLS.contains(&intent.tool.as_str());
    if !valid_tool {
        warn!("⚠️ Invalid tool: {}, falling back to DICTATE", intent.tool);
        warnings.push(format!("Unknown tool '{}' → DICTATE", intent.tool));
        intent.tool = "DICTATE".to_string();
    }

    // 2. Clamp confidence to [0.0, 1.0]
    if intent.confidence < 0.0 {
        warnings.push(format!("Clamped negative confidence {:.2} → 0.0", intent.confidence));
        intent.confidence = 0.0;
    }
    if intent.confidence > 1.0 {
        warnings.push(format!("Clamped confidence {:.2} → 1.0", intent.confidence));
        intent.confidence = 1.0;
    }

    // 3. Validate parameters is an object
    if !intent.parameters.is_object() {
        warn!("⚠️ Parameters not an object, defaulting to empty");
        warnings.push("Parameters were not a JSON object".to_string());
        intent.parameters = serde_json::json!({});
    }

    // 4. Sanitize string values in parameters (strip control chars, limit length)
    if let Some(map) = intent.parameters.as_object_mut() {
        let keys_to_update: Vec<String> = map.keys().cloned().collect();
        for key in keys_to_update {
            if let Some(val) = map.get(&key).and_then(|v| v.as_str()) {
                let sanitized = sanitize_string(val);
                if sanitized != val {
                    warnings.push(format!("Sanitized parameter '{}'", key));
                    map.insert(key, serde_json::Value::String(sanitized));
                }
            }
        }
    }

    // 5. For DICTATE, ensure text parameter exists
    if intent.tool == "DICTATE" {
        if !intent.parameters.get("text").and_then(|v| v.as_str()).is_some() {
            warnings.push("DICTATE missing text parameter".to_string());
        }
    }

    // 6. For action tools, check for required fields
    match intent.tool.as_str() {
        "GMAIL_SEND_EMAIL" => {
            if !intent.parameters.get("to").is_some() && !intent.parameters.get("recipient").is_some() {
                warnings.push("Email missing recipient".to_string());
                intent.confidence = (intent.confidence * 0.5).min(0.3); // Penalize
            }
        }
        "SLACK_SEND_MESSAGE" => {
            if !intent.parameters.get("message").is_some() && !intent.parameters.get("text").is_some() {
                warnings.push("Slack missing message body".to_string());
                intent.confidence = (intent.confidence * 0.5).min(0.3);
            }
        }
        "GITHUB_CREATE_ISSUE" => {
            if !intent.parameters.get("title").is_some() {
                warnings.push("GitHub issue missing title".to_string());
                intent.confidence = (intent.confidence * 0.5).min(0.3);
            }
        }
        _ => {}
    }

    if !warnings.is_empty() {
        info!("⚠️ Validation warnings: {:?}", warnings);
    }

    ValidatedIntent {
        intent,
        warnings,
        retries_used: 0,
    }
}

/// Sanitize a string: strip control chars, limit length
fn sanitize_string(s: &str) -> String {
    let trimmed: String = s.chars()
        .filter(|c| !c.is_control() || *c == '\n' || *c == '\t')
        .take(2000) // Max 2000 chars per parameter
        .collect();
    trimmed
}

/// Sanitize transcription input (strip potential injection)
pub fn sanitize_transcription(text: &str) -> String {
    text.chars()
        .filter(|c| !c.is_control() || *c == ' ')
        .take(500) // Max 500 chars transcription
        .collect::<String>()
        .trim()
        .to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_validate_valid_intent() {
        let intent = ClassifiedIntent {
            tool: "GMAIL_SEND_EMAIL".to_string(),
            parameters: serde_json::json!({"to": "test@example.com", "body": "Hello"}),
            confidence: 0.9,
        };
        let result = validate_intent(intent);
        assert_eq!(result.intent.tool, "GMAIL_SEND_EMAIL");
        assert!(result.warnings.is_empty());
    }

    #[test]
    fn test_validate_invalid_tool() {
        let intent = ClassifiedIntent {
            tool: "HACK_THE_PLANET".to_string(),
            parameters: serde_json::json!({}),
            confidence: 0.5,
        };
        let result = validate_intent(intent);
        assert_eq!(result.intent.tool, "DICTATE");
        assert!(!result.warnings.is_empty());
    }

    #[test]
    fn test_validate_overflow_confidence() {
        let intent = ClassifiedIntent {
            tool: "DICTATE".to_string(),
            parameters: serde_json::json!({"text": "hello"}),
            confidence: 5.0,
        };
        let result = validate_intent(intent);
        assert_eq!(result.intent.confidence, 1.0);
    }
}
