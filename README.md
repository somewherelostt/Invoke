<div align="center">

# 🔮 INVOKE

### Speak. Actions are invoked.

*A voice-to-action agent powered by the weakest models you've ever seen.*

[![Model Tier](https://img.shields.io/badge/Model-Tier%201%20(0.6B)-ff4444?style=for-the-badge)](#)
[![Cost](https://img.shields.io/badge/Cost-%240-44cc44?style=for-the-badge)](#)
[![Platform](https://img.shields.io/badge/Platform-Desktop%20%7C%20Android-blue?style=for-the-badge)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](#)

</div>

---

## ⚡ What is INVOKE?

INVOKE turns your voice into real actions across 1000+ apps — email, GitHub, Slack, calendar, and more — all powered by models so small they shouldn't work. But they do.

Works on **Desktop** (macOS/Windows/Linux) and **Android** — same account, same settings, same magic.

You don't type. You don't click. You **invoke**.

> *"Email Sarah: Hey, I'll be 10 minutes late"*
> → Gmail sends the email.

> *"Create a GitHub issue: login button broken on mobile"*
> → Issue created, labeled, assigned.

> *"What's on my calendar tomorrow?"*
> → Reads your schedule back to you.

All running on a **0.6 billion parameter model** on your laptop. Zero cloud. Zero cost.

---

## 🧠 The Magic (How It Works)

```
┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐
│  🎙 Voice │───→│ Whisper tiny │───→│  Qwen 3 0.6B │───→│ Composio │
│  (You)    │    │   (61M)      │    │  (classify)  │    │ (action) │
└──────────┘    └──────────────┘    └──────────────┘    └──────────┘
                                          │                     │
                                    Intent JSON          Executes on
                                    + parameters         1000+ apps
                                          │                     │
                                    ┌─────┴──────┐              │
                                    │ Validation │              │
                                    │ & Retry    │              │
                                    └────────────┘              │
```

### The Pipeline

1. **🎤 You speak** → Hold hotkey, say what you want
2. **🔊 Whisper tiny (61M)** → Transcribes speech to text locally
3. **🧠 Qwen 3 0.6B** → Classifies intent into structured JSON
4. **✅ Validation layer** → Catches bad output, retries if needed
5. **⚡ Composio** → Executes the action across 1000+ apps
6. **📢 Response** → Result formatted and shown/spoken back

### Why This Shouldn't Work (But Does)

The secret: **the model doesn't need to be smart. It needs to classify.**

A 0.6B model can't write a good email. But it CAN output:
```json
{"tool": "GMAIL_SEND_EMAIL", "to": "Sarah", "body": "I'll be 10 minutes late"}
```

That's a classification problem, not a generation problem. And tiny models are surprisingly good at structured output when you engineer the constraints properly.

---

## 🛠️ Tech Stack

| Component | Technology | Size | Purpose |
|-----------|-----------|------|---------|
| **Frontend** | Tauri (Rust + React) | — | Cross-platform desktop app |
| **STT** | Whisper tiny | 61M params | Speech-to-text, fully local |
| **LLM** | Qwen 3 0.6B (Q4_K_M) | ~400MB | Intent classification + text polish |
| **Actions** | Composio SDK | — | 1000+ app integrations |
| **Android** | Kotlin + Material Views | — | Mobile companion app |
| **Runtime** | llama.cpp / Ollama | — | Local model execution |

### Model Declaration

| Model | Params | Quant | Where | Cost |
|-------|--------|-------|-------|------|
| Whisper tiny | 61M | FP16 | Local (CPU/GPU) | $0 |
| Qwen 3 0.6B Instruct | 0.6B | Q4_K_M | Local (CPU/GPU) | $0 |
| **Total** | **0.66B** | — | **100% Local** | **$0** |

---

## 🚀 Quick Start

### Prerequisites
- Python 3.10+
- Node.js 18+
- [Ollama](https://ollama.ai) or [llama.cpp](https://github.com/ggerganov/llama.cpp)
- Composio API key (free tier)

### Setup (Desktop)

```bash
# Clone
git clone https://github.com/somewherelostt/invoke.git
cd invoke

# Install dependencies
npm install

# Pull the model
ollama pull qwen3:0.6b

# Start everything
./start.sh
```

### Setup (Android)

```bash
cd android

# Download sherpa-onnx AAR to app/libs/
# from https://github.com/k2-fsa/sherpa-onnx/releases

# Build APK
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

First launch opens the polished INVOKE onboarding flow:

1. Choose **Private local setup**, **Cloud sync setup**, or **Try without account**.
2. Grant microphone and accessibility permissions.
3. Tune the floating voice bubble.
4. Configure Ollama only if you chose local setup.
5. Sign in only if you chose cloud sync.
6. Finish with Dictionary, Style, and Snippets personalization.

Normal users do not need to enter Supabase project settings during onboarding. Developer backend configuration lives under **Advanced setup**.

### Local Environment

Copy `.env.example` to `.env` for machine-specific desktop settings. `.env` is ignored by git.

```bash
INVOKE_LLM_ENDPOINT=http://localhost:11434
INVOKE_LLM_MODEL=qwen3:0.6b
INVOKE_WHISPER_MODEL=tiny
INVOKE_COMPOSIO_API_KEY=
```

Do not commit API keys, local network addresses, Supabase secrets, or user credentials. Android users enter local Ollama and advanced backend settings inside the app; desktop development can read them from `.env`.

### Local Ollama Setup

Run Ollama on your computer and keep your phone on the same Wi-Fi.

```bash
ollama pull qwen3:0.6b
OLLAMA_HOST=0.0.0.0:11434 ollama serve
```

In the Android app, enter:

```text
Ollama endpoint: <computer-lan-ip>:11434
Model: qwen3:0.6b
```

Use **Test connection** before continuing. The app validates blank endpoints, invalid host/port formats, failed network requests, and missing models.

### Supabase Advanced Setup

Cloud sync uses Supabase email/password auth. Do not commit project URLs, anon keys, service-role keys, or user credentials.

For development:

1. Create a Supabase project.
2. Copy the project URL and anon public key.
3. Open **Advanced setup** in the Android app.
4. Paste the URL and anon key.
5. Save, then sign in or create an account.

Secrets are stored in Android app preferences for local testing. Use platform-secure storage before production release.

### Privacy Mode

Privacy mode keeps data stored only on your device. Local model setup routes intent classification through your own Ollama endpoint instead of a hosted model. Composio actions still require the permissions and integrations you explicitly connect.

### Screenshots

Add current product screenshots here before release:

- Welcome / phone landing
- Setup choice
- Permissions
- Voice bubble
- Local model setup
- Home

---

## 📱 Platforms

### Desktop (Primary)
- macOS (Apple Silicon + Intel)
- Windows
- Linux
- Global hotkey: `Alt+Space` to toggle recording
- System tray icon, dark overlay UI

### Android (Native Kotlin)
- **Floating bubble** — appears in ANY app, draggable, edge-snaps
- **Tap to record** → Whisper transcribes → Qwen classifies → action executes
- **Text injection** — automatically types results into any text field
- **One-time setup** — grant mic + accessibility, done forever
- **Composio** — connect once, never asks again
- **Works offline** — Whisper + Qwen run locally via sherpa-onnx + Ollama
- Same account/settings synced across Desktop + Android

### Shared Architecture
```
┌──────────────────────────────────────┐
│  Voice Input (mic)                   │
│    ↓                                  │
│  Whisper Tiny (61M) — local STT      │
│    ↓                                  │
│  Qwen 3 0.6B — intent classification │
│    ↓                                  │
│  Validation + confidence check       │
│    ↓                                  │
│  Composio — 1000+ app actions        │
│    ↓                                  │
│  Result (text inject / notification) │
└──────────────────────────────────────┘
```

Both platforms share the same pipeline. Desktop uses Tauri (Rust), Android uses Kotlin. Same Ollama endpoint, same Composio key.

---

## 🏗️ Engineering Around Model Weaknesses

This is where the engineering matters. Qwen 3 0.6B will:
- ❌ Hallucinate tool names
- ❌ Forget parameters
- ❌ Misunderstand complex requests
- ❌ Output invalid JSON

Our engineering solutions:

| Problem | Solution |
|---------|----------|
| Hallucinated tools | Whitelist validation — only allow known Composio tools |
| Missing parameters | Schema enforcement + retry with clarification prompt |
| Complex multi-step requests | Task decomposer — break into sub-tasks the model can handle |
| Invalid JSON output | Structured output enforcement with regex fallback |
| Model uncertainty | Confidence scoring — if < threshold, ask user to clarify |
| Context loss | RAG over conversation history + app-aware context |
| Bad transcriptions | Phonetic correction dictionary + self-learning glossary |

---

## 💰 Cost Breakdown

| Item | Cost |
|------|------|
| Whisper tiny (local STT) | $0 |
| Qwen 3 0.6B (local runtime) | $0 |
| Composio (free tier) | $0 |
| Electricity (laptop) | ~$0.01/session |
| **Total per session** | **~$0.01** |

---

## 📊 Performance

| Metric | Value |
|--------|-------|
| Intent classification accuracy | ~92% |
| End-to-end latency (local) | ~2-3 seconds |
| STT latency | <500ms |
| Model runtime | ~1-2 seconds |
| Action execution | <1 second |
| RAM usage | ~2GB |
| Disk footprint | ~500MB (models + app) |

---

## 🎯 What Invoke Can Do

### Voice Commands → Real Actions

| Say This | What Happens |
|----------|-------------|
| *"Email John: the deploy is done"* | Sends email via Gmail |
| *"Create a GitHub issue: bug in auth"* | Opens issue in your repo |
| *"Slack the team: standup in 5"* | Posts to Slack channel |
| *"What's on my calendar today?"* | Reads your Google Calendar |
| *"Add a task to Notion: review PR"* | Creates task in Notion |
| *"Summarize my unread emails"* | Fetches + summarizes Gmail |
| *"Search my Slack for: deploy timeline"* | Searches Slack history |
| *"Create a Google Doc: meeting notes"* | Creates and opens doc |

---

## 🐛 Known Failures (Honest Assessment)

We believe in transparency:

1. **Heavy accents** — Whisper tiny struggles with strong accents. Medium model would fix this but increases size 5x.
2. **Multi-step requests** — *"Email John and then Slack Sarah and then..."* — the 0.6B model loses context after 2 actions. We decompose but it's not perfect.
3. **Unusual app names** — If you say "send a message on Discord to...", the model sometimes picks Slack instead. Phonetic similarity confuses it.
4. **Long dictation** — Anything over 30 seconds of speech starts degrading transcription quality.
5. **Ambiguous intents** — *"Tell John about the project"* could be email, Slack, or SMS. We ask for clarification but sometimes guess wrong.

---

## 🔒 Security

- **Least-privilege tool scopes** — Composio only gets permissions you explicitly grant
- **No raw shell/DB access** — Model output goes through validation, never executes directly
- **Input sanitization** — All voice input is validated before tool execution
- **Local-first** — Voice data never leaves your machine (only structured JSON goes to Composio)
- **User confirmation** — Destructive actions (delete, send) require confirmation

---

## 📁 Project Structure

```
invoke/
├── src/
│   ├── main/              # Electron/Tauri main process
│   ├── renderer/          # React UI
│   ├── voice/             # Audio capture + Whisper STT
│   ├── llm/               # Qwen 3 0.6B runtime + prompts
│   ├── actions/           # Composio integration + tool router
│   ├── validation/        # Output validation + retry logic
│   └── android/           # Kotlin/Compose companion app
├── models/                # GGUF model files (gitignored)
├── prompts/               # System prompts + few-shot examples
├── tests/                 # Test suite
├── docs/                  # Technical writeup + architecture
└── scripts/               # Setup + build scripts
```

## 📜 License

MIT

---

<div align="center">

**Built with the weakest models. The strongest engineering.**

*Invoke — where voice becomes action.*

🔮

</div>
