# Contributing to AI Interview Assistant

Thank you for your interest in contributing.
Every contribution no matter how small makes this project better.

---

## Ways to Contribute

- Report bugs by opening an issue using the bug report template
- Suggest features by opening an issue using the feature request template
- Fix bugs by submitting a pull request
- Add new features by submitting a pull request
- Improve documentation by editing any markdown file
- Share custom prompt packs for different job domains
- Star the repository to help others find it

---

## Getting Started

### 1. Fork the repository

Go to github.com/devXvibecode/ai-interview-assistant and click Fork.

### 2. Clone your fork

git clone https://github.com/YOUR_USERNAME/ai-interview-assistant.git
cd ai-interview-assistant

### 3. Create a branch

Use a descriptive name that explains what you are working on.

For a new feature:
git checkout -b feat/offline-cheat-sheet

For a bug fix:
git checkout -b fix/overlay-crash-android-14

For documentation:
git checkout -b docs/improve-api-key-guide

### 4. Make your changes

Follow the code guidelines below.
Test on a real Android device.
Make sure the app builds successfully.

### 5. Commit your changes

Follow the commit message format below.

git add .
git commit -m "feat: add offline IT cheat sheet"

### 6. Push and open a Pull Request

git push origin feat/offline-cheat-sheet

Then go to GitHub and open a Pull Request against the main branch.
Fill out the PR template completely.

---

## Commit Message Format

This project follows Conventional Commits.
Every commit message must start with one of these prefixes.

feat: add a new feature
fix: resolve a bug
docs: update or add documentation
refactor: change code structure without changing behavior
style: formatting changes only no logic changes
test: add or update tests
chore: update dependencies configuration or build files
perf: improve performance
ci: changes to GitHub Actions or CI configuration

### Examples

feat: add practice mode where AI asks interview questions
fix: resolve overlay crash on Android 14
docs: add troubleshooting guide to SETUP.md
refactor: simplify speech buffer flush logic
chore: update OkHttp to 4.12.0
style: apply consistent spacing in SettingsScreen

---

## Pull Request Rules

Every pull request must meet these requirements before it will be merged.

The app builds successfully with ./gradlew assembleDebug
Tested on a real Android device not just an emulator
No API keys or secrets are present anywhere in the code
CHANGELOG.md is updated with a description of the change
The PR template is filled out completely
Code follows the existing project style

---

## Code Guidelines

### Language
Kotlin only. No Java files.

### Architecture
Follow the existing patterns. No new frameworks or libraries without discussion.
No ViewModel. No Room. No Hilt. No Koin.
Keep it simple and beginner-friendly.

### Constants
All string constants, numbers, model IDs, and URLs go in Constants.kt.
Never hardcode values directly in business logic files.

### Naming
Use clear and descriptive names for variables, functions, and classes.
Follow Kotlin naming conventions throughout.

### Comments
Add comments for complex logic that is not immediately obvious.
Do not add comments that just repeat what the code already says.

### Functions
Keep functions small and focused on a single responsibility.
If a function is getting long consider breaking it into smaller pieces.

---

## What NOT to Submit

These will cause your pull request to be rejected immediately.

API keys of any kind in any file
Compiled APK files or build outputs
The app/build/ directory or any generated files
Keystore files or signing certificates
The gradle.properties file if it contains any secrets
Personal data, logs, or conversation transcripts
Binary files that do not belong in source control

---

## Reporting Security Issues

Do not open a public GitHub issue for security vulnerabilities.
Email dev.vibecode@proton.me directly.
See SECURITY.md for the full security policy.

---

## Questions

If you have a question about contributing open a GitHub Discussion.
For anything else email dev.vibecode@proton.me

Thank you for helping make this project better.