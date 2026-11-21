# Implementation Plan: Caramel AI Tech Support Chat Assistant

**Branch**: `001-tech-support-chat` | **Date**: 2026-11-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-tech-support-chat/spec.md`

## Summary

Build a web chat assistant that answers Caramel AI platform tech support questions, with streamed
replies, in-session memory, scope and persona enforcement, and friendly failure handling.

Approach: a Spring Boot 4 / Spring AI 2 backend exposes one streaming endpoint (`POST
/api/chat/stream`, Server-Sent Events) and one session-clear endpoint. It calls
`gemma-4-26b-a4b-it` on Google AI Studio through a single `ChatClient`. A windowed, TTL-expiring
in-memory chat memory is keyed by a client-generated session ID. A React + TypeScript (Vite)
frontend reads the stream with `fetch`, renders replies, and maps backend error codes to friendly
messages. Persona, scope, model, temperature, window size, timeouts, and limits all live in
`application.yml`. The API key lives only in the git-ignored `.env`.

### Provisional decisions on open spec questions

`spec.md` still contains two `[NEEDS CLARIFICATION]` markers (FR-013, FR-018). They were not
answered, so this plan adopts the recommended option for each. Both are isolated and cheap to change.

| Spec item | Provisional decision | Where it lands | If the answer differs |
|-----------|----------------------|----------------|-----------------------|
| FR-013 abuse protection | Per-client rate limit plus a message-length cap | `RateLimitFilter`, `caramel.limits.*` | Option B: delete the filter. Option C: needs a `prd.md` amendment, then re-plan. |
| FR-018 unresolved-question escalation | Static support contact shown in the assistant's reply | `caramel.support.contact` in `application.yml`, referenced by the system prompt | Option B: change the prompt text only. Option C: out of scope, needs a `prd.md` amendment. |

The support contact value is **not known**. It is a placeholder (`TODO-SUPPORT-CONTACT`) that must be
replaced before release. Update `spec.md` FR-013 and FR-018 to match once confirmed.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x on Node 22 (frontend). Both are installed
locally (OpenJDK 21.0.12, Node 22.22.2).

**Primary Dependencies**:
- Backend: Spring Boot 4.0.9, Spring AI 2.0.2 (BOM-managed, pinned), `spring-ai-starter-model-google-genai`,
  `spring-boot-starter-webflux`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`
  (health only), Caffeine 3.3.1 (session TTL), Bucket4j 8.22.0 (rate limiting).
- Frontend: React 19, Vite, `react-markdown` (no raw HTML), Vitest, React Testing Library.

**Storage**: None persistent. Conversation memory is in-process and expires. This satisfies FR-015.

**Testing**: JUnit 5, Reactor `StepVerifier`, `WebTestClient`, Mockito for the model seam (backend).
Vitest + React Testing Library for the UI. A separate live evaluation suite, gated on
`GOOGLE_API_KEY`, runs the prompt set.

**Target Platform**: Linux server (single JVM instance); evergreen desktop browsers.

**Project Type**: Web application (backend service plus single-page frontend).

**Performance Goals**: 90% of messages show the first streamed text within 3 s under normal load
(SC-001). Normal load is defined in research as up to 20 concurrent conversations on one instance.

**Constraints**:
- Reply timeout 30 s overall, 15 s idle between chunks. Both configurable.
- Message length cap 2,000 characters. Memory window 20 messages. Idle session TTL 30 min.
- No conversation content in logs (FR-020). No key in any response or frontend asset (FR-017).
- Single instance only. In-memory memory and rate-limit state are not shared across instances.

**Scale/Scope**: One instance, up to 10,000 live sessions (cache cap), 5 user stories, 20 functional
requirements, 2 endpoints, 1 page.

**Unknowns carried into implementation (not blockers for design)**:
1. Whether `gemma-4-26b-a4b-it` is served by the Gemini Developer API for the project key and
   accepts a system instruction. See research R1. A spike task is first in implementation.
