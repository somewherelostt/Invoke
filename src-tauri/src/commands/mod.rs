use log::{info, error};
use std::sync::Arc;
use tauri::State;

use crate::agent::llm_client::LlmClient;
use crate::agent::tools::ToolExecutor;
use crate::coordinator::{CoordinatorState, Stage};
use crate::managers::audio::{AudioManager, AudioStateInner};
use crate::managers::transcription::TranscriptionManager;
use crate::settings::AppSettings;

/// Global audio state (Send + Sync safe)
pub struct AudioState(pub Arc<AudioStateInner>);

/// Global tool executor state (tokio Mutex for Send across await)
pub struct ToolState(pub Arc<tokio::sync::Mutex<ToolExecutor>>);

#[tauri::command]
pub async fn start_recording(
    coordinator: State<'_, CoordinatorState>,
    audio: State<'_, AudioState>,
) -> Result<String, String> {
    info!("🎙️ Start recording command received");

    AudioManager::start_recording(&audio.0).map_err(|e| {
        coordinator.set_stage(Stage::Idle);
        e.to_string()
    })?;

    coordinator.set_stage(Stage::Recording);
    Ok("Recording started".to_string())
}

#[tauri::command]
pub async fn stop_recording(
    coordinator: State<'_, CoordinatorState>,
    audio: State<'_, AudioState>,
    settings: State<'_, std::sync::Mutex<AppSettings>>,
    tool_state: State<'_, ToolState>,
) -> Result<serde_json::Value, String> {
    info!("🎙️ Stop recording command received");
    coordinator.set_stage(Stage::Processing);

    // 1. Stop recording and get samples
    let samples = AudioManager::stop_recording(&audio.0)
        .map_err(|e| e.to_string())?;

    if samples.is_empty() {
        coordinator.set_stage(Stage::Idle);
        return Ok(serde_json::json!({
            "error": "No audio captured. Check your microphone."
        }));
    }

    // 2. Convert to WAV
    let wav_bytes = AudioManager::samples_to_wav(&samples)
        .map_err(|e| e.to_string())?;

    info!("📦 Audio: {} samples, {} bytes WAV", samples.len(), wav_bytes.len());

    // 3. Transcribe via Whisper
    let transcriber = TranscriptionManager::new("http://127.0.0.1:8394");

    let transcription = match transcriber.transcribe(&wav_bytes).await {
        Ok(text) => {
            info!("📝 Transcribed: {}", text);
            text
        }
        Err(e) => {
            error!("Transcription failed: {}", e);
            coordinator.set_stage(Stage::Idle);
            return Ok(serde_json::json!({
                "error": format!("Transcription failed: {}", e)
            }));
        }
    };

    if transcription.trim().is_empty() {
        coordinator.set_stage(Stage::Idle);
        return Ok(serde_json::json!({
            "transcription": "",
            "intent": null,
            "message": "No speech detected"
        }));
    }

    // 4. Classify intent via Qwen 3 0.6B
    coordinator.set_stage(Stage::AgentRunning);

    let (llm_endpoint, llm_model) = {
        let s = settings.lock().map_err(|e| e.to_string())?;
        (s.llm_endpoint.clone(), s.llm_model.clone())
    };

    let llm = LlmClient::new(&llm_endpoint, &llm_model);

    // Sanitize transcription before sending to LLM
    let transcription = crate::agent::validation::sanitize_transcription(&transcription);

    let classified = llm.classify_intent(&transcription).await
        .unwrap_or_else(|e| {
            error!("Classification failed: {}", e);
            LlmClient::fallback_dictate(&transcription)
        });

    // Validate the classified intent (sanitize, check tool name, clamp confidence)
    let validated = crate::agent::validation::validate_intent(classified);
    let classified = validated.intent;

    info!("🧠 Classified: tool={}, confidence={:.2} (warnings: {})", 
        classified.tool, classified.confidence, validated.warnings.len());

    // 5. Execute the action via Composio
    let tool_result = {
        let executor = tool_state.0.lock().await;
        executor.execute(&classified).await.map_err(|e| e.to_string())?
    };

    info!("⚡ Execution: success={}, simulated={}", tool_result.success, tool_result.simulated);

    coordinator.set_stage(Stage::Idle);

    Ok(serde_json::json!({
        "transcription": transcription,
        "duration": samples.len() as f64 / 16000.0,
        "intent": classified,
        "execution": tool_result,
    }))
}

#[tauri::command]
pub fn get_status(coordinator: State<'_, CoordinatorState>) -> serde_json::Value {
    let status = coordinator.get_status();
    serde_json::to_value(status).unwrap_or_default()
}

#[tauri::command]
pub async fn update_settings(
    settings: State<'_, std::sync::Mutex<AppSettings>>,
    tool_state: State<'_, ToolState>,
    new_settings: AppSettings,
) -> Result<(), String> {
    // Check if composio key changed before holding any locks across awaits
    let key_changed = {
        let s = settings.lock().map_err(|e| e.to_string())?;
        s.composio_api_key != new_settings.composio_api_key
    }; // std MutexGuard dropped here
    
    if key_changed {
        let mut executor = tool_state.0.lock().await;
        executor.update_api_key(&new_settings.composio_api_key);
    }
    
    let mut s = settings.lock().map_err(|e| e.to_string())?;
    *s = new_settings;
    info!("⚙️ Settings updated");
    Ok(())
}

#[tauri::command]
pub fn get_settings(
    settings: State<'_, std::sync::Mutex<AppSettings>>,
) -> Result<AppSettings, String> {
    let s = settings.lock().map_err(|e| e.to_string())?;
    Ok(s.clone())
}

#[tauri::command]
pub async fn invoke_action(
    coordinator: State<'_, CoordinatorState>,
    tool_state: State<'_, ToolState>,
    settings: State<'_, std::sync::Mutex<AppSettings>>,
    text: String,
) -> Result<serde_json::Value, String> {
    info!("🔮 Invoke action: {}", text);
    coordinator.set_stage(Stage::Processing);

    let (llm_endpoint, llm_model) = {
        let s = settings.lock().map_err(|e| e.to_string())?;
        (s.llm_endpoint.clone(), s.llm_model.clone())
    };

    let llm = LlmClient::new(&llm_endpoint, &llm_model);
    
    let classified = llm.classify_intent(&text).await
        .unwrap_or_else(|e| {
            error!("Classification failed: {}", e);
            LlmClient::fallback_dictate(&text)
        });

    // Validate intent
    let validated = crate::agent::validation::validate_intent(classified);
    let classified = validated.intent;

    let tool_result = {
        let executor = tool_state.0.lock().await;
        executor.execute(&classified).await.map_err(|e| e.to_string())?
    };

    coordinator.set_stage(Stage::Idle);
    
    Ok(serde_json::json!({
        "intent": classified,
        "execution": tool_result,
    }))
}
