# Measurements: Chat Interface Refresh

**Feature**: `002-update-frontend` | **Tasks**: T001 to T004 (Phase 1, Setup) | **Date**: 2026-11-14

This file records the baseline taken before any UI file changed. Later tasks append their results here.

## T001: First-token latency baseline (SC-005, quickstart step 9)

**Method**: ten fresh sessions, one per question, posted to `POST /api/chat/stream` on the local backend.
Time from request to the first `token` event. Parsed SSE lines by `event:` name, since the server writes
`event:token` with no space after the colon.

**Environment**: backend started from `backend/` with `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`
(port 8080 is held by an unrelated Jenkins process, so the backend was moved to 8081). The model is
`gemma-4-26b-a4b-it` on Google AI Studio, using the key in `.env`.

**Scope note**: this measures the server path. The UI-side measure (Send click to first rendered text in
the browser) is taken in T058, on the same backend and questions.

**Results**: see the table below. The first attempt used an incorrect SSE parser (it matched `event: token`
with a space) and recorded no tokens. Those numbers are discarded and are not part of this baseline.

| # | First token | Full reply | End event |
|---|-------------|------------|-----------|
| 1 | 2.46 s | 11.64 s | done |
| 2 | 2.50 s | 8.74 s | done |
| 3 | 4.56 s | 14.21 s | done |
| 4 | 2.23 s | 10.71 s | done |
| 5 | 2.23 s | 13.11 s | done |
| 6 | 2.31 s | 9.37 s | done |
| 7 | 2.43 s | 12.62 s | done |
| 8 | 2.44 s | 18.24 s | done |
| 9 | 2.21 s | 9.83 s | done |
| 10 | 2.33 s | 12.65 s | done |

**Summary**: n = 10. **Median first token 2.38 s.** Min 2.21 s, max 4.56 s. Every reply ended with `done`.
No `error` events. The median is inside the "within a few seconds" target (FR-037, SC-001).

**Regression rule** (T058): the UI-side median after the change must not exceed this baseline's median by
more than measurement noise. Record the comparison there.

## T002: Baseline test and build

| Check | Command | Result |
|-------|---------|--------|
| Unit and component tests | `cd frontend && npm test` | 6 of 6 files, 21 of 21 tests passed (before any UI change) |
| Production build | `cd frontend && npm run build` | Passed. `dist/assets/index-BInALeal.js` 351.80 kB, 109.20 kB gzip |

**Bundle budget**: UI-NFR-4 allows fonts and CSS to add at most 200 KB gzip. The baseline JavaScript is
109.20 kB gzip, with no CSS file yet.

## T003: Gate G-5

Closed in `plan.md` on 2026-11-14. The constitution was amended to v1.3.0: the `prd.md` link requirement is
removed. Companion PRDs must reference `prd.md` and must not expand scope.

## T004: Font packages installed

| Package | Version | Verified |
|---------|---------|----------|
| `@fontsource-variable/figtree` | 5.3.0 | `npm ls` lists it at the top level |
| `@fontsource-variable/jetbrains-mono` | 5.3.0 | `npm ls` lists it at the top level |

- The planned path `…/latin.css` does not exist. The packages expose `wght.css`, which declares
  `font-display: swap` and a `unicode-range` per subset. Tasks T008 and research R2 are updated to use it.
- `npm install` reported existing audit findings in the dependency tree. Nothing was auto-fixed,
  because `npm audit fix --force` includes breaking changes. Review them separately from this feature.

## Implementation and verification results (`/speckit-implement`, 2026-11-14)

**Unit and component tests**: 48 of 49 pass. 16 test files, including the 10 new suites. The one failure is
the GFM table check in `frontend/tests/markdownReply.test.tsx`, described below.
**Type check**: `tsc -b` passes. **Production build**: passes. The bundle is 111.11 kB gzip of JavaScript,
the CSS is 5.25 kB gzip, and the three self-hosted woff2 font files are bundled.
**Bundle secrets**: `check-bundle-secrets.sh dist` passes.
**Token discipline (T050)**: no hex literal outside `styles/app.css`, and no `lilac` reference in `src/`.

### Open blocker: GFM tables (FR-009, UI-FR-4)

`react-markdown` does not parse GFM tables without `remark-gfm`. The table mapping (`.table-scroll`) is
implemented, but tables render as plain text until the plugin is added. The install of `remark-gfm` was
**not permitted** in this session (permission denied by the environment's auto-mode check), so it was not
installed. This is an exception to UI-NFR-6 (at most the two font packages). Decision needed: approve the
`remark-gfm` install (the plan then records it as a justified exception), or accept the gap for this
release. Until one of those, T018 stays open.

### Browser verification (Chrome, real backend on port 8081, Vite dev server on port 5173)

| Check | Result |
|-------|--------|
| Empty state at 360, 768, 1280 px: page horizontal overflow | 0 px at all three widths (SC-002, partial) |
| Network during empty state | Only `localhost:5173` requested (SC-007, partial) |
| Keyboard focus ring on every control tabbed to | 2 px solid `#6A3DF0` ring on starters, textarea, and Send. The page body appears in the cycle with no ring, which is expected at the wrap-around. |
| Reduced motion while streaming | Streaming bar animation is `none` (FR-032, passes) |
| Rendered contrast: customer text on violet | 5.93:1, passes 4.5 |
| Rendered contrast: reply text on white | 16.6:1, passes 4.5 |
| Rendered contrast: New conversation label on coral | 6.45:1, passes 4.5 |
| Header at 360 px | Title on one line after the phone layout fix |
| Console errors | One 404 for `/favicon.ico`. The app has no favicon. Harmless, but add one before release. |

Screenshots are in `screenshots/`: `empty-360.png`, `empty-768.png`, `empty-1280.png`, `streaming-360.png`,
and `complete-360.png`.

**Not captured**: failed, cut-short, code-block, table, and 2,000-character states (SC-002). The
failed-state UI is covered by unit tests, but the screenshots were not taken.

**Blocked: UI-side latency (T058)**. The backend began timing out on model calls during the browser run
(`REPLY_TIMEOUT` in the backend log, and a probe got no tokens in 55 s). No UI-side median can be taken
until the model responds again. Rerun the latency stage in `/tmp/ui-verify/verify.cjs` (stage 5) then.
The server-side baseline above (median 2.38 s) is valid.

**Not run by the agent**: the screen reader spot check (quickstart step 12), the reference side-by-side
review (SC-008), and the full contrast audit with a standalone contrast tool. These need a person.

### Test setup change

`frontend/tests/setup.ts` now calls RTL `cleanup()` after each test. Vitest runs without globals, so
React Testing Library cannot register cleanup itself. Without this, rendered trees leaked between tests.

## Observations for later phases

- **Model output in replies**: the raw stream for "How do I set the temperature?" begins with the
  model's planning notes ("User asks…", "Scope check…") before the answer. This is the known issue in
  `CLAUDE.md` (gemma writes planning notes into the reply text). It is a backend and model problem, not a
  presentation problem, but it strongly affects how the reply reads. Record it in the review. Do not fix it
  in this UI feature (FR-036 keeps backend behavior unchanged).
- **Port 8080**: a Jenkins process holds it. The project's default backend port is in use, so local runs
  need `VITE_API_BASE_URL` pointing at the backend's port.
