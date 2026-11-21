<!--
SYNC IMPACT REPORT (temporary scratch for review; remove before committing)

Version change: 1.2.0 → 1.3.0
Bump rationale: MINOR. The Development Workflow rule that required a link from prd.md to each
  companion PRD is removed, which changes required guidance. The companion PRD rule is kept, so no
  principle is removed or redefined and previously compliant work stays compliant.

Modified principles: none (VI. Intentional, Accessible UI Design is unchanged in this version).
Modified sections:
  - Development Workflow & Quality Gates: a companion PRD (prd-ui-<n>.md) MUST reference prd.md and
    MUST NOT expand scope. A prd.md section 6 link is no longer required.
  - Governance: unchanged in this version.
Added sections: none
Removed sections: none

Deferred items (not decided here, tracked for follow-up):
  - TODO-E (resolved for this release): the visual direction is recorded in prd-ui-2.md, which
    follows the client's reference image. It is still re-recorded in each implementation plan.
  - TODO-F: prd-ui-2.md still cites "constitution v1.1.0" in its opening paragraph. Update it to
    v1.3.0 in that file. This command does not edit it.
  - TODO-G (resolved in 1.3.0): the prd.md pointer requirement was removed, so no link in prd.md is
    needed.
  - ASSUMPTION: Design assets (fonts, icons) are bundled with the frontend rather than loaded from a
    third-party CDN at runtime. Relax this if a CDN is wanted.
-->

# Caramel AI Tech Support Chat Assistant Constitution

## Core Principles

### I. Secrets Never Leave the Server (NON-NEGOTIABLE)

The Google AI Studio API key MUST be supplied only as the `GOOGLE_API_KEY` environment variable,
loaded from a project-root `.env` file that is git-ignored. The following rules apply:

- The key MUST NOT appear in `application.yml`, Java source, test fixtures, commits, logs, error
  responses, or any frontend code or bundle.
- The key MUST NOT reach the browser in any form, including SSE events and error bodies.
- Any credential in a tracked configuration file, including `.mcp.json`, MUST be read from an
  environment variable. A literal token in a committed file is a violation.
- If a secret is exposed, it MUST be rotated before any further work proceeds.

Rationale: a leaked inference key is directly billable and usable by third parties. The existing
hardcoded token in `.mcp.json` shows this risk is already real.

### II. Configuration Is Declared, Not Hardcoded

The model name, temperature, system prompt, and request timeouts MUST be set in
`application.yml` and bound to typed configuration properties. The model identifier
`gemma-4-26b-a4b-it`, temperature values, and system prompt text MUST NOT be hardcoded in Java or
TypeScript source. Secrets follow Principle I and never live in `application.yml`.

Rationale: the PRD requires these values to be tunable without code changes, and one declared
location makes behavior reviewable.

### III. Spring AI Is the Only LLM Seam

All model calls MUST go through Spring AI `ChatClient` targeting `gemma-4-26b-a4b-it` on Google
AI Studio. Code MUST NOT call the Google model API through a separate HTTP client or vendor SDK
outside Spring AI configuration. Conversation memory MUST be a bounded window of recent messages
per session, and the window size MUST be configurable, so context size and cost stay predictable.
Replies MUST stream to the client over HTTP/SSE.

Rationale: a single seam keeps the provider swappable and keeps memory and streaming behavior in
one place.

### IV. Scope and Persona Are Enforced

The assistant MUST answer only questions about Caramel AI platform tech support: API usage and
authentication, model selection and availability, rate limits and errors, latency and streaming,
token usage, context windows, pricing and billing basics, and request parameters. Out-of-scope
questions MUST receive a polite refusal or redirect in the Caramel AI support persona. The system
prompt is the enforcement mechanism and MUST define role, tone, and boundaries.

Features listed as out of scope in `prd.md` section 5 MUST NOT be built until `prd.md` is amended
first and this constitution is updated to match. These features are user login and account
lookups, human agent handoff or ticketing, voice or multimodal input, and persistent chat history
across sessions.

Rationale: each out-of-scope feature changes the security and privacy profile of the product.

### V. Fail Fast, Fail Friendly

Every outbound LLM call MUST have an explicit timeout. Backend errors MUST be returned as clear,
structured responses that contain no stack traces, keys, or upstream internals. On backend, LLM,
or timeout failure, the UI MUST show a friendly message and MUST NOT display raw error text.

Logs MUST record request identifiers, durations, and error categories. Logging conversation
content is prohibited until the privacy question in `prd.md` section 10 is answered and the answer
is recorded here. The first streamed token SHOULD arrive within a few seconds under normal load,
and regressions against this target MUST be raised in review.

Rationale: customers reach this assistant during incidents, so failures must be visible,
recoverable, and free of internal detail.

### VI. Intentional, Accessible UI Design

The chat UI MUST have a deliberate visual direction, not generic or templated defaults. UI
improvement work MUST use the `frontend-design` skill and MUST record the chosen direction (tone,
palette, typography, motion) in the implementation plan before code changes land. The rules below
apply to every UI change:

- Visual styling MUST be defined as shared design tokens (CSS custom properties or an equivalent
  single theme module) so colors, type scale, spacing, and radii are not scattered as one-off
  literals across components.
