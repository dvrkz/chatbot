# Contract: Chat Stream Events

**Endpoint**: `POST /api/chat/stream` | **Schema**: [chat-api.openapi.yaml](./chat-api.openapi.yaml)

The response is `Content-Type: text/event-stream`. Each event is a standard SSE block: an
`event:` line, a `data:` line holding one line of JSON, and a blank line.

## Events

| Event | Data | Count | Meaning |
|-------|------|-------|---------|
| `token` | `{"text": "<string>"}` | 0 or more | A piece of the reply. Append in arrival order. `text` is never empty. |
| `done` | `{}` | 0 or 1 | The reply finished normally. |
| `error` | `{"code": "<ErrorCode>"}` | 0 or 1 | The reply failed. Any earlier `token` text is incomplete. |

## Guarantees

1. Exactly one of `done` or `error` is the **last** event on every stream the server completes.
2. After `done` or `error`, the server closes the stream. No further events follow.
3. `error.code` is one of the values in the `ErrorCode` enum. It never carries free text, upstream
   messages, stack traces, or credentials.
4. A stream can contain `token` events and then `error` (partial reply). The client keeps the
   partial text, marks the message incomplete, and offers retry.
5. If the connection drops with no `done` or `error`, the client MUST treat the reply as failed
   (`UPSTREAM_UNAVAILABLE` for display purposes) and mark any partial text incomplete.
6. The server sends no event whose data contains customer message text. Only reply text is sent.

## Timing

- The first `token` should arrive within about 3 seconds under normal load (SC-001).
- `REPLY_TIMEOUT` is emitted when no chunk arrives within `caramel.limits.idle-timeout` (default
  15 s) or the reply exceeds `caramel.limits.reply-timeout` (default 30 s).

## Parsing rules for the client

- Split the byte stream on blank lines. Decode as UTF-8, and handle a multi-byte character split
  across network chunks.
- Read `event:` and `data:` fields. Ignore lines starting with `:` (comments) and unknown events.
- Parse `data` as JSON. If parsing fails, treat the stream as failed.
- Do not rely on `id:` or `retry:` fields. The server does not send them and the client does not
  auto-reconnect, because a retry would resend a customer message.

## Example: failure with partial text

```text
event: token
data: {"text":"You are seeing a 429 because "}

event: error
data: {"code":"UPSTREAM_UNAVAILABLE"}

```

Client result: message text "You are seeing a 429 because ", status `incomplete`, friendly message
for `UPSTREAM_UNAVAILABLE`, retry offered.

## Pre-stream errors (not events)

Validation and rate-limit failures happen before any event is sent. They return an HTTP error with
`application/problem+json` and a `code` field from the same `ErrorCode` set. The client maps the
code to the same friendly text it uses for stream errors.
