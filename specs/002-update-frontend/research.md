# Research: Chat Interface Visual and Interaction Refresh

**Feature**: `002-update-frontend` | **Date**: 2026-11-14 | **Plan**: [plan.md](./plan.md)

Each decision lists what was chosen, why, and what was rejected. Where a fact depends on a library version
or browser release, it is marked **verify on install**.

## R1. Palette and contrast (measured)

**Decision**: Use the palette in `plan.md`. Remove `--lilac-line`. Outlined controls use `--violet`.

**Method**: WCAG 2.1 relative luminance and contrast ratio, computed from the hex values in
`prd-ui-2.md` section 5.2 with a short script (no dependency). Ratios are rounded to two decimals.
The browser re-check is part of the quickstart, so the measured numbers are confirmed on the built CSS.

**Results that changed the design**:
- `--lilac-line` (`#DCD9EE`) on `--mist` measures **1.17:1**. It fails the 3:1 boundary rule that
  applies when a border is the only sign of a control (WCAG 1.4.11). It was specified for outlined chip
  borders, so it is replaced by `--violet` (5.02:1 on `--mist`). It is not kept as a decorative hairline,
  because the PRD does not need one.
- `--coral` on `--coral-tint` measures **5.53:1**. The PRD estimated 5.8:1. Still a pass; the PRD value
  must be corrected (Gate G-6).

**Rejected**: keeping `--lilac-line` as a decorative hairline. Hairlines still need review and add a token
that does not carry meaning.

## R2. Font delivery

**Decision**: Two packages, `@fontsource-variable/figtree` and `@fontsource-variable/jetbrains-mono` (v5.3.0,
installed 2026-11-14), imported in `main.tsx` through `wght.css`. Vite bundles the woff2 files, so no
request leaves the origin. `font-display: swap` is set by the package's CSS.

**Verified on install**: both package names resolve. There is no per-subset CSS file (`latin.css` does not
exist). `wght.css` declares one `@font-face` per subset with a `unicode-range`, so Latin text fetches only
the Latin woff2. Each package has Latin and Latin-extended files.

**Rationale**: Variable fonts give the 400, 600, and 700 weights from one file. Bundling satisfies
constitution Design assets and UI-NFR-5.

**Rejected**:
- Static per-weight packages: more files and more bytes for the same three weights.
- Google Fonts `<link>`: a third-party request, which breaks the constitution rule and SC-007.
- System font stack: loses the chosen character and the reference's friendly type.

**Risk**: If a package name differs, the fallback is the same family from another official fontsource
package. The chosen typefaces do not change.

## R3. One stylesheet, tokens, and flat specificity

**Decision**: A single `src/styles/app.css`. Tokens are CSS custom properties on `:root`. Every component
rule is a single class selector. Element selectors are used only inside `.reply` (the markdown content
scope), for paragraphs, lists, tables, and blockquotes, so markdown needs no per-element classes.

**Rationale**: UI-FR-1 requires one global stylesheet and tokens, not literals. Flat selectors avoid the
margin-and-padding cancellation the design skill warns about.

**Rejected**: CSS Modules or CSS-in-JS. Both add build or runtime dependencies that UI-NFR-6 excludes.

## R4. Scroll-follow during streaming

**Decision**: A hook, `useStickToBottom`, keeps a "pinned" flag. The flag is true when the list is within
48 px of its bottom. While pinned and streaming, the list scrolls to the bottom on each change. When the
customer scrolls up, the flag clears and the list does not move; a "Jump to latest" pill appears. Sending
a message always pins and scrolls to the bottom.

**Rationale**: FR-015 and FR-016. The 48 px threshold absorbs sub-pixel and momentum scroll.

**Rejected**: A scroll library (not in the dependency budget). Auto-scrolling on every token without the
pinned check (moves text the customer is reading, violating FR-017).

**Testing**: jsdom has no layout, so the hook takes scroll metrics from properties the test sets
(`scrollTop`, `scrollHeight`, `clientHeight`).

## R5. Composer auto-grow

**Decision**: JavaScript sizes the textarea height from `scrollHeight` after each change, capped at eight
lines, then the textarea scrolls. Line height and the cap come from the tokens.

**Rationale**: The CSS `field-sizing: content` property is not supported in every evergreen browser, so
relying on it would leave some users with a fixed-height box. **Verify on install** whether support is
broad enough to revisit; if so, the JavaScript path can be removed later.

