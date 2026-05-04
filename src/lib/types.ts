export type Stage = "idle" | "recording" | "processing" | "agent_running";

export interface CoordinatorStatus {
  stage: Stage;
  message: string;
}

export interface AppSettings {
  hotkey: string;
  whisper_model: string;
  llm_endpoint: string;
  llm_model: string;
  composio_api_key: string;
  auto_paste: boolean;
  confirm_actions: boolean;
}

export interface ClassifiedIntent {
  tool: string;
  parameters: Record<string, unknown>;
  confidence: number;
}

export interface ActionLogEntry {
  id: string;
  timestamp: number;
  transcription: string;
  intent?: ClassifiedIntent;
  result?: string;
  error?: string;
  stage: Stage;
}

export const TOOL_LABELS: Record<string, string> = {
  DICTATE: "Dictate",
  GMAIL_SEND_EMAIL: "Email",
  GITHUB_CREATE_ISSUE: "GitHub Issue",
  SLACK_SEND_MESSAGE: "Slack",
  GOOGLECALENDAR_EVENTS_LIST: "Calendar",
  NOTION_CREATE_PAGE: "Notion",
  WEB_SEARCH: "Search",
};
