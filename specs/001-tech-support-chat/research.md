# Phase 0 Research: Caramel AI Tech Support Chat Assistant

**Date**: 2026-11-14 | **Plan**: [plan.md](./plan.md)

Each entry gives the decision, the rationale, and the alternatives considered. Items marked
**Unverified** could not be confirmed here (no `GOOGLE_API_KEY` is available in this environment)
and have an explicit verification step.

## R1. Model availability and system-instruction support (**Unverified, highest risk**)

- **Decision**: Use `gemma-4-26b-a4b-it` through `spring-ai-starter-model-google-genai` with the
  Gemini Developer API (`spring.ai.google.genai.api-key`). The first implementation task is a spike
  that makes one live call and answers two questions: (a) does this model ID resolve for the key,
  and (b) does it accept a system instruction.
- **Rationale**: The PRD fixes the model. Open-weight Gemma models served through the Gemini API
  have, in earlier generations, rejected a separate system instruction. If this model does, the
  persona and scope boundaries (Principle IV, FR-007, FR-008) cannot ride on the system role.
- **Fallback if (b) fails**: Keep the same configured prompt text, but deliver it as a leading user
  turn in the prompt instead of a system message. The change stays inside `ChatClientConfig`. The
  prompt remains in `application.yml`, so Principle II still holds. Scope enforcement is then weaker
  against prompt injection, and the evaluation set must be re-run.
- **Fallback if (a) fails**: Stop and raise it. Changing the model needs a `prd.md` amendment
  (constitution, Technology & Security Constraints).
- **Alternatives considered**: Calling the Google API directly (rejected: Principle III). Using a
  Gemini model instead (rejected: PRD fixes the model).

## R2. Streaming stack: WebFlux

- **Decision**: Use `spring-boot-starter-webflux` and return `Flux<ServerSentEvent<...>>`.
- **Rationale**: Spring AI's `ChatClient.stream()` returns a `Flux`, and its reference states that
  streaming requires the reactive stack while non-streaming requires the Servlet stack. One stack
  keeps the app consistent. Handlers must never call `.block()` on the stream.
- **Alternatives considered**: Spring MVC with `SseEmitter` and a bridging subscriber (rejected:
  extra threading code and a mixed stack); `call()` without streaming (rejected: violates FR-002 and
  Principle III).

## R3. Request transport: POST with a `fetch` stream, not `EventSource`

- **Decision**: `POST /api/chat/stream` with a JSON body; the browser reads the response body with
  `fetch` and a small SSE parser.
- **Rationale**: Browser `EventSource` supports only GET. A GET would put customer text in the URL,
  where proxies and access logs record it, which conflicts with FR-020. POST keeps content in the
  body. A fetch stream also lets the UI abort a request.
- **Alternatives considered**: GET with `EventSource` (rejected: content in URLs); WebSocket
  (rejected: more infrastructure than a one-way stream needs); `@microsoft/fetch-event-source`
  (rejected: the SSE framing needed here is small, and a local parser with unit tests avoids a
  dependency).

## R4. Timeouts and cancellation (**Partly unverified**)

