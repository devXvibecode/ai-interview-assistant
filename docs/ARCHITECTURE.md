# Architecture

This document explains how the app is built and why certain decisions were made.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose with Material3 |
| Overlay | Android WindowManager with Views |
| Speech | Android SpeechRecognizer API |
| Networking | OkHttp 4 with Gson |
| Storage | SharedPreferences |
| Async | Kotlin Coroutines and Flow |
| Build | Gradle with Groovy DSL |

---

## Key Design Decisions

### No ViewModel
ConversationManager acts as the state holder and is passed directly to composables.
This works cleanly with the foreground service architecture and keeps things simple.

### No Room Database
Conversations are kept in memory only during the session.
Users can export conversations manually if they want to save them.
This approach is simpler and better for user privacy.

### No Dependency Injection
There is no Hilt or Koin in this project.
Dependencies are passed manually.
This makes the codebase more beginner-friendly and easier to understand.

### Two Separate API Clients
NvidiaApiClient handles the OpenAI-compatible API format used by NVIDIA and custom endpoints.
GeminiApiClient handles Google's unique API format which is completely different.
ConversationManager decides which client to use based on the active provider setting.

---

## How Data Flows Through the App

User speaks into the microphone

SpeechService captures audio using Android SpeechRecognizer
Speech fragments are collected into a buffer
When silence is detected after 1.6 seconds the buffer is flushed

SpeechCorrector.correct() runs on the text
This fixes common IT terminology misrecognitions
For example "in tune" becomes "Intune"

ConversationManager.onSpeechDetected() receives the cleaned text

Speaker identification runs using a scoring system
Volume level is compared against the calibration baseline
Content is analyzed for question words and IT keywords
A score determines if this is the interviewer or the user

If it is the interviewer and the text is a question
The correct AI client is selected based on active provider
The request is sent with streaming enabled
Chunks arrive and update the UI word by word

---

## Speaker Identification Logic

During the 5 second calibration phase the user speaks normally.
The app records the average volume level of the user's voice.
This becomes the baseline.

The threshold is set at 65 percent of the baseline.

For each piece of detected speech a score is calculated.

Volume below threshold adds 3 points to the interviewer score.
Volume above threshold adds 3 points to the user score.
Contains question words adds 2 points to the interviewer score.
Contains IT domain keywords and a question adds 2 more interviewer points.
Long explanation over 15 words adds 2 points to the user score.
Previous speech was a question adds 1 user point (you are now answering).
Previous speech was an answer adds 1 interviewer point (next question coming).

The highest score wins and determines the speaker identity.

---

## Overlay Architecture

The overlay is built as a foreground service using Android WindowManager.

OverlayService runs as a foreground service with microphone type.

The bubble is an ImageView added to WindowManager.
Single tap opens or closes the chat window.
Double tap stops the overlay service.
Dragging moves the bubble to any screen position.

The chat window is a LinearLayout inflated from XML.
It contains a header row with status and speaker indicator.
A ScrollView holds the message list as a LinearLayout.
Long pressing the window for 3 seconds activates drag mode.
The border turns gold to indicate drag mode is active.
Tapping while in drag mode locks the window position.

---

## File Reference

ai/
  ChatMessage.kt           Message data class and MessageRole enum
  ApiModels.kt             NvidiaRequest NvidiaResponse and ApiMsg classes
  GeminiApiClient.kt       Gemini REST API with server-sent events streaming
  NvidiaApiClient.kt       NVIDIA and OpenAI compatible REST with streaming
  ConversationManager.kt   All state management speaker ID and AI routing
  AIMode.kt                Enum with INTERVIEW_FULL CUSTOM GENERAL MEETING LEARNING
  SystemPrompts.kt         System prompts for each mode including IT expert prompt

service/
  OverlayService.kt        WindowManager overlay bubble and chat window management
  SpeechService.kt         Continuous SpeechRecognizer with buffer and auto-restart
  NotificationHelper.kt    Builds the foreground service notification

data/
  AppPreferences.kt        All SharedPreferences read and write operations

utils/
  Constants.kt             Model IDs API URLs preference keys and app branding
  Extensions.kt            dpToPx toTimeString showToast and other helpers
  PermissionHelper.kt      Audio overlay and notification permission checks
  SpeechCorrector.kt       Dictionary of IT term corrections applied before AI call