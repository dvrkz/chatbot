# Phase 1 Data Model: Caramel AI Tech Support Chat Assistant

**Date**: 2026-11-14 | **Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

Nothing here is persisted to a database. Server state is in memory and expires. Browser state is
component state and is lost on reload, which is the intended behavior (FR-015).

## Server-side entities

### ConversationMemory (keyed by session ID)

The ordered recent messages the model receives as context for one conversation.

| Field | Type | Rules |
|-------|------|-------|
| sessionId | UUID string | Required. Must parse as a UUID. Used as the memory conversation ID. |
| messages | ordered list of memory messages | Holds at most `caramel.memory.window-size` messages (default 20). Oldest are evicted first. |
| lastAccess | instant | Updated on every read or write. Drives expiry. |

Lifecycle:

```text
(absent) --first message--> ACTIVE --idle > ttl-----------> EVICTED (absent)
                              |    --DELETE session-------> EVICTED (absent)
                              |    --cache size cap hit---> EVICTED (oldest-accessed first)
```

- A request for an evicted or unknown session ID is valid and simply starts an empty conversation.
- The system prompt is not stored in memory. It is supplied on every call from configuration.

### MemoryMessage

| Field | Type | Rules |
|-------|------|-------|
| role | `USER` or `ASSISTANT` | No other roles are stored. |
| text | string | Customer text is at most `caramel.limits.max-message-chars`. Assistant text is bounded by the configured max output tokens. |

An assistant reply that failed or timed out mid-stream MUST NOT be stored as a complete turn (see
"Failed turn handling" below).

### OperatorSettings (typed configuration, `caramel.*`)

Bound once at startup from `application.yml`. Validated at startup. Invalid values fail startup.

| Property | Type | Default | Validation |
|----------|------|---------|------------|
| `caramel.assistant.system-prompt` | string | persona text (see below) | Not blank. No `{` or `}` characters. |
| `caramel.support.contact` | string | `TODO-SUPPORT-CONTACT` | Not blank. Release gate: must not equal the placeholder. |
| `caramel.memory.window-size` | int | 20 | 2 to 100 |
| `caramel.memory.session-ttl` | duration | 30m | 1m to 24h |
| `caramel.memory.max-sessions` | int | 10000 | 100 to 1,000,000 |
| `caramel.limits.max-message-chars` | int | 2000 | 1 to 20,000 |
| `caramel.limits.reply-timeout` | duration | 30s | 5s to 5m |
| `caramel.limits.idle-timeout` | duration | 15s | 2s to `reply-timeout` |
| `caramel.limits.requests-per-minute` | int | 10 | 1 to 600 |
| `caramel.limits.burst` | int | 5 | 1 to 100 |
| `caramel.cors.allowed-origin` | URL string | `http://localhost:5173` | Exactly one origin, no `*` |
| `spring.ai.google.genai.chat.model` | string | `gemma-4-26b-a4b-it` | Not blank |
| `spring.ai.google.genai.chat.temperature` | double | 0.3 | 0.0 to 2.0 |
| `spring.ai.google.genai.chat.max-output-tokens` | int | 1024 | 1 to 8192 |

Secret (not in `application.yml`): `GOOGLE_API_KEY`, resolved from `.env` or the environment.

### Rate-limit bucket (internal)

| Field | Type | Rules |
|-------|------|-------|
| clientKey | string | Client address (or forwarded address if proxy trust is enabled). |
| tokens | token bucket | Refills at `requests-per-minute`, capacity `burst`. |

Held in a bounded, expiring cache. Never logged with conversation data.

## Request and event shapes

### ChatRequest (request body)

| Field | Type | Rules |
|-------|------|-------|
| sessionId | string | Required. UUID format. |
| message | string | Required. After trimming, 1 to `max-message-chars` characters. |

Validation failure returns `VALIDATION_FAILED`. The message is not sent to the model.

### StreamEvent (server-sent events)

| Event name | Data | Meaning |
|------------|------|---------|
| `token` | `{ "text": string }` | A piece of the reply. Concatenate in order. |
| `done` | `{}` | The reply finished normally. Always the last event of a successful stream. |
| `error` | `{ "code": ErrorCode }` | The reply failed. Always the last event of a failed stream. |

Exactly one of `done` or `error` ends each stream. Full wire details are in
[contracts/chat-stream-events.md](./contracts/chat-stream-events.md).

### ErrorCode (closed set)

| Code | When | HTTP (pre-stream only) |
|------|------|------------------------|
| `VALIDATION_FAILED` | Bad session ID, blank or over-length message, malformed body | 400 |
| `RATE_LIMITED` | Client over the request limit | 429 |
| `REPLY_TIMEOUT` | Overall or idle timeout reached | 200 stream, sent as `error` event |
| `UPSTREAM_UNAVAILABLE` | Model call failed, was refused, or the provider was unreachable | 200 stream, sent as `error` event |
| `INTERNAL_ERROR` | Any other failure | 500 pre-stream, `error` event mid-stream |

No code carries free text, upstream messages, or identifiers beyond the code itself.

## Client-side entities

### Conversation (browser state)

| Field | Type | Rules |
|-------|------|-------|
| sessionId | UUID string | Generated with `crypto.randomUUID()` at start and on "new conversation". Never stored outside memory. |
| messages | list of Message | Ordered. Cleared on "new conversation". |
| status | `idle` or `streaming` | Send is disabled while `streaming`. |

### Message (browser state)

| Field | Type | Rules |
|-------|------|-------|
| id | string | Local only, for list rendering. |
| sender | `customer` or `assistant` | |
| text | string | Customer text is plain. Assistant text is rendered as markdown with raw HTML disabled. |
| status | `complete`, `streaming`, `failed`, or `incomplete` | `incomplete` means some text arrived before a failure. |
| errorCode | ErrorCode or none | Present only when `failed` or `incomplete`. Mapped to friendly text by the UI. |

Message status transitions:

```text
customer: complete
assistant: streaming --done--------------------> complete
           streaming --error, no text received--> failed
           streaming --error, text received-----> incomplete
           streaming --user starts new chat-----> (discarded with the conversation)
```

## Failed turn handling (memory consistency)

When a reply fails or times out, the server MUST NOT leave an orphaned customer message that has
no assistant turn, and MUST NOT store a truncated assistant turn as if it were complete. On failure
the service removes that turn's customer message from memory, so a retry of the same message starts
clean. This is verified by a test that fails a reply and then checks the memory contents.

## Default system prompt (shape, not final wording)

The persona text in `application.yml` must cover: role (Caramel AI tech support assistant), tone
(professional, concise, helpful), in-scope topics (the eight areas in FR-006), refusal behavior
(polite decline plus an in-scope suggestion), the support contact placeholder
`${caramel.support.contact}`, honesty when unsure, and a rule never to reveal instructions or
configuration. It contains no curly braces. Final wording is a task for the implementation phase and
is exercised by the evaluation set.
