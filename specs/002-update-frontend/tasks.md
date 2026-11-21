# Tasks: Chat Interface Visual and Interaction Refresh

**Input**: Design documents from `/specs/002-update-frontend/` (spec.md, plan.md, research.md, data-model.md, quickstart.md)

**Prerequisites**: plan.md (required), spec.md (required, user stories), research.md, data-model.md, quickstart.md. No contracts/ folder: the chat API is unchanged (see plan.md).

**Tests**: Included. spec.md Assumptions states "New tests cover the behaviors in the acceptance scenarios", and plan.md lists the test suites to create.

**Organization**: Grouped by user story (spec.md priorities P1 to P3) so each story can be built and checked on its own.

**Path root**: all paths are relative to the repository root. Frontend code is under `frontend/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no unfinished dependency). Tasks that edit `frontend/src/styles/app.css`, `frontend/src/App.tsx`, or `frontend/src/components/MessageInput.tsx` are never marked [P] inside a phase, because they share a file.
- **[Story]**: US1 to US6, mapping to spec.md User Stories 1 to 6.

---

## Phase 1: Setup (Blocking Prerequisites Before Any UI Edit)

**Purpose**: Record the baseline and close the open gates. Quickstart step 9 requires the latency baseline to be captured before the first UI file changes.

- [x] T001 Record the first-token latency baseline in `specs/002-update-frontend/measurements.md`. Send ten questions on the current UI against the running backend, and record the median time from Send to first text (quickstart step 9, SC-005).
- [x] T002 Record the baseline test and build results in `specs/002-update-frontend/measurements.md`. Run `cd frontend && npm test` and `cd frontend && npm run build`, and note the pass or fail output (quickstart steps 2 and 3).
- [x] T003 Close gate G-5 in `specs/002-update-frontend/plan.md`. The maintainer chooses either a one-line pointer to `prd-ui-2.md` in `prd.md` section 6, or a constitution amendment that removes the pointer requirement. Record the choice and its date in the plan's gate list.
- [x] T004 Install the two font packages. Run `cd frontend && npm install @fontsource-variable/figtree @fontsource-variable/jetbrains-mono`, then `npm ls` on both. Updates `frontend/package.json` and `frontend/package-lock.json` (plan.md Technical Context, UI-NFR-6).

---

## Phase 2: Foundational (Blocks All User Stories)

**Purpose**: The shared stylesheet, the font loading, the layout shell, and the constants and helpers every story uses.

**⚠️ CRITICAL**: No user story work begins until this phase is complete.

- [x] T005 Create `frontend/src/styles/app.css` with the color tokens on `:root`, using exactly these values from data-model.md section 4: `--mist: #ECEBF6`, `--paper: #FFFFFF`, `--violet: #6A3DF0`, `--violet-deep: #4F26C9`, `--ink: #1F1B33`, `--slate: #5F5A7A`, `--coral: #B42340`, `--coral-tint: #FBE9EE`. Do NOT define `--lilac-line` (measured at 1.17:1, removed in prd-ui-2 section 5.2).
- [x] T006 Add to `frontend/src/styles/app.css` the type, spacing, and radius tokens: font sizes 14, 17, 20, and 24 px (base 17 px, ratio 1.2); reply line-height 1.55 and code line-height 1.5; reply measure `max-width: 68ch`; radii 20 px (cards and customer messages), 12 px (code blocks and notices), 16 px (composer), and a full pill; spacing steps 4, 8, 12, 16, and 24 px. Set `body` to `background: var(--mist)`, `color: var(--ink)`, and the Figtree family. Components must use these tokens, not literal values (FR-025).
- [x] T007 [P] Edit `frontend/index.html`. Add `<meta name="theme-color" content="#ECEBF6" />` inside `<head>` (UI-FR-11).
- [x] T008 Edit `frontend/src/main.tsx`. Import `./styles/app.css`, plus `@fontsource-variable/figtree/wght.css` and `@fontsource-variable/jetbrains-mono/wght.css`. The packages have no per-subset CSS file: `wght.css` declares `font-display: swap` and `unicode-range` per subset, so the browser downloads only the Latin file for Latin text (verified in T004). Depends on T004 and T005.
- [x] T009 [P] Create `frontend/src/content/starterQuestions.ts`. Export `STARTER_QUESTIONS` as an array of exactly four objects `{ id, text }`, with these texts verbatim: "Why am I getting a 429 error?", "How do I stream a response?", "What counts toward my token limit?", "How do I choose a model?" (spec FR-006, data-model section 3).
- [x] T010 [P] Create `frontend/src/utils/copyText.ts`. Export `async function copyText(text: string): Promise<boolean>`. Use `navigator.clipboard.writeText` when it exists. Otherwise copy through a temporary off-screen textarea and `document.execCommand('copy')`. Return `false` if both fail. No new dependency (research R6).
- [x] T011 Restructure the layout shell in `frontend/src/App.tsx`. Wrap the page in a root column that is at most `40rem` wide, centered, with 16 px side padding and `min-height: 100dvh`. Create three regions in order: a header slot, a scrollable message region (class `message-scroll`), and a composer region with `padding-bottom: env(safe-area-inset-bottom)`. Keep the existing `useConversation` wiring unchanged. Depends on T005 and T006.

