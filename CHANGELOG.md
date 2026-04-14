# Changelog

All notable changes to AI Interview Assistant are documented here.
This project follows Semantic Versioning.

---

## [1.0.0] - 2025

### Added

#### Core Overlay
- Floating overlay bubble that stays on top of every app
- Transparent chat window with adjustable opacity
- Hold window for 3 seconds to enable drag mode
- Border turns gold during drag mode to indicate it is active
- Tap while dragging to lock window in position
- Bubble drag to reposition anywhere on screen
- Single tap bubble to open or close chat window
- Double tap bubble to stop the overlay service
- Safe screen area calculation that respects status bar and navigation bar

#### Speech Recognition
- Continuous speech recognition that never stops listening
- Auto-restart loop after each result or error
- Speech buffer that accumulates fragments before processing
- Silence detection that flushes buffer after 1.6 seconds
- Partial results shown in real time while user speaks
- Debounced partial display to reduce flickering
- Automatic cleanup every 20 restarts to prevent memory leaks

#### Speaker Identification
- 5 second voice calibration on first overlay open
- Volume baseline learned from user's own voice
- Scoring system using volume level and content analysis
- Interviewer detection based on quiet volume and question patterns
- User detection based on loud volume and long explanations
- Conversation flow awareness for better accuracy
- Visual indicator showing who is speaking

#### AI Intelligence
- Question detection using keyword matching
- IT domain keyword list with 100 plus terms
- Speech correction dictionary for common misrecognitions
- Auto-fixes terms like "in tune" to "Intune" and "d kim" to "DKIM"
- Context memory keeping last 16 messages for follow-up questions
- Queue system that holds questions while AI is processing
- Streaming responses that appear word by word
- Custom system prompt support for any job domain

#### Interview Expert Mode
- Deep knowledge of Microsoft 365 including Exchange SharePoint Teams OneDrive
- Microsoft Entra ID including Conditional Access MFA PIM and RBAC
- Microsoft Intune including MDM MAM Autopilot and compliance policies
- Email security including SPF DKIM DMARC and Exchange Online Protection
- Microsoft Defender for Endpoint Office 365 Identity and Cloud Apps
- Zero Trust security model and principles
- Active Directory DNS and Group Policy
- Zscaler ZIA and ZPA fundamentals
- macOS management with JAMF Pro and DEP
- Troubleshooting guidance for common IT issues

#### AI Providers
- NVIDIA NIM integration with 8 models
- Google Gemini integration with 4 models
- Custom OpenAI-compatible endpoint support
- Separate API key storage for each provider
- Per-provider model selection that persists between sessions
- Test connection button for each provider
- Streaming support for all providers

#### Settings
- Three-tab provider selector showing active provider with ACTIVE badge
- Green dot indicator showing which providers have keys configured
- Individual save and test buttons per provider section
- NVIDIA section with fast and powerful model groups
- Gemini section with free tier limit information
- Custom section with URL model name and key fields
- Custom prompt toggle with text editor
- Overlay transparency slider from 30 to 100 percent
- Text size slider from 10 to 24 sp
- Dark text mode toggle for light backgrounds
- Conversation export via copy to clipboard or share sheet
- Clear all conversations with confirmation dialog
- About section showing active provider and model

#### UI and Theme
- Deep space dark background color
- Glassmorphism card design with subtle borders
- Gradient header with logo and provider indicator
- Modern message bubbles with distinct AI card style
- Typing indicator with animated dots and thinking label
- Empty state with branded illustration
- Developer credit footer on home screen
- Gradient send button that activates when text is entered
- Proper insets padding for status bar and navigation bar

### Security
- API keys stored in SharedPreferences on device only
- allowBackup set to false to prevent cloud backup of keys
- No analytics or telemetry of any kind
- No data sent to any server except chosen AI provider

---

## [Unreleased]

### Planned
- Offline IT cheat sheet that works without internet
- Practice mode where AI asks the user interview questions
- Post-session summary with topics covered and action items
- Community custom prompt packs for different job roles
- Multi-language speech recognition and AI responses
- Home screen widget for quick overlay launch
- Session history and conversation replay
- Confidence indicator for speech recognition results