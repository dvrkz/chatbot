# Feature Specification: Chat Interface Visual and Interaction Refresh

**Feature Branch**: Not created (the repository has no git extension hook and is not under git)

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "Create spec in new folder 002-update-frontend inside specs using @prd-ui-2.md and @.specify/memory/constitution.md"

**Source documents**: `prd-ui-2.md` (UI design pass, the source for this spec) and
`.specify/memory/constitution.md` v1.2.0 (Principle VI, Intentional, Accessible UI Design; Principle V,
Fail Fast, Fail Friendly; Principle IV, Scope and Persona). Exact color values, type choices, and
measured contrast results live in `prd-ui-2.md` section 5 and are carried into the plan, not restated
here.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Start a conversation from the opening screen (Priority: P1)

A customer opens the chat, sees a friendly opening screen with suggested questions, and starts a
conversation either by choosing a suggestion or by typing their own question.

**Why this priority**: The first screen decides whether a customer in the middle of an incident keeps
going. Without it, the interface looks unfinished, which is the problem this release fixes.

**Independent Test**: Open the chat on a phone-sized screen with no prior messages. Tap one suggested
question and confirm it is sent and answered. Repeat by typing a question instead.

**Acceptance Scenarios**:

1. **Given** the chat has no messages, **When** the customer opens it, **Then** the screen shows a
   greeting, one line saying what the assistant can help with, and four suggested questions.
2. **Given** the opening screen, **When** the customer chooses a suggested question, **Then** that
   question is sent exactly as if typed, and the suggestions are hidden.
3. **Given** the opening screen, **When** the customer types a question and sends it, **Then** the
   question appears as their message and the reply begins to stream.

---

### User Story 2 - Read and act on a streamed reply (Priority: P1)

A customer reads a reply as it arrives, including code samples, lists, and tables, and copies a code
sample in one action.

**Why this priority**: The reason customers use the chat is to get an answer they can act on, such as
a corrected request or a command to run.

**Independent Test**: Ask a question whose answer includes a code block and a table. Confirm the text
appears progressively, the code can be copied with one action, and the table fits the screen.

**Acceptance Scenarios**:

1. **Given** a reply is streaming, **When** text arrives, **Then** the text appears progressively and
   a waiting indicator is visible until the reply ends.
2. **Given** a reply contains a code block, **When** the customer activates Copy, **Then** the code is
   copied and the control confirms "Copied" for about two seconds.
3. **Given** a reply contains a wide code line or a wide table, **When** the screen is narrow, **Then**
   only that block scrolls sideways and the rest of the page does not.
4. **Given** a reply contains a link, **When** the customer activates it, **Then** it opens in a new
   tab.

---

### User Story 3 - Stay in control while a reply streams (Priority: P1)

A customer who scrolls up to re-read an earlier part of a reply is not pulled back to the bottom. They
can return to the newest text with one action. A customer can also draft their next question while the
reply is still arriving.

**Why this priority**: Streaming replies that move the page under the reader make the chat hard to use
during an incident, and the draft-ahead behavior saves time.

**Independent Test**: Start a long streamed reply, scroll up during the stream, and confirm the view
holds still and a "Jump to latest" control appears. Type a draft while the reply streams.

**Acceptance Scenarios**:

1. **Given** the customer is at the bottom of the list, **When** a reply streams, **Then** the view
   follows the newest text.
2. **Given** the customer has scrolled up, **When** a reply streams, **Then** the view does not move and
   a "Jump to latest" control appears.
3. **Given** a reply is streaming, **When** the customer types, **Then** they can keep typing, but Send
   is unavailable until the reply ends.
4. **Given** the customer sends a message, **When** it is accepted, **Then** the view moves to the
   bottom.

---

### User Story 4 - Recover from a failed or cut-off reply (Priority: P2)

When a reply fails or stops partway, the customer sees a calm, plain-language explanation and can try
again without losing their place.

**Why this priority**: Failures happen during incidents, and customers must be able to recover without
reading technical detail.

