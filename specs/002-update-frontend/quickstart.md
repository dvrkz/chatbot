# Quickstart: Validate the Chat Interface Refresh

**Feature**: `002-update-frontend` | **Plan**: [plan.md](./plan.md) | **Spec**: [spec.md](./spec.md)

This guide proves the feature works end to end. It lists commands and expected results. Implementation
detail belongs in `tasks.md`.

## Prerequisites

- Node 22 and npm, as used by `frontend/` (`engines.node >= 22`).
- For real streamed replies: the backend running with `GOOGLE_API_KEY` in the git-ignored `.env`
  (see `CLAUDE.md`). Without it, the unit tests still cover every state using a mocked stream.
- A desktop browser with developer tools, and a phone or device emulation for the 360 px checks.

## 1. Install and verify the font packages

```bash
cd frontend
npm install
npm ls @fontsource-variable/figtree @fontsource-variable/jetbrains-mono
```

**Expected**: both packages are listed once, at the top level. If a package name does not resolve, stop and
resolve it before continuing (research R2).

## 2. Unit and component tests

```bash
cd frontend
npm test
```

**Expected**: all existing suites pass (`errorFlow`, `newConversation`, `sseParser`, `useConversation*`,
`friendlyMessages`), plus the new suites: `emptyState`, `codeCopy`, `scrollFollow`, `composer`,
`focusReturn`, `accessibleNames`.

## 3. Type check and production build

```bash
cd frontend
npm run build
```

**Expected**: `tsc -b` reports no errors and `vite build` completes.

## 4. Bundle secret and third-party check

```bash
cd frontend
./scripts/check-bundle-secrets.sh dist
```

**Expected**: the script reports no secret. In the browser, the Network panel on first load shows requests
only to `localhost` (SC-007). Fonts load from the same origin.

## 5. Run the app

```bash
# terminal 1 (repository root)
cd backend && mvn spring-boot:run

# terminal 2
cd frontend && npm run dev
```

Open `http://localhost:5173`.

## 6. Screen states at three widths (SC-002)

Use device emulation at **360 px**, **768 px**, and **1280 px**. Capture each state:

1. Opening screen (no messages): greeting, subline, four starter cards.
2. Mid-stream reply (first token arrived, bar visible).
3. Complete reply containing a fenced code block with a Copy control.
4. Complete reply containing a table.
5. Failed reply (trigger with the backend stopped): coral notice and Try again, no code shown.
6. Cut-short reply (stop the backend mid-stream): partial text, "cut short" notice, Try again.
7. A message of exactly 2,000 characters, then 2,001 (refused with a plain message).

**Expected**: no clipped text, and no sideways scrolling of the page at any width. Code blocks and tables
scroll inside themselves, and so does the starter row.

## 7. Keyboard-only run-through (SC-003, FR-030)

Use Tab and Shift+Tab only, with no mouse:

1. Tab to a starter card, press Enter. The question sends.
2. Tab to the composer, type, press Enter to send.
3. During the reply, Tab to a code block's Copy, press Enter. "Copied" appears.
4. Force a failure, Tab to Try again, press Enter.
5. Tab to New conversation, press Enter. Focus lands in the composer.

**Expected**: every control in the path has a visible focus ring. No control is skipped and none traps focus.

## 8. Streaming behavior (FR-015, FR-016, FR-019)

1. Send a question that produces a long reply. Stay at the bottom: the view follows the new text.
2. Scroll up mid-stream: the view holds still and "Jump to latest" appears. Activate it: the view returns
   to the bottom.
3. While a reply streams, type a draft: the text is accepted, and Send stays disabled until the reply ends.

## 9. First-token latency baseline (SC-005, Principle V)

Before the first UI change lands, measure time from Send to the first text, on the current UI, over ten
sends. Record the median. Repeat after the change, on the same backend and prompt set. The median must not
rise (the constitution treats a regression as a review issue). Record both numbers in the review.

## 10. Reduced motion (FR-032)

Turn on "reduce motion" in the operating system, or emulate `prefers-reduced-motion: reduce` in developer
tools. During a stream, the waiting bar must be static, and nothing else may move on its own.

## 11. Contrast re-check on the built CSS (FR-029, Principle VI)

Run the production build, then, in the browser, check each pair from `plan.md`'s palette table with the
browser's contrast tool. Record each measured value next to the planned one. Any pair below its required
ratio fails the review, regardless of the planned value.

## 12. Screen reader spot check (FR-031, research R10)

With one desktop screen reader and one mobile screen reader:
1. A reply is not announced word by word while it streams.
2. The complete reply is announced once it ends.
3. A failure notice is announced as an alert.
4. Copy's "Copied" confirmation is announced.

If any check fails, revise research R10 before tasks are closed.

## 13. Accessible names (prd-ui-2 section 9)

These names must still resolve, in both the tests and the browser: label "Your question", button "Send",
button "New conversation", button "Try again", the alert role on errors, status role "Assistant is typing",
and `aria-live` on the message list.

## Done when

- [ ] Steps 1 to 4 pass, with the outputs recorded.
- [ ] Screens in step 6 captured at all three widths.
- [ ] Keyboard run-through (step 7) and streaming behavior (step 8) confirmed.
- [ ] Latency median before and after recorded (step 9).
- [ ] Contrast re-check (step 11) and the screen reader check (step 12) recorded.
- [ ] Gates G-5 and G-6 in `plan.md` closed.