**Rejected**: A fixed three-line textarea (fails FR-018).

## R6. Copy with a fallback and a live confirmation

**Decision**: `utils/copyText.ts` uses `navigator.clipboard.writeText` when it exists. Otherwise it copies
through a temporary off-screen textarea and `document.execCommand('copy')`. The `CodeBlock` button shows
"Copied" for two seconds. The confirmation is also written to a visually hidden `role="status"` region, so
a screen reader announces it. The timer is cleared on unmount.

**Rationale**: `navigator.clipboard` exists only in secure contexts, so a page served over plain HTTP on a
LAN address lacks it. FR-010 requires copy to work, and the fallback keeps it working with no dependency
(UI-NFR-6 allows this helper).

**Rejected**: A clipboard library (adds a dependency the PRD says to avoid for this case).

## R7. Markdown styling with raw HTML still disabled

**Decision**: Keep `react-markdown` without `rehype-raw`, so raw HTML stays escaped (existing guarantee).
Supply a `components` map: `a` (new tab, `rel="noopener noreferrer"`), `pre` (wrapped in `CodeBlock`),
`table` (wrapped in a horizontally scrolling container), and `h1`-`h3` (rendered at the 20 px step, so a
reply cannot out-shout the header).

**Rationale**: The map changes presentation only. The security property comes from not adding `rehype-raw`,
which is already the case.

## R8. Motion and reduced motion

**Decision**: The streaming bar animates with a transform on a pseudo-element inside `.typing-bar`. Under
`prefers-reduced-motion: reduce` the animation is removed and the bar shows a static partial fill. The copy
confirmation fades only when motion is allowed, otherwise it appears at once. No other element has an
animation, and no transition is set on load.

**Rationale**: FR-032 and UI-NFR-3. A transform animation stays off the layout path, which protects the
first-token latency target (constitution Principle VI).

## R9. Focus management

**Decision**: The composer textarea has a ref. Focus moves to it after a reply ends, after New
conversation, and on load. Focus rings use `:focus-visible` with a 2 px `--violet` outline and a 2 px
offset. Removing focus outlines is not allowed.

**Rationale**: FR-021 and FR-030.

## R10. Screen reader announcements while streaming

**Decision**: Keep `aria-live="polite"` on the message list. While a reply streams, set `aria-busy="true"`
on the list, which holds announcements back. When the reply ends, the completed reply is announced once.
Failure notices keep `role="alert"`.

**Rationale**: Without this, a polite live region announces every streamed token, which makes the list
unusable with a screen reader. FR-031 is met by announcing the complete reply.

**Risk**: Behavior differs across screen readers. The quickstart includes a manual check with one desktop
and one mobile screen reader, and this decision is revised if the check fails.

## R11. Mobile layout and the on-screen keyboard

**Decision**: Use `100dvh` for the page height. The composer sits in the normal flow at the bottom of the
column, with `padding-bottom: env(safe-area-inset-bottom)`. Input font size is 16 px so iOS does not zoom.

**Rationale**: FR-034 and UI-NFR-1. `dvh` tracks the visible viewport when the keyboard opens, which `vh`
does not.

## R12. Starter cards

**Decision**: A horizontal row using `overflow-x: auto` and `scroll-snap-type: x mandatory`. Each card is a
`<button>`, so it is keyboard reachable and has a name. Activating a card calls the same `send` as typing.

**Rationale**: FR-006, FR-007, and FR-028. The row is the only place a control scrolls sideways besides code
and tables, as FR-028 permits.

## R13. Icons

**Decision**: Inline SVG for the header mark, the pencil or plus icon, and the paper-plane Send icon. Each
SVG is `aria-hidden`, and the button carries the visible or accessible name.

**Rationale**: No icon library (UI-NFR-6). Three icons do not justify a dependency.

## Open items carried to implementation

1. **Verify font package names and the Latin subset path on install** (R2).
2. **Confirm screen reader behavior** on one desktop and one mobile reader (R10).
3. **Re-measure contrast in the browser** on the built CSS and record the result in the quickstart report
   (R1). The computed values here are the design check; the browser check is the Principle VI check.
4. **First-token latency baseline** is captured before the first UI change lands, so regressions are
   measured, not estimated (quickstart step 9).
