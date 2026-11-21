# Implementation Plan: Chat Interface Visual and Interaction Refresh

**Branch**: `002-update-frontend` (a folder name only; the repository is not under git) | **Date**: 2026-11-14 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-update-frontend/spec.md`. Design source:
[prd-ui-2.md](../../prd-ui-2.md). Governing rules: [constitution v1.2.0](../../.specify/memory/constitution.md).

**Written for**: engineers implementing the frontend, and reviewers checking it against the spec.

## Summary

Restyle and refine the existing React chat UI to follow the client reference (lavender page, violet
customer bubbles, white assistant cards, pill controls, coral header action and error states), and fix
the interaction gaps the unstyled UI hid: scroll-follow during streaming, a composer that stays editable
while a reply streams, a copy action for code, and mobile layout.

The change is **frontend-only**. The backend, the streaming API, the error codes, and the conversation
hook keep their current behavior. The conversation hook already aborts an in-flight reply on reset and
already blocks a second send while streaming, so those rules carry over unchanged.

Three findings from the measured contrast audit (research R1) change the design in `prd-ui-2.md`:

1. **Outlined chip borders in `--lilac-line` fail WCAG (1.17:1).** Outlined controls use `--violet`
   (5.02:1). `--lilac-line` is removed from the palette.
2. **Coral on the error tint measures 5.53:1**, not the 5.8:1 estimated in the PRD. It still passes.
3. **The measured values replace every estimate in `prd-ui-2.md` section 5.2**, as the constitution
   requires.

These corrections are recorded here and must be carried into `prd-ui-2.md` (see Gate G-5).

## Design Direction (recorded per constitution Principle VI)

- **Tone**: friendly and light, with exact copy and code. A calm support desk, not a consumer chat app.
- **Palette**: the tokens below, with measured contrast. Violet is the speaker and action color. Coral
  means "attention" or "start over" and is used only for the header action and error states.
- **Typography**: Figtree for interface and reply text, JetBrains Mono for code. Scale 14 / 17 / 20 / 24 px,
  base 17 px, ratio 1.2.
- **Motion**: two motions only: the streaming progress bar and the Copy confirmation. Both are static
  under reduced motion.
- **Reference mapping**: `prd-ui-2.md` section 5.1 maps each reference element to the product. Nothing is
  copied from the reference's content, data, artwork, or fonts.

### Palette with measured contrast (WCAG 2.1 relative luminance, computed 2026-11-14)

| Token | Hex | Role | Pair checked | Ratio | Needed | Result |
|---|---|---|---|---|---|---|
| `--mist` | `#ECEBF6` | Page background | (surface) | n/a | n/a | n/a |
| `--paper` | `#FFFFFF` | Assistant cards, composer | (surface) | n/a | n/a | n/a |
| `--ink` | `#1F1B33` | Reply and body text | on `--paper` | 16.63 | 4.5 | PASS |
| `--ink` | `#1F1B33` | Code text | on `--mist` | 14.08 | 4.5 | PASS |
| `--violet` | `#6A3DF0` | Customer fill, Send, links, focus, outlined chips | on `--mist` | 5.02 | 4.5 (text) / 3.0 (boundary) | PASS |
| `--violet` | `#6A3DF0` | Links in cards | on `--paper` | 5.93 | 4.5 | PASS |
| white | `#FFFFFF` | Customer message text | on `--violet` | 5.93 | 4.5 | PASS |
| `--violet-deep` | `#4F26C9` | Pressed and hover fill | white text on it | 8.56 | 4.5 | PASS |
| `--slate` | `#5F5A7A` | Secondary text, placeholder, counter | on `--mist` | 5.50 | 4.5 | PASS |
| `--slate` | `#5F5A7A` | Placeholder, counter | on `--paper` | 6.50 | 4.5 | PASS |
| `--coral` | `#B42340` | Error text | on `--paper` | 6.45 | 4.5 | PASS |
| `--coral` | `#B42340` | Error text | on `--coral-tint` | 5.53 | 4.5 | PASS |
| `--coral` | `#B42340` | New conversation fill | white label on it | 6.45 | 4.5 | PASS |
| `--coral` | `#B42340` | Try again outline | on `--coral-tint` | 5.53 | 3.0 (boundary) | PASS |
| `--violet` | `#6A3DF0` | Focus ring | on `--paper` | 5.93 | 3.0 | PASS |
| `--lilac-line` | `#DCD9EE` | (removed) | on `--mist` | 1.17 | 3.0 | **FAIL, removed** |

## Technical Context

**Language/Version**: TypeScript 5.6 (`tsc -b`), React 19, Node 22 (`engines.node >= 22`).

**Primary Dependencies**:
- Existing: `react`, `react-dom`, `react-markdown` ^9 (no raw HTML), Vite 6.
- New (exactly two, per UI-NFR-6): `@fontsource-variable/figtree` and `@fontsource-variable/jetbrains-mono`
  (Latin subset, self-hosted by Vite). Package names are verified on install (quickstart step 1).