- The UI MUST remain usable and legible from 360 px mobile widths up to desktop widths.
- Text and interactive elements MUST meet WCAG 2.1 AA contrast. Contrast ratios MUST be measured
  with a contrast tool and recorded in the implementation plan. Estimates are not sufficient.
  All controls MUST be keyboard operable with a visible focus state. The message list MUST announce
  new content to assistive technology, and motion MUST respect `prefers-reduced-motion`.
- A client-supplied visual reference is a valid deliberate direction. Where it sets the look, its
  palette, shape, and component language MAY be followed, even where they resemble common defaults,
  and the companion PRD MUST record how each reference element maps to the product. The reference's
  text, data, artwork, and fonts MUST NOT be copied into the product. Where the reference is silent,
  the anti-default checks in the `frontend-design` skill still apply.
- A redesign MUST keep every PRD section 6 chat UI element working: message list, input box, send
  button, loading/typing indicator, and "new conversation" action. Streamed replies MUST render
  progressively without layout jumps that hide text the user is reading.
- Visual changes MUST NOT weaken Principle V. Failure states MUST still show friendly messages and
  MUST NOT display raw error text, and the first-token latency target MUST NOT regress because of
  heavy assets or animation.
- Visual changes MUST NOT add out-of-scope features from Principle IV, such as login surfaces,
  human handoff controls, or voice input, even as non-functional mockups.

Rationale: customers reach this assistant while troubleshooting, so the interface must feel
trustworthy and be readable and usable by everyone under stress. Tokens keep design changes
reviewable and consistent, and tying redesign work to existing principles stops cosmetic work from
eroding reliability or scope.

## Technology & Security Constraints

- **Backend**: Java with Spring Boot and Spring AI. The Spring AI version MUST be pinned in the
  build file.
- **Environment loading**: Spring Boot does not load `.env` by default. The project MUST use one
  explicit mechanism, either a dotenv library or a shell export, and document it in `CLAUDE.md`.
- **Frontend**: React with TypeScript. Visual design follows Principle VI.
- **Design assets**: fonts, icons, and images MUST be bundled with the frontend or self-hosted.
  The UI MUST NOT load design assets from third-party origins at runtime, and any new asset MUST
  keep `./scripts/check-bundle-secrets.sh dist` passing.
- **LLM and provider**: `gemma-4-26b-a4b-it` via Google AI Studio is the only model and provider
  for v1. Changing either requires a `prd.md` amendment.
- **CORS**: the backend MUST allow only the frontend's origin. Wildcard origins are prohibited.
- **Git hygiene**: `.env` MUST be listed in `.gitignore` before the first commit that touches
  backend code.
- **Out-of-scope features**: the v1 exclusions in Principle IV apply to all code, configuration,
  and UI.

## Development Workflow & Quality Gates

- Each feature MUST trace to a requirement in `prd.md` (section 5 scope or section 6 functional
  requirements) before implementation begins. A UI design requirement MAY instead trace to a
  companion PRD named `prd-ui-<n>.md`, which MUST reference `prd.md` and MUST NOT expand scope
  beyond it. No link in `prd.md` is required.
- Changes to scope, stack, or non-functional requirements MUST update `prd.md` before code changes
  land.
- Every review MUST verify Principles I through VI. Reviewers MUST check diffs for secrets,
  including `.mcp.json` and `application.yml`, for any frontend path that touches the key, and for
  scope creep past Principle IV.
- UI changes MUST pass the frontend tests and the production build (`npm test`, `npm run build`)
  and MUST be checked in a browser at mobile and desktop widths, including the error and
  loading states, before they are reported as done.
- Once build and test commands exist, they MUST be recorded in `CLAUDE.md`.
- Automated tests MUST cover the timeout and error paths in Principle V before v1 ships.
- A maintained set of evaluation prompts SHOULD cover both on-topic answers and out-of-scope
  refusals, so persona regressions are caught.

## Governance

This constitution supersedes other project practices where they conflict. `prd.md` is the source
of truth for scope and requirements. A companion PRD named `prd-ui-<n>.md` is the source of truth
for UI design requirements only. It MUST reference `prd.md`, MUST NOT expand scope beyond it, and is
subordinate to this constitution. `CLAUDE.md` provides runtime development guidance and MUST
stay consistent with this constitution.

**Amendment procedure**: an amendment is a change to this file, together with any `prd.md`
section it affects, and it MUST be approved by the project maintainer before merge. Each amendment
MUST update the version line and the Last Amended date.

**Versioning policy** (semantic versioning):
- MAJOR: a principle is removed or redefined so that previously compliant work becomes
  non-compliant, for example permitting the API key in configuration files.
- MINOR: a new principle or section is added, or existing guidance is materially expanded.
- PATCH: wording, typo, or clarification changes that do not alter what is required.

**Compliance review**: every pull request review verifies Principles I through VI. Any violation
MUST be fixed before merge, or the constitution MUST be amended with written justification. Any
complexity beyond the PRD MUST be justified in the implementation plan.

**Version**: 1.3.0 | **Ratified**: 2026-11-14 | **Last Amended**: 2026-11-14