**Checkpoint**: Foundation ready. The stylesheet loads, the layout shell renders, and the constants and copy helper exist.

---

## Phase 3: User Story 1, Start a Conversation from the Opening Screen (Priority: P1) 🎯 MVP

**Goal**: A customer with no messages sees a greeting, a subline, and four suggested questions. Choosing one sends it as if typed, and the suggestions disappear.

**Independent Test**: Open the chat with no prior messages. Choose one suggested question and confirm it is sent and answered. Then type a question and confirm it is sent.

### Tests for User Story 1

- [x] T012 [P] [US1] Create `frontend/tests/emptyState.test.tsx`. Assert the heading "Hi, how can we help?", the subline "Ask about the API, errors, or rate limits.", a "Suggested questions" heading, and exactly four buttons whose names equal the `text` values in `STARTER_QUESTIONS`.
- [x] T013 [P] [US1] Create `frontend/tests/starterSend.test.tsx`. Mock `streamChat` from `frontend/src/services/chatApi.ts`. Assert that activating a starter sends its exact text as the customer message, that the starters are hidden after the first message, and that they return after the conversation is reset.

### Implementation for User Story 1

- [x] T014 [P] [US1] Create `frontend/src/components/EmptyState.tsx`. Props: `onSelect(text: string)`. Render the greeting, the subline, the "Suggested questions" heading, and a list of four `<button>` elements with class `starter-card`. Each button calls `onSelect` with its text (FR-006, FR-007).
- [x] T015 [US1] Add to `frontend/src/styles/app.css` the classes `.empty-state`, `.starter-row` (`overflow-x: auto`, `scroll-snap-type: x mandatory`, 12 px gap), and `.starter-card` (white `--paper` background, 20 px radius, `scroll-snap-align: start`, minimum 44 px height, focus ring). Depends on T006.
- [x] T016 [US1] Update `frontend/src/App.tsx`. Render `EmptyState` when `messages.length === 0`, and pass `onSelect={(text) => void send(text)}`. Depends on T011 and T014.

**Checkpoint**: User Story 1 works. A customer can start from a suggestion or from typing, and the opening screen returns after a reset.

---

## Phase 4: User Story 2, Read and Act on a Streamed Reply (Priority: P1)

**Goal**: Replies render lists, tables, inline code, code blocks, quotes, and links clearly. Code can be copied in one action, and the Copy confirmation is announced.

**Independent Test**: Ask a question whose answer includes a code block and a table. Confirm the text appears progressively, the code is copied in one action, and the table fits the screen.

### Tests for User Story 2

- [x] T017 [P] [US2] Create `frontend/tests/codeCopy.test.tsx`. Mock `copyText`. Assert that clicking Copy passes the exact code text, without fence markers, to `copyText`. Assert the label changes to "Copied" at once, and reverts to "Copy" after exactly 2000 ms (use `vi.useFakeTimers`). Assert the visually hidden status region contains "Copied".
- [ ] T018 [P] [US2] Create `frontend/tests/markdownReply.test.tsx`. Assert that a reply containing a list, inline code, a blockquote, a link, and a table renders each, with the table inside a `.table-scroll` wrapper. Assert the link has `target="_blank"` and `rel="noopener noreferrer"`. Assert that a reply containing the text `<script>alert(1)</script>` shows the text and creates no `script` element (research R7).

### Implementation for User Story 2