- Explicitly not added: an icon library, a component library, a CSS framework, a CSS-in-JS runtime, a
  clipboard library, a scroll library.

**Storage**: None. Conversation state stays in memory (existing `useConversation`). Nothing is persisted
(spec Assumptions: no saved conversations).

**Testing**: Vitest 2 with jsdom 25 and React Testing Library (existing suites in `frontend/tests/`). New
UI tests are listed in `data-model.md` and `quickstart.md`. Browser checks cover 360, 768, and 1280 px,
keyboard-only use, reduced motion, and a screen reader spot check. Contrast is re-measured in the browser
on the built CSS during review.

**Target Platform**: Evergreen desktop and mobile browsers. Phones down to 360 px wide.

**Project Type**: Frontend-only change to an existing web application. The backend is unchanged.

**Performance Goals**: The first streamed text appears within the existing baseline (FR-037, SC-005).
Fonts and CSS add at most 200 KB gzip (UI-NFR-4). Fonts use `font-display: swap`.

**Constraints**:
- No runtime requests to third-party origins (SC-007). `./scripts/check-bundle-secrets.sh dist` must pass.
- Input text is 16 px or larger, so mobile browsers do not zoom (FR-034).
- Touch targets are 44 px or larger (FR-033).
- Reduced motion is honored (FR-032).
- The page never scrolls sideways; only code blocks, tables, and the starter row scroll (FR-028).

**Scale/Scope**: One page and one conversation view. Roughly 8 components are changed and 6 are added.
About 15 new UI tests. Estimated tasks in `tasks.md`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Gate | Requirement (constitution v1.2.0) | Pre-research | Post-design |
|------|-----------------------------------|--------------|-------------|
| I. Secrets | Key never in config, source, logs, errors, or frontend | PASS | PASS: no backend or key path touched; bundle secret check kept in the quality gate |
| II. Configuration | Model, prompt, timeouts in `application.yml` | PASS | PASS: no model or timeout values change. Starter questions and UI copy are UI text, not model config |
| III. Spring AI seam | Only `ChatClient`; bounded memory; SSE | PASS | PASS: backend unchanged |
| IV. Scope and persona | In-scope only; no login, handoff, voice, multimodal, or history | PASS | PASS: starter questions are in-scope; no placeholder controls for out-of-scope features |
| V. Fail fast, fail friendly | Explicit timeouts; friendly UI errors; first token within a few seconds | PASS | PASS: error copy and retry logic unchanged; first-token latency is measured before and after (quickstart) |
| VI. Intentional, accessible UI | Skill used; direction recorded; tokens; measured WCAG AA; reference mapped; no out-of-scope mockups | PASS (direction recorded above; contrast measured in research R1) | PASS: tokens in one stylesheet (R3); measured pairs above; reference mapping in `prd-ui-2.md` 5.1; `--lilac-line` removed |
| Workflow: traceability | Requirements trace to `prd.md`, or to a companion PRD that references `prd.md` and does not expand scope (constitution v1.3.0) | PASS | PASS: G-5 closed; `prd.md` link no longer required |
| Governance: source of truth | `prd.md` owns scope; companion PRD owns UI requirements only and does not expand scope | PASS | PASS: spec and plan add no scope beyond `prd-ui-2.md` |

### Gates to close before implementation starts

- **G-5 (constitution Development Workflow): CLOSED 2026-11-14.** The maintainer chose to remove the
  `prd.md` pointer requirement. Constitution amended to v1.3.0: a companion PRD must reference `prd.md`
  and must not expand scope, and no link in `prd.md` is required. Constitution TODO-G is resolved.
- **G-6 (PRD accuracy): CLOSED 2026-11-14.** `prd-ui-2.md` section 5.2 now uses the measured values, and
  `--lilac-line` is removed. The header cites the constitution version (updated to v1.3.0 in the same
  pass as this gate, constitution TODO-F).

No Complexity Tracking entries are needed for the constitution: no principle is violated.

**UI-NFR-6 exception (open)**: GFM tables (FR-009) need `remark-gfm`, which is a third dependency beyond the
two font packages. Its install was denied during implementation, so the table test fails until a decision
is made (see `measurements.md`, "Open blocker").

## Project Structure

### Documentation (this feature)

```text
specs/002-update-frontend/
├── spec.md              # Feature specification (/speckit-specify)
├── plan.md              # This file (/speckit-plan)
├── research.md          # Phase 0: decisions R1-R13 with measured contrast
├── data-model.md        # Phase 1: UI state and constant entities (no persisted data)
├── quickstart.md        # Phase 1: runnable validation guide
├── contracts/           # Skipped: no new external interface (see below)
├── checklists/
│   └── requirements.md  # Spec quality checklist (/speckit-specify)
└── tasks.md             # Created by /speckit-tasks, not by this command
```

