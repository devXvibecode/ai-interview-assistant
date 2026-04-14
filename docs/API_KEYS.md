# API Keys Guide

This guide explains how to get free API keys for each supported provider.

---

## NVIDIA NIM — Free Evaluation

NVIDIA NIM provides access to powerful open source models for free.
No credit card is required.
Your free evaluation access lasts until 2027.

### How to Get Your Key

1. Visit https://build.nvidia.com
2. Create a free account
3. Browse the model catalog
4. Click any model such as Llama 3.1 8B
5. Click Get API Key
6. Copy your key — it starts with nvapi-

### Available Models

| Model | Speed | Best Use |
|-------|-------|----------|
| meta/llama-3.1-8b-instruct | Very fast | Quick interview Q&A |
| meta/llama-3.3-70b-instruct | Fast | Better quality answers |
| meta/llama-3.1-70b-instruct | Fast | Reliable performance |
| meta/llama-3.1-405b-instruct | Slower | Most powerful responses |
| mistralai/mistral-7b-instruct-v0.3 | Very fast | Lightweight alternative |
| mistralai/mistral-nemo-12b-instruct | Fast | Balanced quality |
| mistralai/mixtral-8x22b-instruct-v0.1 | Slower | Expert level responses |
| deepseek-ai/deepseek-r1 | Slower | Reasoning and analysis |

---

## Google Gemini — Free Forever

Google Gemini offers a generous free tier with no expiry date.
No credit card is required.

### How to Get Your Key

1. Visit https://aistudio.google.com
2. Sign in with your Google account
3. Click Get API Key in the top menu
4. Click Create API Key
5. Copy your key — it starts with AIzaSy

### Available Models

| Model | Speed | Free Limit |
|-------|-------|------------|
| gemini-2.0-flash | Fastest | 15 requests per minute, 1 million tokens per day |
| gemini-1.5-flash | Very fast | 15 requests per minute, 1 million tokens per day |
| gemini-1.5-flash-8b | Fastest | 15 requests per minute, 1 million tokens per day |
| gemini-1.5-pro | Slower | 2 requests per minute, 50 requests per day |

Recommended model for interviews: gemini-2.0-flash
It is the fastest and smartest free option available.

---

## Custom Endpoint

The custom endpoint option lets you connect to any API that uses the OpenAI format.

Examples of compatible services:
- Ollama running locally on your computer or home server
- OpenRouter which provides access to many models
- Any self-hosted LLM with an OpenAI-compatible API

### What You Need to Configure

Base URL — the API endpoint address
Example: http://localhost:11434/v1

Model Name — the exact name of the model
Example: llama3 or mistral

API Key — depends on the service you are using
Some local services do not require a key at all

---

## Security Rules

Never commit API keys to git under any circumstances
Never share your API keys with other people
Never post API keys in GitHub issues or discussions
Every user of this app should use their own personal key
Keys are stored only on your local device
Keys are never sent to our servers because we have no servers

---

## Where Keys Are Stored

All API keys are stored in Android SharedPreferences on your device.
They are never backed up to the cloud.
The app manifest has allowBackup set to false for this reason.
Keys are only ever sent directly to the AI provider you select.