- [x] T019 [P] [US2] Create `frontend/src/components/CodeBlock.tsx`. Props: the `pre` element children. Render `<pre className="code-block">` with a `<button className="copy-pill">Copy</button>` at the top right. On click, call `copyText` with the code element's text. Show "Copied" for 2000 ms, then revert. Clear the timer on unmount. Include a visually hidden `role="status"` region that announces "Copied" (FR-010, research R6).
- [x] T020 [US2] Update `frontend/src/components/MessageList.tsx`. Render customer messages as `.message-customer` (violet fill, white text, right aligned, at most 85% wide, 20 px radius with a 6 px bottom-right corner). Render assistant replies as `.message-assistant` (white card, 20 px radius). Pass to `ReactMarkdown` a `components` map: `pre` uses `CodeBlock`, `a` opens in a new tab with `rel="noopener noreferrer"`, `table` is wrapped in `.table-scroll`, and `h1` to `h3` use a `.reply-heading` class capped at the 20 px step. Do NOT add `rehype-raw` (raw HTML stays escaped). Depends on T019.
- [x] T021 [P] [US2] Update `frontend/src/components/TypingIndicator.tsx`. Replace the three dots with a `.typing-bar` track and a `.typing-bar-segment`, and show the words "Looking into it" beside it. Keep `role="status"` and `aria-label="Assistant is typing"` (spec FR-014, prd-ui-2 section 5.5).
- [x] T022 [US2] Add to `frontend/src/styles/app.css` the classes `.message-customer`, `.message-assistant`, `.reply` (scoped element styles for `p`, `ul`, `ol`, `blockquote`, `code`, and `a`, with violet underlined links), `.code-block` (`--mist` background, 12 px radius, `overflow-x: auto`, JetBrains Mono, line-height 1.5), `.copy-pill`, `.table-scroll`, and `.typing-bar-segment` with its keyframe animation inside `@media (prefers-reduced-motion: no-preference)`. Depends on T006 and T015.

**Checkpoint**: User Story 2 works. Replies render fully, code copies in one action, and the streaming bar shows while text arrives.

---

## Phase 5: User Story 3, Stay in Control While a Reply Streams (Priority: P1)

**Goal**: The view follows new text only when the customer is at the bottom. Scrolling up holds the view still and shows "Jump to latest". The composer stays editable while a reply streams, and focus returns to it when the reply ends.

**Independent Test**: Start a long streamed reply, scroll up, and confirm the view holds and "Jump to latest" appears. Type a draft during the stream and confirm Send stays unavailable until the reply ends.

### Tests for User Story 3

- [x] T023 [P] [US3] Create `frontend/tests/scrollFollow.test.tsx`. Set `scrollTop`, `scrollHeight`, and `clientHeight` on the scroll region. Assert that within 48 px of the bottom, new text scrolls to the bottom. Assert that more than 48 px above the bottom, `scrollTop` does not change when new text arrives, and "Jump to latest" appears. Assert that activating "Jump to latest" scrolls to the bottom and hides it. Assert that sending a message scrolls to the bottom (FR-015, FR-016, FR-017).
- [x] T024 [P] [US3] Create `frontend/tests/composer.test.tsx`. Assert that the textarea stays enabled while the status is `streaming`, that Send is disabled then, and that typed text is kept. Assert that the counter is hidden at 1599 characters and shows "1,600 of 2,000" at 1600 (80% of the 2000 limit). Assert that above 2000 characters the counter and the note use the coral class, and that blank input shows "A message is required." (FR-005, FR-019, FR-020).
- [x] T025 [P] [US3] Create `frontend/tests/focusReturn.test.tsx`. Assert that the textarea receives focus when a reply moves from `streaming` back to `idle` (FR-021).

### Implementation for User Story 3