**Contracts skipped**: the only external interface is the existing chat API, documented in
`specs/001-tech-support-chat/contracts/chat-api.openapi.yaml` and `chat-stream-events.md`. This feature
does not change them, so no new contract is written.

### Source Code (repository root)

```text
frontend/
├── index.html                         # unchanged (title already set; theme-color added via UI-FR-11)
├── package.json                       # + two @fontsource-variable packages
├── src/
│   ├── main.tsx                       # + import of the stylesheet and the two font packages
│   ├── App.tsx                        # layout: Header, EmptyState or MessageList, composer, Jump to latest
│   ├── styles/
│   │   └── app.css                    # NEW: the single global stylesheet (tokens + components)
│   ├── content/
│   │   └── starterQuestions.ts        # NEW: the four fixed opening questions (UI-FR-3)
│   ├── utils/
│   │   └── copyText.ts                # NEW: copy with a fallback when the Clipboard API is unavailable
│   ├── hooks/
│   │   ├── useConversation.ts         # unchanged
│   │   └── useStickToBottom.ts        # NEW: scroll-follow state for streaming (UI-FR-7, R4)
│   ├── components/
│   │   ├── Header.tsx                 # NEW: assistant mark, title, New conversation (UI-FR-2)
│   │   ├── NewConversationButton.tsx  # restyled: coral pill with icon and visible text
│   │   ├── EmptyState.tsx             # NEW: greeting, subline, starter cards (UI-FR-3)
│   │   ├── MessageList.tsx            # customer bubble, assistant card, markdown overrides (UI-FR-4..6)
│   │   ├── CodeBlock.tsx              # NEW: fenced code with Copy and live confirmation (UI-FR-5)
│   │   ├── TypingIndicator.tsx        # progress bar replaces the three dots (UI-FR-8 waiting state)
│   │   ├── ErrorBanner.tsx            # coral notice and outlined pill (UI-FR-10)
│   │   ├── MessageInput.tsx           # composer card, auto-grow, Send icon, counter (UI-FR-8, UI-FR-9)
│   │   └── JumpToLatest.tsx           # NEW: violet pill above the composer (UI-FR-7)
│   ├── errors/friendlyMessages.ts     # unchanged (copy frozen for this release)
│   ├── services/                      # unchanged
│   ├── types/chat.ts                  # unchanged
│   └── config.ts                      # unchanged
└── tests/
    ├── emptyState.test.tsx            # NEW: greeting, four starters, send-on-activate, hide and return
    ├── codeCopy.test.tsx              # NEW: Copy, "Copied" for two seconds, live announcement
    ├── scrollFollow.test.tsx          # NEW: follows at bottom, holds when scrolled up, Jump to latest
    ├── composer.test.tsx              # NEW: editable while streaming, Send disabled, counter threshold
    ├── focusReturn.test.tsx           # NEW: focus returns to the composer after reply and reset
    ├── accessibleNames.test.tsx       # NEW: names and roles from prd-ui-2 section 9 still resolve
    └── (existing suites)              # must keep passing; see the compatibility note below
```

**Structure Decision**: Keep the existing component tree and add only what the design requires. A
single stylesheet (`styles/app.css`) is used, as UI-FR-1 requires. Components use class names and
tokens, not inline styles, which keeps selector specificity flat (one class per rule, no element
selectors outside the reply-content scope, see R3).

**Compatibility note**: No existing test asserts a disabled composer. `useConversation.test.tsx` checks
that a second send is blocked while streaming, and the hook still enforces that, so it keeps passing.
The composer change (FR-019) affects only the `disabled` prop in `App.tsx` and `MessageInput.tsx`.

## Complexity Tracking

No violations. Nothing in this plan needs justification beyond the spec.

## Provisional Decisions

These follow the defaults in `spec.md` Assumptions and change cheaply if the answer differs.

| Item | Provisional decision | Where it lands | If the answer differs |
|------|----------------------|----------------|-----------------------|
| Starter questions | Send on activation | `EmptyState.tsx`, `App.tsx` | Fill the composer instead: change one handler |
| Time stamps | Not shown | Not built | Add a per-message time in `MessageList.tsx` (client-side only) |
| New conversation mid-reply | Aborts the reply and clears it (existing hook behavior) | `useConversation.ts` (unchanged) | Keep the partial reply: change `reset` only |
| Dark mode | Deferred | Tokens allow a second theme later | Add a `[data-theme]` token block |
| Brand assets | Reference look used until assets arrive | `styles/app.css` tokens and the header mark | Replace tokens and the mark only |
| Screen reader streaming | Announce on completion, not per token (R10) | `MessageList.tsx` | Announce per token: remove `aria-busy` |