- **Decision**: Two configurable limits in `caramel.limits`: overall reply time (default 30 s,
  from the spec's assumption) and idle time between streamed chunks (default 15 s). Apply Reactor
  `timeout` for idle and `takeUntilOther` for the overall limit. Both map to error code
  `REPLY_TIMEOUT`.
- **Rationale**: The overall limit satisfies FR-012. The idle limit catches stalled upstream
  streams earlier than 30 s and satisfies Principle V's "explicit timeout on every outbound call".
- **Unverified**: The Google GenAI Java SDK that Spring AI wraps may run the stream on a blocking
  thread. Reactor cancellation then stops delivery to the client but may not abort the upstream
  call immediately. The spike (R1) also checks whether the SDK exposes an HTTP timeout property; if
  it does, set it slightly above the overall limit as a backstop.
- **Alternatives considered**: Single overall timeout only (rejected: slow to detect a stall).

## R5. Conversation memory and expiry

- **Decision**: `MessageWindowChatMemory` with a configurable window (default 20 messages, Spring
  AI's own default), backed by a custom `ExpiringChatMemoryRepository` built on a Caffeine cache
  with `expireAfterAccess` (default 30 min) and a maximum size (default 10,000 sessions).
- **Rationale**: The stock in-memory repository never evicts, so abandoned sessions would grow
  without bound. That is both a memory leak and an abuse vector. Caffeine gives TTL and a size cap
  in a few lines. It satisfies FR-004, FR-005, FR-015, and Principle III's bounded-memory rule.
- **Consequence**: Memory is per instance. Running more than one instance would split sessions, so
  v1 is explicitly single-instance (recorded in Technical Context).
- **Alternatives considered**: Stock in-memory repository (rejected: unbounded); JDBC/Redis
  repository (rejected: persistence is out of scope, Principle IV); no server memory with the client
  resending history (rejected: PRD 6.3 requires server-side memory, and it trusts client-supplied
  history).

## R6. Session identity

- **Decision**: The browser generates a random UUID per conversation and keeps it in component
  state only (not `localStorage`). The server validates UUID format. "New conversation" generates a
  new UUID and calls `DELETE /api/chat/sessions/{id}` to free the old memory.
- **Rationale**: A reload loses React state, so it starts a fresh conversation, which is exactly
  FR-015. Server-side validation rejects arbitrary strings used as cache keys.
- **Alternatives considered**: Server-issued session cookies (rejected: adds cookies, CORS
  credentials, and state with no benefit for anonymous use); `localStorage` ID (rejected: would
  make conversations survive reloads, contradicting FR-015).

## R7. Abuse protection (provisional for FR-013)

- **Decision**: A `WebFilter` applies a per-client token bucket (Bucket4j) to
  `POST /api/chat/stream`, defaulting to 10 requests per minute per client address, with a burst of
  5. A request over the limit gets HTTP 429 with error code `RATE_LIMITED` and a `Retry-After`
  header. Bucket state lives in a bounded Caffeine cache. The message length cap is 2,000
  characters.
- **Rationale**: Anonymous use on a shared billable key needs a cost ceiling. This needs no accounts
  (out of scope). The client address comes from the connection by default. Forwarded headers are
  trusted only if a property enables them for a known proxy.
- **Limitation**: Per-address limits are weak against distributed abuse and can throttle many users
  behind one NAT. This is accepted for v1.
- **Alternatives considered**: No limit (spec option B, rejected as the default: unbounded cost);
  API gateway limiting (deferred: no gateway exists yet); verified-customer access (spec option C,
  needs a `prd.md` amendment).

## R8. Escalation text (provisional for FR-018)

- **Decision**: Put a static support contact in `caramel.support.contact`. The system prompt
  references it with a Spring property placeholder, so it resolves when the YAML binds. The prompt
  tells the assistant to give that contact when it cannot resolve an in-scope question or when asked
  for a human. The value is currently `TODO-SUPPORT-CONTACT` and must be supplied before release.
- **Rationale**: This keeps FR-018's "no handoff, no ticket" while giving the customer a next step.
  The contact lives in configuration (Principle II) and does not need a code change to update.
- **Gotcha**: `ChatClient` treats the default system text as a template, so curly braces in the
  prompt can be misread as variables. The prompt must contain no `{` or `}`. A unit test enforces it.
- **Alternatives considered**: A separate template variable (rejected: needless indirection);
  contact capture form (rejected: counts as handoff, out of scope).

## R9. Configuration and `.env` loading

- **Decision**: In `application.yml`, use
  `spring.config.import: optional:file:.env[.properties],optional:file:../.env[.properties]` and
  `spring.ai.google.genai.api-key: ${GOOGLE_API_KEY}`. Provide a committed `.env.example` with a
  placeholder and a git-ignored `.env`.
- **Rationale**: One built-in mechanism (no extra library) resolves the key whether the backend is
  started from the repository root or from `backend/`. It meets the constitution's "one explicit
  mechanism" rule. The mechanism is to be documented in `CLAUDE.md` during implementation.
- **Startup behavior**: If `GOOGLE_API_KEY` is missing the app MUST fail at startup with a message
  that names the variable but never prints any value.
- **Alternatives considered**: A dotenv library (rejected: extra dependency for the same effect);
  shell export only (rejected: easy to forget and harder to document for non-shell users).

## R10. CORS

- **Decision**: One exact allowed origin from `caramel.cors.allowed-origin` (dev default
  `http://localhost:5173`). Allowed methods `POST`, `DELETE`, `OPTIONS`. Allowed header
  `Content-Type`. No credentials. No wildcard.
- **Rationale**: Constitution requires only the frontend origin. No cookies are used, so credentials
  are unnecessary.
- **Alternatives considered**: Vite dev proxy to avoid CORS in development (rejected as the only
  mechanism: it would leave CORS untested until deployment).

## R11. Error model and the friendly-message boundary

- **Decision**: The backend never sends human-readable error prose to the UI. It sends a fixed
  error code (`VALIDATION_FAILED`, `RATE_LIMITED`, `REPLY_TIMEOUT`, `UPSTREAM_UNAVAILABLE`,
  `INTERNAL_ERROR`). Errors before streaming starts use HTTP status plus an RFC 9457 problem
  body. Errors after streaming starts use an SSE `error` event. The UI owns all customer-facing text
  in `friendlyMessages.ts`.
- **Rationale**: Mapping upstream exceptions to a closed set of codes prevents stack traces, keys,
  and provider internals from reaching the browser (Principle I, V; FR-011, FR-017). The UI cannot
  display raw error text because it never receives any.
- **Test**: Force an upstream exception whose message contains a fake key and assert the string is
  absent from the response and from logs.
- **Alternatives considered**: Forwarding provider messages (rejected: leak risk).

## R12. Logging without conversation content

- **Decision**: Log a request ID, session ID prefix, duration, outcome, and error code. Do not
  register `SimpleLoggerAdvisor` and keep `org.springframework.ai` and the Google SDK loggers at
  `WARN` or above. Log nothing from request or response bodies.
- **Rationale**: FR-020 and Principle V. Spring AI's logger advisor writes prompts, so it must stay
  off. A test scans captured logs after a conversation for the user text.
- **Alternatives considered**: Hashing content in logs (rejected: still content-derived and
  unneeded).

## R13. Frontend rendering of replies

- **Decision**: Render assistant text with `react-markdown` with raw HTML disabled. Render customer
  text as plain text.
- **Rationale**: Tech support answers include code samples and lists. Model output is untrusted, so
  HTML injection must be impossible. The default `react-markdown` configuration does not render raw
  HTML.
- **Alternatives considered**: Plain text only (rejected: poor readability for code); rendering HTML
  from the model (rejected: XSS risk).

## R14. Frontend behavior details

- **Decision**: Disable send while a reply streams (resolves the "message while streaming" edge
  case). Trim input and block empty or over-length messages client-side with an inline note, and
  re-validate on the server. A failed reply keeps the partial text marked "incomplete" and offers
  retry of the same message. Abort the in-flight request when starting a new conversation.
- **Rationale**: Directly implements FR-003, FR-012, FR-014, FR-019 and the edge cases in the spec.
- **Alternatives considered**: Queueing messages during a stream (rejected: unneeded complexity).

## R15. Test and evaluation strategy

- **Decision**:
  - Backend unit and web tests with the model seam mocked: validation, memory windowing and expiry,
    rate limiting, timeout and error mapping, CORS allow and deny, prompt has no curly braces, no
    secret or content in responses and logs.
  - Frontend tests: SSE parser, conversation hook (send, stream, fail, retry, reset), and
    component behavior.
  - A live evaluation suite under `backend/src/test/resources/eval/`, run only when
    `GOOGLE_API_KEY` is set. It covers in-scope answers, out-of-scope redirects, instruction-disclosure
    attempts, and multi-turn follow-ups, using keyword assertions plus a human-reviewed report.
- **Rationale**: The constitution requires tests for timeout and error paths and recommends an
  evaluation prompt set. SC-003, SC-004, and SC-007 are model-behavior outcomes, so they need live
  evaluation. They cannot be proven by mocks.
- **Limitation**: Keyword assertions are an approximation of "on topic". The report is reviewed by a
  person before v1 sign-off.

## R16. "Normal load" definition for SC-001

- **Decision**: Define normal load as up to 20 concurrent active conversations on one instance.
  Measure time to first streamed chunk in the quickstart.
- **Rationale**: The spec left "normal load" undefined. Twenty concurrent chats is a reasonable
  v1 support-desk assumption and is checkable. Adjust if real traffic data exists.

## Resolution summary

All Technical Context items are resolved except two **Unverified** items (R1 model and
system-instruction support, R4 SDK timeout support). Both are covered by a first-task spike, and
each has a documented fallback or stop condition. No design change is expected unless R1 fails.

## Spike results (executed 2026-11-14, T013)

Live run of `GemmaSpikeTest` and a short probe set against `gemma-4-26b-a4b-it` on the project key.

- **(a) Model ID resolves:** yes. The model replies.
- **(b) System instruction honored:** **no.** With a system message asking for the single word "PONG", the reply narrated the user's question ("The user is asking ...") instead of following the instruction. The R1 fallback (leading user turn) was also probed and showed the same problem, so it was not adopted. The system role is kept.
- **(c) Time to first chunk:** about 5.7 s on the first call.
- **Additional blocking finding:** the model writes its planning notes ("Plan:", "Constraint 1:", "Draft:") into the reply text in every variant tested: user-only, system role, user-turn persona, and answer-tag wrapper. A customer-facing reply of about 2,600 characters contained such notes before the answer. Wrapping the reply in `<answer>` tags did not stop it, because the notes mention the tags. Replies took 11 to 23 s when not streamed.
- **Thinking controls:** `includeThoughts=false` and `thinkingBudget=0` are both rejected for this model with "Failed to generate content".
- **Alternative model:** `gemini-2.5-flash` is retired for new users on this key ("no longer available to new users").

Decision needed: the model choice. Options are a different hosted model (a PRD amendment) or accepting visible reasoning. Stripping it reliably is not feasible. See validation-report.md.