- [x] T026 [P] [US3] Create `frontend/src/hooks/useStickToBottom.ts`. Return `{ ref, pinned, scrollToBottom, onScroll }`. A list is pinned when `scrollHeight - scrollTop - clientHeight <= 48`. While pinned, the hook scrolls to the bottom whenever the message list changes. When not pinned, it does not move the view (research R4).
- [x] T027 [P] [US3] Create `frontend/src/components/JumpToLatest.tsx`. Render a `<button className="jump-pill">Jump to latest</button>`. Props: `onClick`. It is rendered only when the list is not pinned and messages exist (FR-015, UI-FR-7).
- [x] T028 [US3] Update `frontend/src/App.tsx`. Attach `useStickToBottom` to the `message-scroll` region. Call `scrollToBottom` on send. Render `JumpToLatest` above the composer when the list is not pinned. Depends on T011, T016, T026, and T027.
- [x] T029 [US3] Update `frontend/src/components/MessageInput.tsx`. Remove the `disabled` attribute from the textarea. Keep `disabled` on the Send button only. Keep the existing validation and messages: "A message is required." for blank input, and the over-length note "Messages can be at most 2000 characters." Enter sends, and Shift+Enter inserts a line break (FR-001, FR-019).
- [x] T030 [US3] Update `frontend/src/components/MessageInput.tsx`. Auto-grow the textarea: after each change, set its height from `scrollHeight`, capped at eight lines, after which it scrolls (FR-018, research R5). Add a visually hidden label "Your question" and the placeholder "Ask about the API, errors, or rate limits". Add the hint "Enter to send, Shift+Enter for a new line", hidden below 480 px. Depends on T029.
- [x] T031 [US3] Update `frontend/src/components/MessageInput.tsx`. Add the counter: when the trimmed draft is at least 80% of `MAX_MESSAGE_CHARS` (1600 of 2000), show "{n} of {MAX}" with a thousands separator, in `--slate`. Above the limit, use the coral class. Replace the Send text with a circular button that has an inline SVG paper-plane icon (`aria-hidden`) and the accessible name "Send" (UI-FR-8). Depends on T030.
- [x] T032 [US3] Update `frontend/src/App.tsx` and `frontend/src/components/MessageInput.tsx`. Forward a ref to the textarea. In `App.tsx`, when the status changes from `streaming` to `idle`, focus the textarea (FR-021, research R9). Depends on T028 and T031.
- [x] T033 [US3] Add to `frontend/src/styles/app.css` the classes `.composer` (white card, 16 px radius, the one soft shadow token on white only), the textarea font size of 16 px or larger (FR-034), `.counter` and `.counter-over`, `.send-button` (circular, violet fill, minimum 44 px), `.jump-pill` (violet fill, white text, pill), and a media query that hides `.composer-hint` below 480 px. Depends on T006.

**Checkpoint**: User Story 3 works. Streaming does not move text the customer is reading, the composer stays editable, and focus returns after each reply.

---

## Phase 6: User Story 4, Recover from a Failed or Cut-Off Reply (Priority: P2)

**Goal**: Failures show a calm coral notice with the friendly text and a Try again pill. Partial replies are labelled as cut short. No error code or internal detail is shown.

**Independent Test**: Simulate a failed reply and a reply cut off mid-stream. Confirm the friendly text and the Try again action appear, and that no error code is shown.

### Tests for User Story 4

- [x] T034 [P] [US4] Create `frontend/tests/errorNotice.test.tsx`. Assert that a failed reply shows a `role="alert"` notice with the text from `friendlyMessage` and a "Try again" button. Assert that an incomplete reply starts its notice with "Reply was cut short." and keeps its partial text. Assert that no error code string (for example `UPSTREAM_UNAVAILABLE` or `INTERNAL_ERROR`) appears anywhere in the document (FR-004, SC-006).

### Implementation for User Story 4

- [x] T035 [P] [US4] Update `frontend/src/components/ErrorBanner.tsx`. Wrap the content in a `.notice` element. Keep `role="alert"`, the `friendlyMessage(code)` text, the "Reply was cut short. " prefix when `partial` is true, and the `canRetry` logic. Render Try again as a `.pill-outline-coral` button (FR-004, UI-FR-10).
- [x] T036 [US4] Add to `frontend/src/styles/app.css` the classes `.notice` (`--coral-tint` background, `--coral` text, 12 px radius), `.pill-outline-coral` (2 px `--coral` border, full pill, minimum 44 px), and the `.counter-over` coral text used by the over-limit note. Depends on T033.

**Checkpoint**: User Story 4 works. Failures are calm, recoverable, and free of internal detail.

---

## Phase 7: User Story 5, Use the Chat on Any Screen and Input Method (Priority: P2)

**Goal**: The page works from 360 px to desktop. Every control is keyboard reachable with a visible focus ring. New content is announced, and reduced motion is respected.

**Independent Test**: Complete a full run-through using the keyboard only, then repeat at 360 px and at desktop width.

### Tests for User Story 5

- [x] T037 [P] [US5] Create `frontend/tests/accessibleNames.test.tsx`. Assert these names and roles still resolve: label "Your question" for the textarea, button "Send", button "New conversation", button "Try again" after a failure, `role="alert"` on errors, `role="status"` named "Assistant is typing" while streaming, and an `aria-live="polite"` list. Assert that the list has `aria-busy="true"` while streaming and `aria-busy` absent or `"false"` after the reply completes (prd-ui-2 section 9, research R10).

### Implementation for User Story 5