**Independent Test**: Simulate a failed reply and a reply cut off mid-stream. Confirm the friendly text
and the Try again action appear, and that no error code or internal text is shown.

**Acceptance Scenarios**:

1. **Given** a reply fails, **When** the failure is shown, **Then** the message is the friendly text
   for that failure and a Try again action is offered, with no error code or internal detail.
2. **Given** a reply is cut off after some text arrived, **When** it stops, **Then** the partial text
   stays visible, is labelled as cut short, and offers Try again.
3. **Given** a message is blank or longer than 2,000 characters, **When** the customer tries to send it,
   **Then** it is not sent and a plain message explains why.

---

### User Story 5 - Use the chat on any screen and with any input method (Priority: P2)

A customer can use the whole chat from a 360-pixel phone screen up to a desktop, with a mouse, touch,
or keyboard alone, and with assistive technology.

**Why this priority**: Customers arrive on many devices, and a support tool that excludes some users
fails its purpose.

**Independent Test**: Complete a full run-through (start, send, copy, retry, new conversation) using only
the keyboard, then repeat on a 360-pixel-wide screen and a desktop-width screen.

**Acceptance Scenarios**:

1. **Given** a keyboard-only customer, **When** they move through the screen, **Then** every control is
   reachable and shows a visible focus indicator.
2. **Given** a screen reader user, **When** new reply text arrives, **Then** the new content is announced
   and failure messages are announced as alerts.
3. **Given** a customer with reduced motion enabled, **When** a reply is streaming, **Then** the waiting
   indicator is static and nothing else moves on its own.
4. **Given** a phone, **When** the on-screen keyboard opens, **Then** the message box stays visible above
   it.

---

### User Story 6 - Start over (Priority: P3)

A customer who has finished, or wants to change topic, starts a fresh conversation from the header.

**Why this priority**: It is a small but frequently used action that must be obvious.

**Independent Test**: Send two messages, activate New conversation, and confirm the screen returns to the
opening state with suggestions visible and the message box focused.

**Acceptance Scenarios**:

1. **Given** a conversation with messages, **When** the customer activates New conversation, **Then** the
   screen returns to the opening state with suggestions visible.
2. **Given** the conversation is empty, **When** the screen is shown, **Then** New conversation is
   unavailable.
3. **Given** a reply is still streaming, **When** the customer activates New conversation, **Then** the
   streaming reply stops and is discarded from the screen.

---

### Edge Cases

- A reply is cut off after a few words: the partial text stays readable and Try again is offered.
- A message is exactly 2,000 characters: it is accepted. At 2,001 characters it is refused with a clear
  message.
- A long unbroken code line or a very wide table on a 360-pixel screen: only that block scrolls sideways.
- The customer scrolls up just as a reply finishes: the view stays where they left it and "Jump to
  latest" remains available.
- The customer activates Send more than once quickly: the message is sent once.
- A reply fails after part of it is shown: the failure note is attached to that reply, and the partial
  text is kept.
- The customer turns on reduced motion mid-session: the waiting indicator becomes static without a page
  reload.
- The customer uses the keyboard to reach a suggested question, a Copy control, Jump to latest, or Try
  again: each is reachable in a logical order.

## Requirements *(mandatory)*

### Functional Requirements

**Conversation core (existing behavior, kept)**

- **FR-001**: Customers MUST be able to send a message by pressing Send or by pressing Enter. Shift+Enter
  MUST insert a new line instead of sending.
- **FR-002**: Assistant replies MUST appear progressively as they arrive.
- **FR-003**: Customers MUST be able to start a new conversation at any time from the header, which clears
  the visible conversation.
- **FR-004**: Failed or cut-off replies MUST show a friendly message and a Try again action. Error codes,
  stack traces, keys, and internal details MUST NOT be shown to the customer.
- **FR-005**: Blank messages and messages longer than 2,000 characters MUST NOT be sent. The customer MUST
  see a plain message explaining the reason.

**Opening screen**