2. Whether the Google GenAI SDK used by Spring AI exposes an HTTP timeout property. See research R4.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Requirement (constitution) | Pre-research | Post-design |
|------|----------------------------|--------------|-------------|
| I. Secrets | Key only via `GOOGLE_API_KEY` from git-ignored `.env`; never in config, source, logs, errors, or frontend | PASS (planned) | PASS: key is read through `${GOOGLE_API_KEY}`; errors map to codes only; leak test planned (T-sec) |
| II. Configuration | Model, temperature, prompt, timeouts in `application.yml` via typed properties | PASS (planned) | PASS: `ChatProperties` binds all of them; no literals in code |
| III. Spring AI seam | Only `ChatClient`; bounded configurable memory; SSE streaming | PASS (planned) | PASS: single `ChatClient` bean; `MessageWindowChatMemory`; SSE only |
| IV. Scope and persona | In-scope topics only; polite redirect; out-of-scope features not built | PASS | PASS: no login, handoff, multimodal, or persistence in design |
| V. Fail fast, fail friendly | Explicit timeouts; structured errors; friendly UI; no content logging | PASS (planned) | PASS: overall and idle timeouts; error codes; UI owns all user-facing text |
| Tech constraints | Spring AI pinned; one `.env` loading mechanism; only allowed CORS origin; `.env` ignored before first backend commit | PASS (planned) | PASS: `spring.config.import`; exact-origin CORS; `.gitignore` is the first task |
| Workflow gates | Traceable to `prd.md`; review checks I-V; tests cover timeout and error paths; evaluation prompts | PASS | PASS: traceability table below; evaluation set planned |

**Pre-existing violation (outside this feature's code, must be fixed before the first commit):**
`.mcp.json` holds a literal GitHub token in an `Authorization` header, which breaks Principle I's
rule on credentials in tracked files. The token also reported as expired. Before any commit, move it
to an environment variable reference and rotate it. This is not introduced by the plan and needs no
Complexity Tracking entry, but it blocks the first commit. There is currently no root `.gitignore`
and the directory is not a git repository yet.

### Traceability to `prd.md`

| PRD requirement | Plan element |
|-----------------|--------------|
| 6.1 Chat endpoint (REST, streaming, message + session ID) | `POST /api/chat/stream`, `ChatController` |
| 6.2 LLM integration via `ChatClient` and `.env` key | `ChatClientConfig`, `spring.config.import` |
| 6.3 Conversation memory | `ExpiringChatMemoryRepository` + `MessageWindowChatMemory` |
| 6.4 Configurable system prompt | `caramel.assistant.system-prompt` |
| 6.5 Chat UI | `frontend/src/components/*` |
| 6.6 Error handling | `ErrorCode` + `ApiExceptionHandler` + UI error mapping |
| 7 Security, Performance, Reliability, Configurability, CORS | Principles I, II, V; CORS config; timeouts |

## Project Structure

### Documentation (this feature)

```text
specs/001-tech-support-chat/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   ├── chat-api.openapi.yaml
│   └── chat-stream-events.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
.env                      # git-ignored; GOOGLE_API_KEY (never committed)
.env.example              # committed; placeholder values only
.gitignore                # created first; ignores .env, target/, node_modules/, dist/

backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/caramelai/support/
│   │   │   ├── SupportApplication.java
│   │   │   ├── config/
│   │   │   │   ├── ChatProperties.java         # typed binding for caramel.*
│   │   │   │   ├── ChatClientConfig.java       # ChatClient + memory advisor
│   │   │   │   └── CorsConfig.java             # single allowed origin
│   │   │   ├── chat/
│   │   │   │   ├── ChatController.java         # POST stream, DELETE session
│   │   │   │   ├── ChatService.java            # streaming, timeouts, event mapping
│   │   │   │   └── dto/                        # ChatRequest, StreamEvent
│   │   │   ├── memory/
│   │   │   │   └── ExpiringChatMemoryRepository.java
│   │   │   ├── limits/
│   │   │   │   └── RateLimitFilter.java
│   │   │   └── error/
│   │   │       ├── ErrorCode.java
│   │   │       └── ApiExceptionHandler.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       ├── java/com/caramelai/support/         # unit, web, and error-path tests
│       └── resources/eval/                     # evaluation prompt set (live, gated)

frontend/
├── package.json
├── vite.config.ts
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── components/
│   │   ├── MessageList.tsx
│   │   ├── MessageInput.tsx
│   │   ├── TypingIndicator.tsx
│   │   └── ErrorBanner.tsx
│   ├── services/
│   │   ├── chatApi.ts                          # fetch + SSE reader
│   │   └── sseParser.ts
│   ├── hooks/useConversation.ts                # session ID, messages, retry, reset
│   └── errors/friendlyMessages.ts              # error code to user text
└── tests/
```

**Structure Decision**: Web application layout with sibling `backend/` and `frontend/` projects,
matching the PRD's two-tier architecture. The chosen layout keeps secrets handling, CORS, and
build tooling separate per tier. The `.env` file stays at the repository root as the PRD requires,
and the backend imports it from both its own directory and the parent.

## Complexity Tracking

No constitution violations are introduced by this plan, so there is nothing to justify.