- [x] T038 [US5] Update `frontend/src/components/MessageList.tsx` and `frontend/src/App.tsx`. Pass `status` into `MessageList`, and set `aria-busy={status === 'streaming'}` on the list. This holds announcements until the reply is complete (research R10). Depends on T020 and T028.
- [x] T039 [US5] Add to `frontend/src/styles/app.css` a global `:focus-visible` rule with a 2 px `--violet` outline and a 2 px offset, applied to every control. Do not use `outline: none` anywhere (FR-030, research R9). Depends on T036.
- [x] T040 [US5] Add to `frontend/src/styles/app.css` a minimum 44 px target for every button, starter card, copy pill, and the Send button (FR-033). Depends on T039.
- [x] T041 [US5] Add to `frontend/src/styles/app.css` a `@media (prefers-reduced-motion: reduce)` block. In it, the typing bar is a static partial fill, the Copy confirmation appears without a fade, and no other transition or animation is set (FR-032, research R8). Depends on T022.
- [x] T042 [US5] Add to `frontend/src/styles/app.css` `min-width: 0` on flex children and `overflow-wrap: anywhere` on prose, so the page never scrolls sideways. Code blocks, tables, and the starter row keep their own overflow (FR-028). Depends on T040.

**Checkpoint**: User Story 5 works on every width and input method, and assistive technology announces the right content.

---

## Phase 8: User Story 6, Start Over (Priority: P3)

**Goal**: A customer starts a fresh conversation from the header. The header stays visible, New conversation is unavailable when the conversation is empty, and focus lands in the composer afterward.

**Independent Test**: Send two messages, activate New conversation, and confirm the opening state returns with suggestions visible and the composer focused.

### Tests for User Story 6

- [x] T043 [P] [US6] Create `frontend/tests/header.test.tsx`. Assert that the title "Caramel AI Tech Support" is shown, and that the assistant mark is `aria-hidden`. Assert that New conversation is disabled with no messages and enabled after one. Assert that activating it clears the messages, shows the starters, and focuses the textarea (FR-003, FR-021, FR-022).

### Implementation for User Story 6

- [x] T044 [P] [US6] Create `frontend/src/components/Header.tsx`. Render a `<header>` containing the assistant mark (a violet circle with a "C" glyph in inline SVG, `aria-hidden`), an `<h1>` with the title "Caramel AI Tech Support", and `NewConversationButton`. Props: `onReset`, `canReset`. Apply the sticky class (UI-FR-2).
- [x] T045 [US6] Update `frontend/src/components/NewConversationButton.tsx`. Render a coral pill with the text "New conversation" and a pencil-or-plus inline SVG (`aria-hidden`). Accept a `disabled` prop. Minimum 44 px target (UI-FR-2).
- [x] T046 [US6] Update `frontend/src/App.tsx`. Replace the inline header with `Header`, passing `canReset={messages.length > 0}`. On reset, focus the textarea (FR-021). Depends on T028, T032, T044, and T045.
- [x] T047 [US6] Add to `frontend/src/styles/app.css` the classes `.app-header` (sticky at the top, `--mist` background), `.new-conversation` (coral fill, white label, pill, 44 px minimum), and `.mark` (violet circle). Depends on T039.

**Checkpoint**: User Story 6 works. All six stories are complete.

---

## Final Phase: Polish & Cross-Cutting Verification

**Purpose**: Confirm the whole feature against the spec, the constitution, and the quickstart.

