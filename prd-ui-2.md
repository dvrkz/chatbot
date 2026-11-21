# PRD: Caramel AI Tech Support Chat, UI Design Pass (v2)

Companion to `prd.md`. This document covers the visual and interaction design of the chat UI only.
It does not change scope, backend behavior, or the API. Governing rules: `.specify/memory/constitution.md`
v1.3.0, especially Principle VI (Intentional, Accessible UI Design), Principle V (Fail Fast, Fail
Friendly), and Principle IV (Scope and Persona).

## 1. Overview

The v1 chat works end to end, but the frontend ships with **no stylesheet**. There is no CSS file, no
inline style block, and nothing imported by `index.html` or `main.tsx`. The UI renders in browser
defaults: a bare heading, a plain list, an unstyled textarea and buttons.

This pass gives the chat a visual identity taken from a client reference image: a mobile screen with a
lavender-grey background, solid violet and white rounded cards, pill-shaped chips, circular avatars,
and a coral-red accent. The reference sets the visual language. Its content, data, and features
(tasks, dates, progress figures, avatar stacks, the create-task form) are not carried over, because
they have no counterpart in this product.

## 2. Objective

A Caramel AI customer who opens the chat during an incident can state their problem, read a reply that
is easy to scan and copy from, and recover from a failure, on a phone or a desktop.

## 3. Goals and Non-Goals

### Goals
- Replace browser-default styling with a design system built on shared tokens, following the reference.
- Make replies with code, lists, and tables readable, and make code copyable in one action.
- Make the composer and scrolling behave correctly while a reply streams.
- Meet WCAG 2.1 AA and work from 360 px to desktop widths.
- Keep every existing behavior, test, and accessible name working.

### Non-goals (this release)
- Backend, API, SSE protocol, system prompt, or model changes.
- Anything in `prd.md` section 5 "Out of scope": login, human handoff, persistent history, voice or
  multimodal input. The reference's task, schedule, and user-profile elements fall under this rule.
- Copying the reference's text, data, artwork, or fonts. Only its visual language is used.
- Dark mode, syntax highlighting, localization, and a custom logo (see section 11).

## 4. Audience and Design Brief

**Who:** developers integrating the Caramel AI inference API, plus less technical customers dealing
with usage or errors (`prd.md` section 4). Many arrive mid-incident: a failing 401, a 429, a stream
that hangs.

**Job of the screen:** get a question in fast, get a trustworthy answer that can be pasted into a
terminal or editor, and recover cleanly when something fails.

**Feel:** friendly and light, but exact. The reference's softness is kept, and the copy and the
code blocks stay precise. The interface should read like a competent, approachable support desk, not
a consumer chatbot with decoration that carries no information.

## 5. Design Direction

### 5.0 Revision note

An earlier draft of this PRD used a cool off-white page and a caramel-brown accent. The client then
supplied the reference image below, which takes precedence. The caramel direction is withdrawn.

In this revision, the contrast values were measured (section 5.2) and the outlined-control border was
changed from a lilac tint to `--violet`, because the tint failed the boundary contrast rule.

### 5.1 What the reference pins, and how each element maps

| Reference element | Adopted as | Notes |
|---|---|---|
| Lavender-grey page | Page background `--mist` | Sets the whole screen's tone. |
| Solid violet card with white text | Customer message | The strongest speaker contrast in the screen. |
| White rounded card with soft shadow | Assistant reply, composer, starter cards | One soft shadow token, used on white surfaces only. |
| Pill chips (filled and outlined) | "Copy", "Try again", "Jump to latest" | Actions only. The chips carry no filter state. |
| Coral-red circular button | "New conversation" in the header | Coral is the attention accent. |
| Progress bar on a card | Streaming indicator on the reply card's top edge | Replaces the bouncing dots. |
| Circular avatar | Assistant mark in the header | No customer avatar: there is no customer identity (login is out of scope). |
| Greeting with a subline | Empty-state heading | Neutral wording; no name. |
| Horizontal row of cards | Starter questions | Scroll-snap on narrow screens. |
| Time pills, avatar stacks, date strip, create-task form, progress figures, count badges | Not adopted | No counterpart in this product. Time pills are an open question (section 11). |