- **FR-006**: With no messages, the screen MUST show a greeting, one line describing what the assistant
  helps with, and four suggested questions:
  - "Why am I getting a 429 error?"
  - "How do I stream a response?"
  - "What counts toward my token limit?"
  - "How do I choose a model?"
- **FR-007**: Choosing a suggested question MUST send it exactly as if the customer had typed it.
  Suggestions MUST disappear after the first message and return after New conversation.

**Messages and replies**

- **FR-008**: Customer messages and assistant replies MUST be distinguishable by position and shape, not by
  color alone.
- **FR-009**: Assistant replies MUST display paragraphs, lists, inline code, code blocks, quotations,
  links, and tables with clear formatting.
- **FR-010**: Code blocks MUST scroll sideways instead of wrapping, and MUST offer a Copy action. After a
  copy, the control MUST show "Copied" for about two seconds, and the change MUST be announced to assistive
  technology.
- **FR-011**: Links in replies MUST open in a new tab.
- **FR-012**: Tables MUST scroll inside their own area on narrow screens and MUST NOT widen the page.
- **FR-013**: Headings inside replies MUST NOT be larger than the screen's title, so a reply cannot
  overpower the page.

**Waiting and streaming**

- **FR-014**: From send until the first text arrives, the assistant's reply area MUST show a waiting
  indicator with the words "Looking into it". The indicator MUST remain visible while the reply streams
  and MUST disappear when the reply ends.
- **FR-015**: While a reply streams, the view MUST follow the newest text only when the customer is already
  at the bottom. Otherwise the view MUST NOT move, and a "Jump to latest" control MUST appear.
- **FR-016**: Sending a message MUST move the view to the newest message.
- **FR-017**: Streamed text MUST NOT shift text the customer is already reading.

**Message box**

- **FR-018**: The message box MUST grow with the text up to eight lines, then scroll.
- **FR-019**: While a reply streams, the customer MUST be able to keep typing. Send MUST be unavailable until
  the reply ends.
- **FR-020**: When a draft passes 80% of the 2,000-character limit, the screen MUST show a count such as
  "1,740 of 2,000". Over the limit, the count and the note MUST use the error treatment.
- **FR-021**: After a reply ends, and after New conversation, keyboard focus MUST return to the message box.

**Header**

- **FR-022**: The header MUST show the assistant's mark, the title "Caramel AI Tech Support", and the New
  conversation action. It MUST stay visible while the list scrolls. New conversation MUST be unavailable
  when the conversation is empty.

**Visual direction (follows the client reference, recorded in `prd-ui-2.md`)**

- **FR-023**: The screen MUST use a soft lavender-grey background, solid violet customer messages with white
  text, white rounded cards for assistant replies, and pill-shaped action controls.
- **FR-024**: The coral accent MUST be used only for the New conversation action and for error states, so
  that a coral element always means "attention" or "start over".
- **FR-025**: Colors, type sizes, spacing, and corner roundness MUST be defined once and applied consistently
  on every screen, so no screen uses its own one-off values.
- **FR-026**: The interface MUST use one readable typeface for text and a monospace typeface for code. Both
  MUST be available without loading anything from third-party sites.
- **FR-027**: No text, data, artwork, or font from the client reference image MUST appear in the product.

**Responsive layout and accessibility**

- **FR-028**: The whole chat MUST be usable from a 360-pixel-wide screen up to desktop widths. The page
  itself MUST NOT scroll sideways at any width. Only code blocks, tables, and the suggested-question row
  MAY scroll sideways, and only inside themselves.
- **FR-029**: Text and controls MUST meet WCAG 2.1 AA contrast. Every contrast ratio used MUST be measured,
  not estimated, before the work is reported complete.
- **FR-030**: Every control MUST be operable by keyboard alone and MUST show a visible focus indicator. The
  focus indicator MUST NOT be removed.
- **FR-031**: New reply content MUST be announced to assistive technology, and failure messages MUST be
  announced as alerts.
