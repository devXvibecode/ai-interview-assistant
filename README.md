Paste this directly into your `README.md` file:

```
<div align="center">

# 🤝 AI Interview Assistant

**Real-time AI-powered interview assistance with floating overlay,
live speech detection, and intelligent speaker identification.**

[![License: MIT](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-26%2B-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![NVIDIA](https://img.shields.io/badge/NVIDIA%20NIM-Free%20Tier-76b900)](https://build.nvidia.com)
[![Gemini](https://img.shields.io/badge/Google%20Gemini-Free%20Tier-4285F4)](https://aistudio.google.com)

*Built by [vibecode](https://github.com/devXvibecode)*

[Features](#-features) • [Quick Start](#-quick-start) • [API Keys](#-getting-api-keys) • [Build](#-building-from-source) • [Contributing](#-contributing)

</div>

---

## ✨ Features

### 🎯 Core
- **Floating Overlay Bubble** — Stays on top of every app, fully draggable
- **Live Speech Recognition** — Continuous listening with auto-restart loop
- **Smart Speaker ID** — Distinguishes interviewer from you via voice calibration
- **Real-time AI Responses** — Streaming answers appear word by word
- **Transparent Chat Window** — See your screen and AI answers simultaneously
- **Hold to Move** — Hold overlay window 3 seconds to drag and reposition

### 🧠 AI Intelligence
- **Question Detection** — Only responds to relevant questions, ignores filler speech
- **IT Domain Keywords** — 100+ Microsoft and IT terms for accurate detection
- **Speech Correction** — Auto-fixes misrecognitions (e.g. "in tune" becomes "Intune")
- **Context Memory** — Remembers full conversation for follow-up questions
- **Expert Interview Mode** — Deep knowledge of M365, Intune, Entra ID, Security, DNS, Zscaler, JAMF

### 🔌 Multiple AI Providers

| Provider | Models | Cost |
|----------|--------|------|
| NVIDIA NIM | Llama 3.1 8B, 70B, 405B — Llama 3.3 70B — Mistral 7B, Nemo — Mixtral 8x22B — DeepSeek R1 | Free eval |
| Google Gemini | Gemini 2.0 Flash — 1.5 Flash — 1.5 Flash 8B — 1.5 Pro | Free tier |
| Custom | Any OpenAI-compatible API endpoint | Varies |

### 🎨 UI and UX
- Deep space dark theme with glassmorphism card design
- Adjustable overlay transparency and text size
- Safe screen area that respects status bar and navigation bar
- Modern settings with per-provider API key management
- Conversation export via copy or share

---

## 📱 Screenshots

| Home Screen | Overlay Active | Settings |
|-------------|----------------|----------|
| Coming soon | Coming soon | Coming soon |

---

## 🚀 Quick Start

### Requirements
- Android phone running API 26 or higher (Android 8.0 minimum)
- NVIDIA NIM or Google Gemini API key (both are free)
- Microphone permission
- Display over other apps permission

### Installation

**Option 1 — Download APK (Recommended)**
1. Go to [Releases](../../releases)
2. Download the latest app-debug.apk
3. On your phone go to Settings and enable Install unknown apps
4. Install and open the app

**Option 2 — Build from source**
See the Building from Source section below

### First Launch
1. Open the app and tap the Settings icon
2. Choose your provider — NVIDIA or Gemini
3. Enter your API key then tap Save and Test
4. Go back to home and tap the chat bubble icon to launch overlay
5. Speak naturally — the AI responds to detected questions automatically

---

## 🔑 Getting API Keys

### NVIDIA NIM — Free until 2027

1. Visit https://build.nvidia.com
2. Create a free account (no credit card needed)
3. Choose any model and click Get API Key
4. Your key will start with nvapi-

### Google Gemini — Free Forever

1. Visit https://aistudio.google.com
2. Sign in with your Google account
3. Click Get API Key then Create API Key
4. Your key will start with AIzaSy

Never share your API keys and never commit them to git.

---

## 🎯 How It Works

### Interview Mode Step by Step

SETUP — takes only 5 seconds the first time

  Launch the overlay bubble
  A calibration prompt will appear
  Say a few words at your normal speaking volume
  The app learns your voice level to identify the interviewer

DURING THE INTERVIEW

  Interviewer asks a question
  AI detects it as a question
  Answer appears on the transparent overlay
  You read and respond naturally

  You speak your answer
  Detected as your voice
  Skipped — no AI call made

OVERLAY CONTROLS

  Tap bubble once      Open or close the chat window
  Double tap bubble    Stop the overlay service completely
  Drag bubble          Move the bubble anywhere on screen
  Hold window 3s       Enable window drag mode border turns gold
  Tap while dragging   Lock the window in its current position

---

## 🏗️ Building from Source

### Requirements

| Tool | Version |
|------|---------|
| Android SDK | API 34 |
| JDK | 17 |
| AGP | 8.7.3 |
| Gradle | 8.11.1 |
| Kotlin | 1.9.24 |

### Build on Desktop (Android Studio)

Clone the repository
cd into the folder
Run ./gradlew assembleDebug

### Build on Mobile (AndroidIDE)

Clone the repository
Move the folder to your AndroidIDEProjects directory
Open in AndroidIDE and run assembleDebug

### APK Output Location

app/build/outputs/apk/debug/app-debug.apk

---

## 📁 Project Structure

app/src/main/java/com/assistant/app/

ai/
  ChatMessage.kt           Message data model and role enum
  ApiModels.kt             NVIDIA request and response models
  GeminiApiClient.kt       Google Gemini streaming API client
  NvidiaApiClient.kt       NVIDIA NIM streaming API client
  ConversationManager.kt   Core AI logic and speaker identification
  AIMode.kt                Interview mode enum definitions
  SystemPrompts.kt         Expert system prompts per mode

service/
  OverlayService.kt        Floating overlay foreground service
  SpeechService.kt         Continuous speech recognition loop
  NotificationHelper.kt    Foreground notification builder

ui/
  screens/
    HomeScreen.kt          Main chat interface
    SettingsScreen.kt       Provider model and preference settings
  components/
    MessageBubble.kt       Chat bubble composable
    QuickActions.kt        Quick action chips row
  theme/
    Color.kt               Deep space color palette
    Theme.kt               Material3 dark theme
    Type.kt                Typography definitions

data/
  AppPreferences.kt        SharedPreferences wrapper for all settings

utils/
  Constants.kt             All constants model IDs and API URLs
  Extensions.kt            Kotlin extension functions
  PermissionHelper.kt      Runtime permission handling
  SpeechCorrector.kt       IT terminology correction dictionary

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 100% |
| UI Framework | Jetpack Compose with Material3 |
| Overlay System | Android WindowManager View-based |
| Speech Engine | Android SpeechRecognizer API |
| Networking | OkHttp 4 with Gson |
| Local Storage | SharedPreferences |
| Async | Kotlin Coroutines and Flow |
| Build System | Gradle with Groovy DSL |

---

## 🤝 Contributing

All contributions are welcome. See CONTRIBUTING.md for full guidelines.

Fork the repo on GitHub
Clone your fork
Create a branch with a descriptive name
Make your changes and commit using conventional commits
Push and open a Pull Request

### Commit Message Format

feat: add new feature
fix: resolve a bug
docs: update documentation
refactor: clean up code without changing behavior
style: formatting only
chore: update dependencies or config

---

## 📋 Roadmap

- [ ] Offline IT cheat sheet that works without internet
- [ ] Practice mode where AI asks you interview questions
- [ ] Post-session interview summary and report
- [ ] Community custom prompt packs for different job roles
- [ ] Multi-language support for international users
- [ ] Home screen widget for quick overlay launch
- [ ] Session history and conversation replay

---

## 📜 License

This project is licensed under the MIT License.
See LICENSE for full details.
You are free to use, modify, and distribute this project.
Attribution is appreciated but not required.

---

## 🔒 Privacy

This app collects zero data.
No analytics, no tracking, no servers of our own.
Your conversations go only to the AI provider you choose.
See PRIVACY.md for full details.

---

## 🐛 Issues and Support

Found a bug? Open a bug report via the Issues tab
Have an idea? Request a feature via the Issues tab
General question? Open a discussion via the Discussions tab

---

## 👨‍💻 Developer

<div align="center">

**vibecode**

[![Email](https://img.shields.io/badge/Email-dev.vibecode%40proton.me-purple)](mailto:dev.vibecode@proton.me)
[![GitHub](https://img.shields.io/badge/GitHub-%40devXvibecode-181717)](https://github.com/devXvibecode)

*Building tools that make people smarter.*

---

⭐ Star this repo if it helped you crack an interview!

</div>
```