### 5.2 Color tokens

| Token | Hex | Use |
|---|---|---|
| `--mist` | `#ECEBF6` | Page background |
| `--paper` | `#FFFFFF` | Assistant cards, composer, starter cards |
| `--violet` | `#6A3DF0` | Customer message fill, Send button, links, streaming bar, focus ring, outlined chip and button borders |
| `--violet-deep` | `#4F26C9` | Hover and pressed state of violet fills |
| `--ink` | `#1F1B33` | Body text, reply text, headings |
| `--slate` | `#5F5A7A` | Secondary text, hints, placeholder |
| `--coral` | `#B42340` | "New conversation" fill, error text, "Try again" outline |
| `--coral-tint` | `#FBE9EE` | Error notice background |

Contrast, measured with the WCAG 2.1 relative luminance formula (computed 2026-11-14):

| Pair | Used for | Ratio | Required | Result |
|---|---|---|---|---|
| `--ink` on `--paper` | Reply text | 16.63 | 4.5 | Pass |
| `--ink` on `--mist` | Code text | 14.08 | 4.5 | Pass |
| `--violet` on `--mist` | Links on the page, outlined chip border | 5.02 | 4.5 text, 3.0 boundary | Pass |
| `--violet` on `--paper` | Links in cards, focus ring | 5.93 | 4.5 text, 3.0 boundary | Pass |
| White on `--violet` | Customer message text | 5.93 | 4.5 | Pass |
| White on `--violet-deep` | Hover and pressed text | 8.56 | 4.5 | Pass |
| `--slate` on `--mist` | Secondary text | 5.50 | 4.5 | Pass |
| `--slate` on `--paper` | Placeholder, length counter | 6.50 | 4.5 | Pass |
| `--coral` on `--paper` | Error text | 6.45 | 4.5 | Pass |
| `--coral` on `--coral-tint` | Error notice text, Try again outline | 5.53 | 4.5 text, 3.0 boundary | Pass |
| White on `--coral` | "New conversation" label | 6.45 | 4.5 | Pass |

Removed: `--lilac-line` (`#DCD9EE`) on `--mist` measures 1.17:1, which fails the 3:1 boundary rule.
Outlined controls therefore use `--violet` for their border. A lighter tint MUST NOT be used for any
border that is the only sign of a control.

These values are computed from the hex values. The build MUST re-measure every pair in a browser on the
built CSS with a contrast tool (UI-NFR-2 and section 10, item 4). Any text pair under 4.5:1 fails the review.

Violet is the speaker and action color. Coral is used only for the header action and for errors,
so a coral element always means "attention" or "start over". Neither color is used as a page wash.

### 5.3 Type

| Role | Family | Notes |
|---|---|---|
| Interface, headings, and reply text | Figtree (variable) | Weights 400, 600, 700. One family for header, replies, and controls. |
| Code and inline code | JetBrains Mono (variable) | Code only, never decorative labels. |

Figtree is chosen because its rounded geometric forms match the reference's friendly interface type.
Scale (ratio 1.2, base 17 px): 14 / 17 / 20 / 24 px. Reply line height 1.55; code line height 1.5.
Reply measure capped at 68 characters. Sentence case everywhere, no all-caps labels, no tracked-out
eyebrows. The header title is a single style: no single-word accent.

Both fonts are open-licensed and MUST be self-hosted and bundled (constitution, Design assets
constraint) via `@fontsource`, as woff2, Latin subset, with `font-display: swap` so fonts never
delay the first token.

### 5.4 Layout

Mobile first: one column, full width with 16 px side padding, cards inset from the edges. On desktop
(768 px and up) the same column is centered at `max-width: 40rem`. There is no phone frame and no side
panel. Text is left aligned; customer messages sit right aligned as blocks. Nothing is justified.