- **FR-032**: Motion MUST be limited to the waiting indicator and the Copy confirmation. When reduced motion
  is requested, the waiting indicator MUST be static and nothing else may move on its own.
- **FR-033**: Touch targets MUST be at least 44 pixels in each direction.
- **FR-034**: On a phone, typing in the message box MUST NOT cause the page to zoom, and the message box
  MUST stay visible above the on-screen keyboard.

**Scope and behavior guardrails**

- **FR-035**: This release MUST NOT add user login, account lookups, human agent handoff, voice input,
  multimodal input, or persistent chat history, including as placeholder controls or mockups.
- **FR-036**: This release MUST NOT change what the assistant answers, how it answers, or the way replies
  are produced. Only the presentation and interaction of the chat change.
- **FR-037**: The first streamed text MUST arrive within a few seconds under normal load, and the presentation
  MUST NOT slow that down.

### Key Entities

- **Message**: One item in the conversation. Has a sender (customer or assistant), text, and a status
  (waiting, streaming, complete, failed, or cut short). A failed or cut-short message also has a failure
  category that maps to friendly text.
- **Conversation**: The messages shown on screen since the customer last started a new conversation.
  Exists only for the current visit and is not saved between visits.
- **Suggested question**: One of the four fixed opening questions. Choosing it creates a customer message.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A customer can start a conversation from the opening screen with a single tap or click on a
  suggested question.
- **SC-002**: Every screen state listed in this specification (opening, streaming, complete with code,
  complete with a table, failed, cut short, and a message at the 2,000-character limit) is shown without
  clipped text or sideways page scrolling at 360, 768, and 1280 pixel widths.
- **SC-003**: A customer can complete a full run-through (start, send, copy code, try again, start a new
  conversation) using only the keyboard, and each step shows a visible focus indicator.
- **SC-004**: The contrast measurement covers 100% of the text and control color pairs, and every pair meets
  the WCAG 2.1 AA threshold for its use.
- **SC-005**: The time until the first text of a reply appears is unchanged from the current baseline under
  the same load.
- **SC-006**: In every failure test, 100% of customer-facing messages show only the friendly text, with no
  error code or internal detail.
- **SC-007**: Loading the chat makes no requests to third-party websites.
- **SC-008**: Reviewers comparing the chat with the client reference image agree that the background, the
  violet and white message pairing, the pill controls, and the coral header action follow it. Target: all
  reviewers agree.
- **SC-009**: All behaviors that passed before this release still pass, with the exception of the one
  deliberate change to whether the message box is disabled during streaming (FR-019).

## Assumptions

- **Source of design direction**: the look (lavender background, violet and white messages, pill controls,
  coral accent) comes from the client reference image, as recorded in `prd-ui-2.md`. Until the client
  supplies brand assets, that direction stands.
- **Reference use**: the client is comfortable with the design resembling the reference, and only its
  visual language is used.
- **Opening suggestions**: the four suggested questions send immediately when chosen, as specified in
  `prd-ui-2.md`. They are fixed text and are not personalized.
- **Time stamps**: the reference shows a time on each item. Per-message times are not part of this release,
  since they are an open question in `prd-ui-2.md`.
- **Dark mode, syntax highlighting, localization, and a custom logo**: out of scope for this release.
- **Font licensing**: the two chosen typefaces are open-licensed and their Latin character set is sufficient.
- **Backend and API**: unchanged. The presentation change uses the existing reply stream and error categories.
- **Error wording**: the existing friendly messages are kept unchanged in this release.
- **New conversation during a reply**: starting a new conversation while a reply streams stops that reply and
  removes it from the screen. This is a reasonable default that the plan may refine.
- **Session scope**: conversations are not saved between visits, consistent with the scope in `prd.md`.
- **Constitution**: this specification follows constitution v1.2.0. Principle VI requires the implementation
  plan to record the chosen direction and the measured contrast values.
- **Testing**: the existing automated tests remain the baseline. New tests cover the behaviors in the
  acceptance scenarios above.
