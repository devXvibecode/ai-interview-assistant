# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x | Yes - active support |
| Below 1.0 | No |

---

## Reporting a Vulnerability

Please do not open a public GitHub issue to report a security vulnerability.
Public issues are visible to everyone and could put users at risk.

Instead send a private email to:
dev.vibecode@proton.me

### What to Include in Your Report

A clear description of the vulnerability and what it affects
The steps needed to reproduce the issue
The potential impact if the vulnerability were exploited
Your suggested fix if you have one in mind

### What to Expect

You will receive a response within 48 hours confirming receipt.
The vulnerability will be investigated and assessed promptly.
You will be kept informed of progress toward a fix.
A security patch will be released as soon as possible.
You will be credited in the release notes if you wish.

---

## Security Best Practices for Users

### API Keys
Never share your API keys with anyone.
Never commit API keys to any git repository.
Never post API keys in GitHub issues, discussions, or pull requests.
Each person using this app should have their own personal API key.
If you believe your key has been compromised rotate it immediately at the provider's website.

### Installation
Only download APK files from the official GitHub Releases page.
Verify you are on the correct repository at github.com/devXvibecode/ai-interview-assistant
Do not install APK files from unknown or unofficial sources.

### Updates
Keep the app updated to the latest version.
Security patches are included in new releases.
Check the Releases page regularly for updates.

---

## How API Keys Are Protected in This App

API keys are stored in Android SharedPreferences on your local device only.
The app manifest has allowBackup set to false.
This prevents keys from being included in Android backups to Google Drive.
Keys are never logged in logcat or any debug output.
Keys are transmitted directly to the AI provider using HTTPS only.
Keys are never sent to any server owned or operated by this project.

---

## Known Security Limitations

Android SharedPreferences is not encrypted by default.
On rooted devices a root user could potentially read SharedPreferences data.
If you are using this app on a rooted device be aware of this limitation.
A future version may implement EncryptedSharedPreferences to address this.

---

## Responsible Disclosure

We follow responsible disclosure practices.
Security issues will be patched before being publicly disclosed.
Researchers who responsibly disclose vulnerabilities will be credited.

---

## Contact

Security issues: dev.vibecode@proton.me
General issues: github.com/devXvibecode/ai-interview-assistant/issues