```
Mobile (360 px)
┌─────────────────────────┐
│ (C)  Caramel AI Tech …  (✎ New conversation)│  header, sticky
│                         │
│ Hi, how can we help?    │  greeting
│ Ask about the API,      │
│ errors, or rate limits. │
│ Suggested questions     │
│ ┌────────┐ ┌────────┐ → │  starter cards, horizontal scroll
│ │Why 429?│ │Stream? │   │
│ └────────┘ └────────┘   │
│                         │
│          ┌───────────┐  │  customer: violet fill, white text
│          │ Why am I  │  │
│          │ getting…  │  │
│          └───────────┘  │
│ ┌─────────────────────┐ │  assistant: white card
│ │▬▬▬▬▬▬▬▬▬▬ (streaming)│ │  progress bar on top edge
│ │ A 429 means you hit │ │
│ │ a rate limit.       │ │
│ │ ┌────────────[Copy]┐│ │  code block, mist surface
│ │ │ curl …           ││ │
│ │ └──────────────────┘│ │
│ └─────────────────────┘ │
│ ┌──────────────────┐(➤)│  composer: white card, violet Send
│ │ Ask about the …  │   │
│ └──────────────────┘   │
└─────────────────────────┘
```

Radius is set by role, not by one value everywhere: 20 px for cards and customer messages, 12 px for
code blocks and error notices, full pill for chips and the header button, 16 px for the composer.
The customer bubble's bottom-right corner is 6 px so consecutive messages group visually.

### 5.5 The one memorable element: the violet and white pairing

A solid violet customer bubble and a white assistant card on lavender make the two voices clear
without avatars or labels. While a reply streams, a short violet segment moves along the card's top
edge, like the progress bar in the reference. Before the first token, the same bar runs alone next to
the text "Looking into it". When the reply ends, the bar disappears. The indicator keeps
`role="status"` and the accessible name "Assistant is typing".

This is the only non-user-triggered motion, together with the Copy confirmation. Everything else is
still.

### 5.6 Where the reference is followed, and where defaults still apply

The reference uses soft shadows and rounded white cards. The `frontend-design` skill lists that
combination among generic defaults. The skill's own rule applies: where the brief pins a direction,
the brief's words win. So the reference's shadows and rounded cards are followed here.

Where the reference is silent, the anti-default checks still apply:
- No all-caps labels, tracked-out eyebrows, or dot-separated meta strings.
- No arrows appended to link or button text. The Send icon and the starter-card scroll cue are
  icons, not text.
- No decorative gradients. The lavender page is a flat fill.
- Shadow appears on white surfaces only, with one token; it is not applied to every element.
- Radius varies by role (section 5.4), not one value on everything.

## 6. Functional Requirements

Each requirement keeps the element it touches working as in v1 (`prd.md` section 6, item 5).

**UI-FR-1: Design system.** Add a single global stylesheet imported from `main.tsx`, defining the
tokens in section 5.2 as CSS custom properties, plus a type scale, spacing scale, and radii.
Components MUST reference tokens, not literals. No CSS-in-JS runtime dependency is added.

**UI-FR-2: Header.** Shows the assistant mark (a violet circle with a simple "C" glyph, inline SVG),
the title "Caramel AI Tech Support" in one style, and a coral pill button labelled "New conversation".
The button shows a pencil or plus icon beside the text. It is disabled while the conversation is
empty. The header is sticky while the message list scrolls.

**UI-FR-3: Empty state.** With no messages, show the heading "Hi, how can we help?", the subline
"Ask about the API, errors, or rate limits.", and a "Suggested questions" heading. Below it, four
starter cards in a horizontal scroll row:
- "Why am I getting a 429 error?"
- "How do I stream a response?"
- "What counts toward my token limit?"
- "How do I choose a model?"

Each card is a button. Activating one sends the question exactly as if it had been typed. The
starters disappear after the first message and return after "New conversation". They are static text
in a frontend constants file, and they are questions inside the Principle IV scope.

