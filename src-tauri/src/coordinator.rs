use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU8, Ordering};
use std::sync::Arc;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Stage {
    Idle,
    Recording,
    Processing,
    AgentRunning,
}

impl Stage {
    pub fn as_str(&self) -> &'static str {
        match self {
            Stage::Idle => "idle",
            Stage::Recording => "recording",
            Stage::Processing => "processing",
            Stage::AgentRunning => "agent_running",
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CoordinatorStatus {
    pub stage: Stage,
    pub message: String,
}

pub struct CoordinatorState {
    stage: Arc<AtomicU8>,
}

impl CoordinatorState {
    pub fn new() -> Self {
        Self {
            stage: Arc::new(AtomicU8::new(0)), // Idle = 0
        }
    }

    pub fn get_stage(&self) -> Stage {
        match self.stage.load(Ordering::SeqCst) {
            0 => Stage::Idle,
            1 => Stage::Recording,
            2 => Stage::Processing,
            3 => Stage::AgentRunning,
            _ => Stage::Idle,
        }
    }

    pub fn set_stage(&self, stage: Stage) {
        let val = match stage {
            Stage::Idle => 0,
            Stage::Recording => 1,
            Stage::Processing => 2,
            Stage::AgentRunning => 3,
        };
        self.stage.store(val, Ordering::SeqCst);
    }

    pub fn get_status(&self) -> CoordinatorStatus {
        let stage = self.get_stage();
        let message = match stage {
            Stage::Idle => "Ready".to_string(),
            Stage::Recording => "Listening...".to_string(),
            Stage::Processing => "Processing...".to_string(),
            Stage::AgentRunning => "Executing action...".to_string(),
        };
        CoordinatorStatus { stage, message }
    }
}
