# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Feature 001 (tech support chat) is implemented in `backend/` (Spring Boot 4 / Spring AI 2.0.2) and
`frontend/` (React + TypeScript + Vite). Spec artifacts are in `specs/001-tech-support-chat/`.
Check `specs/001-tech-support-chat/tasks.md` and `validation-report.md` for the current state.

## Source of truth

`prd.md` defines the scope, requirements, and architecture. `.specify/memory/constitution.md` holds the
governing principles. Read both before making design decisions.

## Commands

Run from the repository root unless noted.

| Task | Command |
|------|---------|
| Backend unit and web tests (no provider calls) | `cd backend && mvn test` |
| Backend live evaluation and latency (calls the model, needs `GOOGLE_API_KEY`) | `cd backend && mvn -Plive-eval test` |
| Run backend | `cd backend && mvn spring-boot:run` (serves `http://localhost:8080`) |
| Frontend tests | `cd frontend && npm test` |
| Frontend type check and production build | `cd frontend && npm run build` |
| Frontend dev server | `cd frontend && npm run dev` (serves `http://localhost:5173`) |
| Check the built bundle has no secrets | `cd frontend && ./scripts/check-bundle-secrets.sh dist` |

Live tests are tagged `live` and are excluded from `mvn test`. They are skipped unless
`GOOGLE_API_KEY` is set. The live evaluation writes `backend/target/eval-report.md` for human review.

## Configuration and secrets

- Model name, temperature, token limits, persona, memory window, timeouts, rate limits, and the
  allowed CORS origin live in `backend/src/main/resources/application.yml` (the `caramel.*` block
  and `spring.ai.google.genai.*`).
- The Google API key is `GOOGLE_API_KEY`. Put it in `.env` at the repository root. `.env` is
  git-ignored. Copy `.env.example` to create it. Never put the key in `application.yml`, source
  code, tests, or frontend code.
- Spring Boot does not read `.env` by default. `application.yml` loads it explicitly with
  `spring.config.import: optional:file:.env[.properties],optional:file:../.env[.properties]`, so the
  backend works from the repository root or from `backend/`. The backend refuses to start without the
  key.
- Frontend API base URL: `VITE_API_BASE_URL` (default `http://localhost:8080`).

## Known issues

- Live model behavior: `gemma-4-26b-a4b-it` writes its planning notes into the reply text, and the
  system instruction is not reliably honored. Thinking controls are rejected by this model. See
  `specs/001-tech-support-chat/research.md` R1 for the spike results. The model choice is an open
  decision.
- `.mcp.json` reads the GitHub token from `${GITHUB_TOKEN}`. Do not put a literal token in it.

## Out of scope (v1)

User authentication, human agent handoff, persistent chat history, and voice or multimodal input.
Don't add these without updating `prd.md` first.