**UI-FR-4: Messages.** Customer messages are plain text in a violet fill with white text, right aligned,
at most 85% of the column width. Assistant replies are rendered markdown on a white card (section 5.5).
Markdown MUST be styled for: paragraphs, lists, inline code, fenced code, blockquotes, links, and
tables. Headings inside replies are capped at the 20 px step so a reply cannot out-shout the header.
Raw HTML is still never rendered (existing guarantee).

**UI-FR-5: Code blocks.** Fenced code sits on the `--mist` surface inside the white card, in JetBrains
Mono, scrolls horizontally instead of wrapping, and has a "Copy" pill at its top right. After
activation the label changes to "Copied" for two seconds and the change is announced politely. Tables
scroll horizontally inside their own container on narrow screens.

**UI-FR-6: Links.** Links in replies are `--violet`, underlined, and open in a new tab with
`rel="noopener noreferrer"`.

**UI-FR-7: Scrolling.** While a reply streams, the list follows the newest text only if the user is
already at the bottom. If the user has scrolled up, the list does not move, and a "Jump to latest"
violet pill appears above the composer. Sending a message always scrolls to the bottom.

**UI-FR-8: Composer.** A white card above the bottom edge holds a textarea that auto-grows from 1 to
8 lines, then scrolls. Enter sends and Shift+Enter inserts a line break (existing). The card has a
visually hidden label "Your question", a placeholder "Ask about the API, errors, or rate limits", and
a visible hint "Enter to send, Shift+Enter for a new line", hidden below 480 px. The Send button is a
circular violet button with a paper-plane icon and the accessible name "Send". The textarea stays
editable while a reply streams, so the user can draft the next question; only Send is disabled until
the reply ends. Focus returns to the textarea when a reply finishes and after "New conversation".

**UI-FR-9: Length counter.** When the draft passes 80% of `MAX_MESSAGE_CHARS`, show "1,740 of 2,000"
in `--slate`. Over the limit, the counter and the inline note use the coral error treatment. Existing
validation rules and messages are unchanged.

**UI-FR-10: Errors.** The error notice sits inside the failed or cut-off reply card, on `--coral-tint`
with `--coral` text. It uses the existing friendly text and the existing "Try again" action, styled as
a coral outlined pill. Error codes and raw text are never shown (Principle V). Copy in
`friendlyMessages.ts` is unchanged in this release.

**UI-FR-11: Document chrome.** Keep `<title>`; add a `theme-color` meta matching `--mist`.

## 7. Interaction States

Each of these MUST be designed, not left to browser defaults:

| Surface | States |
|---|---|
| Buttons (Send, New conversation, Try again, Copy, starters, Jump to latest) | rest, hover, pressed, focus-visible, disabled |
| Composer | empty, typing, over-limit, disabled Send while streaming |
| Assistant card | waiting for first token, streaming, complete, failed, incomplete |
| Message list | empty, populated, scrolled up with "Jump to latest" |

Focus is shown by a 2 px `--violet` ring with a 2 px offset on every control. It is never removed.

## 8. Non-Functional Requirements

- **UI-NFR-1 Responsive:** usable at 360 px through desktop. No horizontal page scroll at any width;
  only starter cards, code blocks, and tables scroll sideways, and they do so inside their own
  containers. Input text is at least 16 px so mobile browsers do not zoom. The composer respects
  `env(safe-area-inset-bottom)` and uses `dvh` units so it stays visible when the mobile keyboard opens.
- **UI-NFR-2 Accessibility:** WCAG 2.1 AA. Text contrast at least 4.5:1, interactive boundaries and
  focus indicators at least 3:1. Verify every pair in section 5.2 with a contrast tool, not by eye.
  Fully keyboard operable. The message list stays `aria-live="polite"`. Errors keep `role="alert"`.
  Touch targets are at least 44 px. Streaming text MUST NOT reorder or reflow text the user has
  already read.
- **UI-NFR-3 Motion:** the streaming bar and the Copy confirmation are the only motion.
  `prefers-reduced-motion: reduce` replaces the moving bar with a static partial bar, and the Copy
  confirmation appears without animation.
