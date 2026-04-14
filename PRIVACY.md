# Privacy Policy

## The Short Version

This app collects absolutely nothing.
No data is sent to our servers because we have no servers.
Your conversations go only to the AI provider you choose.

---

## What This App Does NOT Collect

- No usage analytics or statistics
- No crash reports unless you manually share them
- No personal information of any kind
- No device identifiers
- No location data
- No contact information
- No behavioral tracking

---

## What Happens to Your Data

### API Keys
Your API keys are stored locally on your device using Android SharedPreferences.
They are never transmitted to anyone except the AI provider you configure.
They are never logged, cached externally, or shared with any third party.
The app manifest has allowBackup set to false so keys are never backed up to Google Drive or any cloud service.

### Conversation Data
Your conversations exist in memory only while the app session is active.
They are permanently deleted when you close the overlay or clear the chat.
They are never written to disk permanently.
They are never stored on any external server.
The only place conversation text goes is to the AI provider you have selected in Settings.

### Voice and Audio Data
The app uses Android's built-in SpeechRecognizer to process your voice.
No audio is recorded or stored by this app.
The SpeechRecognizer sends audio data to Google's speech recognition service.
This is subject to Google's own privacy policy which can be found at policies.google.com/privacy

---

## Third-Party Services

When you use this app your conversation text is sent to whichever AI provider you configure.

If you use NVIDIA NIM your conversations are sent to NVIDIA.
Their privacy policy is at nvidia.com/en-us/about-nvidia/privacy-policy

If you use Google Gemini your conversations are sent to Google.
Their privacy policy is at policies.google.com/privacy

If you use a custom endpoint your conversations are sent to whatever service you configure.
You are responsible for understanding the privacy policy of your custom endpoint.

We have no control over and no visibility into how these third-party providers handle your data.

---

## Children's Privacy

This app is not intended for use by children under the age of 13.
We do not knowingly collect any information from children.

---

## Open Source Verification

This app is fully open source.
You can read every line of code and verify all privacy claims yourself.
The source code is available at github.com/devXvibecode/ai-interview-assistant

---

## Changes to This Policy

If this privacy policy changes in the future the updated version will be
committed to the repository with a clear description of what changed.

---

## Contact

If you have any questions about this privacy policy please contact:
dev.vibecode@proton.me

Last updated: 2025