- [ ] T048 Run `cd frontend && npm test`. All existing suites (`errorFlow`, `newConversation`, `sseParser`, `useConversation*`, `friendlyMessages`) and every new suite must pass (quickstart step 2).
- [x] T049 Run `cd frontend && npm run build`, then `cd frontend && ./scripts/check-bundle-secrets.sh dist`. Both must pass (quickstart steps 3 and 4, UI-NFR-5).
- [x] T050 Verify token discipline. Run `grep -rn "lilac" frontend/src` and search `frontend/src` for hex literals outside `frontend/src/styles/app.css`. Both must return nothing (FR-025, data-model section 4).
- [ ] T051 Check for third-party requests. Run the built app and open the Network panel on first load. Only `localhost` requests may appear, and the fonts must load from the same origin (SC-007, quickstart step 4).
- [ ] T052 Capture the seven screen states at 360, 768, and 1280 px into `specs/002-update-frontend/screenshots/`: opening, mid-stream, code block, table, failed, cut short, and a 2000-character message. No clipped text and no page-level sideways scroll (SC-002, quickstart step 6).
- [ ] T053 Run the keyboard-only run-through from quickstart step 7 and record the result in `specs/002-update-frontend/measurements.md` (SC-003).
- [ ] T054 Run the streaming checks from quickstart step 8 (follow, hold, Jump to latest, draft while streaming) and record the result.
- [x] T055 Check reduced motion from quickstart step 10. With the setting on, the typing bar is static and nothing else moves (FR-032).
- [ ] T056 Re-measure every pair in `prd-ui-2.md` section 5.2 with a browser contrast tool on the built CSS (quickstart step 11, FR-029). If any text pair is below 4.5:1, fix the token in `frontend/src/styles/app.css` and update `prd-ui-2.md` section 5.2 to match.
- [ ] T057 Run the screen reader spot check from quickstart step 12 with one desktop and one mobile reader. If the streaming behavior fails, revise research R10 and the `aria-busy` task (T038) before closing.
- [ ] T058 Compare the first-token latency median after the change with the T001 baseline (quickstart step 9). It must not rise. Record both values.
- [ ] T059 Do the reference side-by-side review from `prd-ui-2.md` section 10, item 8, and record the result, including the reviewers' agreement (SC-008).
- [ ] T060 Close out. Confirm every quickstart "Done when" item is checked, and that gates G-5 (T003) and G-6 (the `prd-ui-2.md` correction) are closed in `specs/002-update-frontend/plan.md`.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: starts first. T001 must run before any UI file changes.
- **Foundational (Phase 2)**: depends on Setup and blocks every user story.
- **User stories (Phases 3 to 8)**: depend on Foundational. They share `App.tsx`, `MessageInput.tsx`, `MessageList.tsx`, and `app.css`, so run them in priority order (US1, US2, US3, then US4 to US6). Within a story, only the [P] tasks can run side by side.
- **Polish (Final Phase)**: depends on all six stories being complete.

### Story dependencies

- **US1 (P1)**: depends only on Foundational.
- **US2 (P1)**: depends on Foundational and T015 (shared stylesheet).
- **US3 (P1)**: depends on US1 (`App.tsx` layout) and US2 (`MessageList` scroll target).
- **US4 (P2)**: depends on US2 (reply card) and US3 (`MessageInput` counter class).
- **US5 (P2)**: depends on US2, US3, and US4 (the final CSS and list markup).
- **US6 (P3)**: depends on US3 (focus return) and US5 (focus rules).

### Within each story

- Tests are written first, and each must fail before its implementation exists.
- Components come before the `App.tsx` wiring that uses them.
- Styles come after the markup they style.

---

## Parallel Example: User Story 2

```bash
# Tests (different files, run together):
Task: "T017 Create frontend/tests/codeCopy.test.tsx"
Task: "T018 Create frontend/tests/markdownReply.test.tsx"

# Components (different files, run together):
Task: "T019 Create frontend/src/components/CodeBlock.tsx"
Task: "T021 Update frontend/src/components/TypingIndicator.tsx"
```

---

## Implementation Strategy

### MVP first (User Story 1)

1. Complete Phase 1 (Setup) and Phase 2 (Foundational).
2. Complete Phase 3 (US1).
3. Stop and check the opening screen at 360 px and at desktop width.

US1 is the MVP increment, but it does not ship the release on its own. US2 and US3 are also P1, so the release needs all three P1 stories.

### Incremental delivery

1. Setup and Foundational: the design system is in place.
2. US1: the opening screen works.
3. US2: replies read well and code copies.
4. US3: streaming behaves correctly, and the composer is editable.
5. US4 to US6: failures, responsive behavior and accessibility, and start over.
6. Polish: verification against the spec, constitution, and quickstart.

Each increment is checked against its story's independent test before the next one starts.

---

## Summary

- **Total tasks**: 60
- **Setup (Phase 1)**: 4 tasks (T001 to T004)
- **Foundational (Phase 2)**: 7 tasks (T005 to T011)
- **US1 (P1)**: 5 tasks (T012 to T016)
- **US2 (P1)**: 6 tasks (T017 to T022)
- **US3 (P1)**: 11 tasks (T023 to T033)
- **US4 (P2)**: 3 tasks (T034 to T036)
- **US5 (P2)**: 5 tasks (T037 to T042)
- **US6 (P3)**: 5 tasks (T043 to T047)
- **Polish (Final Phase)**: 13 tasks (T048 to T060)
- **Parallel opportunities**: 20 tasks are marked [P]. The test files, the component files, and `frontend/index.html` are independent of each other.
- **Suggested MVP scope**: Phases 1 to 3 (US1)