- **UI-NFR-4 Performance:** added fonts and CSS total at most 200 KB gzip. Fonts use
  `font-display: swap`. The first-token latency target in `prd.md` section 7 MUST NOT regress.
- **UI-NFR-5 Self-contained:** no runtime requests to third-party origins. Fonts come from
  `@fontsource` packages bundled by Vite. Icons are inline SVG. `./scripts/check-bundle-secrets.sh dist`
  MUST still pass.
- **UI-NFR-6 Dependencies:** at most the two font packages are added. No icon library, no component
  library, and no CSS framework. A clipboard helper is added only if the native Clipboard API proves
  insufficient. Justify any other addition in the plan.

## 9. Compatibility With Existing Code and Tests

- Preserve these accessible names and roles so current tests keep passing: label "Your question",
  button "Send", button "New conversation", button "Try again", `role="alert"` on errors,
  `role="status"` with name "Assistant is typing", and `aria-live` on the message list. The Send and
  New conversation buttons use visible icons, so their names are set with `aria-label`.
- Any test that finds Send or New conversation by visible text rather than role or name MUST be
  checked after the icon change.
- Tests that assume the textarea is disabled while streaming MUST be updated for UI-FR-8, with the
  change noted in the plan.
- No changes to `useConversation`, `chatApi`, `sseParser`, or `types/chat.ts` except where UI-FR-7 or
  UI-FR-8 needs a small ref or prop. Any such change MUST be listed in the plan.
- New tests cover: starter questions send and hide; Copy and "Copied"; "Jump to latest" appears only
  when scrolled up; the length counter threshold; Send disabled but textarea editable while streaming.

## 10. Acceptance Criteria

1. `cd frontend && npm test` and `npm run build` pass.
2. Run in a real browser at 360, 768, and 1280 px, and capture each of: empty, mid-stream,
   complete with a code block, complete with a table, failed, incomplete, and a 2,000-character
   message. No clipped text, no page-level horizontal scroll.
3. Keyboard-only run-through: start a conversation from a starter, send a message, copy code, reach
   "Try again" and "New conversation" with a visible focus ring each time.
4. Contrast audit recorded in the plan for every token pair in section 5.2, with measured values.
5. With reduced motion enabled in the OS, no element moves on its own.
6. Network panel shows no third-party requests; the bundle secret check passes.
7. Failure states show only the friendly text, never an error code or raw message.
8. Side-by-side review against the reference image: lavender page, violet and white card pairing,
   pill chips, and the coral header action match its visual language. No text, data, artwork, or
   font from the reference appears in the app.

## 11. Open Questions

- **Brand assets:** does Caramel AI have a logo, palette, or typeface of its own? If so, the mark in
  UI-FR-2 and the tokens in section 5.2 are replaced and the plan is revised. The reference image
  sets the look for now.
- **Reference use:** confirm that the reference is used only as a visual guide, and that the client
  is comfortable with the design resembling it. No asset from it is reused.
- **Time pills:** the reference shows a time on each item. Showing a send time on each message is
  client-side only and needs no backend change. Add it, or leave it out?
- **Dark mode:** useful for developers, but doubles the contrast audit. The tokens are built so a
  second theme can be added later. Defer, or include in this pass?
- **Syntax highlighting:** adds a dependency and bundle weight. Leave plain until requested?
- **Starter questions:** should they send immediately (specified) or fill the composer for editing?
- **Font licensing:** confirm that Figtree and JetBrains Mono, both open-licensed, and the Latin-only
  subset are acceptable.

## 12. Follow-Up Steps

1. Add a one-line pointer to this document in `prd.md` section 6, item 5, since starter questions,
   Copy, and "Jump to latest" extend the listed chat UI elements (constitution: scope changes update
   `prd.md` first).
2. Run `/speckit-specify` with this PRD to create the feature spec, then `/speckit-plan`. The plan
   MUST record the design direction from section 5 (constitution, Principle VI) and the measured
   contrast values.
3. Implement with the `frontend-design` skill, then check against section 10.
