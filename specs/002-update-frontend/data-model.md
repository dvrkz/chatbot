# Data Model: Chat Interface Visual and Interaction Refresh

**Feature**: `002-update-frontend` | **Date**: 2026-11-14 | **Plan**: [plan.md](./plan.md)

This feature stores nothing. The backend data model in
`specs/001-tech-support-chat/data-model.md` is unchanged. The entities below are UI state and constants
that the presentation needs. They exist only in memory, for the current visit.

## 1. Message (existing, unchanged)

Source: `frontend/src/types/chat.ts`. The type is not changed.

| Field | Type | Rule |
|-------|------|------|
| `id` | string | Unique within the visit |
| `sender` | `'customer' \| 'assistant'` | Customer messages render as violet bubbles; assistant messages as white cards |
| `text` | string | Customer text is plain. Assistant text is rendered as markdown with raw HTML disabled |
| `status` | `'complete' \| 'streaming' \| 'failed' \| 'incomplete'` | Drives the waiting bar, the failure notice, and the Try again action |
| `errorCode` | `ErrorCode` (optional) | Present only for `failed` and `incomplete`. Mapped to friendly text; never shown raw |

**Visual state mapping** (presentation only):

| Status | Assistant card shows |
|--------|----------------------|
| `streaming` with empty text | Progress bar and "Looking into it" |
| `streaming` with text | Text, and the progress bar on the top edge |
| `complete` | Text, no bar, Copy controls on code blocks |
| `failed` | No text. Coral notice with friendly text and Try again |
| `incomplete` | Partial text, a "Reply was cut short" prefix in the notice, and Try again |

## 2. ConversationView (new, transient UI state)

Owned by `App.tsx` and `useStickToBottom`. Never sent anywhere.

| Field | Type | Rule |
|-------|------|------|
| `pinnedToBottom` | boolean | True when the list is within 48 px of its bottom. Set by scroll events |
| `showJumpToLatest` | boolean | Derived: `!pinnedToBottom && messages.length > 0 && a reply is streaming or has changed since the customer last scrolled` |
| `draft` | string | Lives in `MessageInput`. Preserved while a reply streams |
| `draftCount` | number | Derived from `draft.trim().length`. Shown when `draftCount >= 0.8 * MAX_MESSAGE_CHARS` |
| `copiedBlockId` | string or null | The code block that shows "Copied". Cleared after two seconds |
| `focusRequest` | counter | Incremented when the composer must take focus (reply ended, New conversation) |

**Transitions**:
- Send → `pinnedToBottom = true`, scroll to bottom.
- Scroll up by more than 48 px → `pinnedToBottom = false`; `showJumpToLatest` may become true.
- Jump to latest activated → scroll to bottom, `pinnedToBottom = true`, `showJumpToLatest = false`.
- Reply ends → `focusRequest` incremented.
- New conversation → messages cleared, `pinnedToBottom = true`, `focusRequest` incremented.

## 3. SuggestedQuestion (new constant)

Source: `frontend/src/content/starterQuestions.ts`. Fixed text, no backend source.

| Field | Type | Rule |
|-------|------|------|
| `id` | string | Stable key, for example `rate-limit-429` |
| `text` | string | One of the four questions in `spec.md` FR-006. Each is inside the Principle IV scope |

**Validation** (test): exactly four entries; each text is non-empty and at most `MAX_MESSAGE_CHARS`; each is
inside the Principle IV scope (checked by review, recorded in the test file header).

## 4. DesignToken (new, defines the visual system)

Source: `frontend/src/styles/app.css`, as custom properties on `:root`.

| Token | Value | Used for | Measured ratio (see plan) |
|-------|-------|----------|---------------------------|
| `--mist` | `#ECEBF6` | Page background, code surface | n/a |
| `--paper` | `#FFFFFF` | Assistant cards, composer, starter cards | n/a |
| `--violet` | `#6A3DF0` | Customer fill, Send, links, focus ring, outlined chip border | 5.02 on mist |
| `--violet-deep` | `#4F26C9` | Hover and pressed violet fills | 8.56 with white |
| `--ink` | `#1F1B33` | Body and reply text | 16.63 on paper |
| `--slate` | `#5F5A7A` | Secondary text, placeholder, counter | 5.50 on mist |
| `--coral` | `#B42340` | New conversation fill, error text, Try again outline | 6.45 on paper |
| `--coral-tint` | `#FBE9EE` | Error notice background | n/a |
| `--lilac-line` | (removed) | Was outlined chip border; fails at 1.17:1 | n/a |

**Rules**: components reference tokens, never literals (FR-025). Radii are also tokens: 20 px for cards and
customer messages, 12 px for code and notices, 16 px for the composer, full pill for chips and the header
button. Spacing and type-scale steps are tokens too.

## 5. Validation summary (for tests)

- Each assistant card renders the friendly text and never a raw code (existing guarantee; extended to the
  new notice).
- The composer is enabled while `status === 'streaming'`; Send is disabled in that state.
- The counter appears at 80% of `MAX_MESSAGE_CHARS` and uses the coral treatment above the limit.
- Focus moves to the composer after a reply ends and after New conversation.
