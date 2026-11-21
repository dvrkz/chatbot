# PRD: Caramel AI Tech Support Chat Assistant

## 1. Overview

Caramel AI is an LLM inferencing company, similar to OpenRouter, that gives customers a unified API to run a range of hosted models. This project is a customer service chat assistant for **Tech Support on LLM Inferencing**. It answers customers' technical questions about using the Caramel AI platform.

## 2. Objective

Build a web-based chat assistant that helps Caramel AI customers resolve LLM inferencing issues quickly, without waiting for a human agent.

## 3. Tech Stack

| Layer            | Choice                                   |
|------------------|------------------------------------------|
| Backend          | Java                                     |
| Framework        | Spring AI                                |
| Frontend         | React + TypeScript                       |
| LLM              | `gemma-4-26b-a4b-it`                     |
| Service provider | Google, via AI Studio                    |

## 4. Target Users

- Developers integrating Caramel AI's inference API into their applications.
- Technical and non-technical customers who need help with accounts, usage, or errors.

## 5. Scope

### In scope
- Chat UI where a user sends messages and sees streamed replies.
- Multi-turn conversations that keep context within a session.
- Answers on LLM inferencing topics, such as:
  - API usage and authentication
  - Model selection and availability
  - Rate limits, quotas, and errors (e.g. 401, 429, 5xx)
  - Latency, timeouts, and streaming issues
  - Token usage, context windows, and pricing/billing basics
  - Request parameters (temperature, max tokens, etc.)
- A system prompt that keeps the assistant on topic and in the Caramel AI support persona.
- A polite refusal or redirect for out-of-scope questions.

### Out of scope (initial version)
- User login and account-specific data lookups.
- Human agent handoff or ticketing integration.
- Voice or multimodal input.
- Persistent chat history across sessions.

## 6. Functional Requirements

1. **Chat endpoint**: the backend exposes a REST endpoint (with streaming support) that accepts a user message and a session ID.
2. **LLM integration**: the backend calls `gemma-4-26b-a4b-it` through Google AI Studio using Spring AI's `ChatClient`, authenticated with the Google API key from the `.env` file.
3. **Conversation memory**: the backend keeps recent messages per session and sends them to the model as context.
4. **System prompt**: the backend sets a configurable system prompt that defines the Caramel AI tech support role, tone, and boundaries.
5. **Chat UI**: the React frontend has a message list, an input box, a send button, a loading/typing indicator, and a "new conversation" action.
6. **Error handling**: the UI shows a friendly message when the backend or LLM call fails or times out.

## 7. Non-Functional Requirements

- **Security**: the Google AI Studio API key is stored in a `.env` file at the project root and loaded into the backend as an environment variable (`GOOGLE_API_KEY`). The `.env` file is git-ignored. The key is never committed, logged, or sent to the frontend.
- **Performance**: the first token should stream back within a few seconds under normal load.
- **Reliability**: the backend sets request timeouts and returns clear error responses.
- **Configurability**: model name, temperature, and system prompt are set in `application.yml`. Secrets such as the API key are set in `.env`, not in `application.yml`.
- **CORS**: the backend allows only the frontend's origin.

## 8. High-Level Architecture

```
React + TypeScript (browser)
        │  HTTP / SSE
        ▼
Spring Boot + Spring AI (Java)
        │  ChatClient
        ▼
Google AI Studio  →  gemma-4-26b-a4b-it
```

## 9. Success Metrics

- A user can hold a multi-turn tech support conversation end to end.
- Answers stay on topic and match the Caramel AI support persona.
- Responses stream to the UI without errors in a normal session.

## 10. Open Questions

- Should answers be grounded in Caramel AI docs (RAG) in a later phase?
- What is the escalation path for issues the assistant can't resolve?
- Are there content or privacy policies for logging conversations?
