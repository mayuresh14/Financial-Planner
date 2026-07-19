# Finance Planner — Project Context

This file is read automatically by Claude Code at the start of every session
in this repo. It summarizes what this project is, what's been decided, and
where things stand — so you don't need to re-explain context each time.

## What this is

An Android app for financial goal planning, retirement/FIRE planning, and a
suite of India-specific investment calculators (SIP, PPF, EPF, SSY, NPS,
EMI, STP, SWP, etc.). Full details: `docs/finance-app-requirements.md` and
`docs/finance-app-technical-design.md`.

- **Frontend:** Android, Kotlin, Jetpack Compose, Material 3
- **Backend (Phase 2, not yet built):** Spring Boot + PostgreSQL
- **Market:** India — INR, lakh/crore formatting, 5 languages (English,
  Hindi, Marathi, Tamil, Telugu)

## Current status: Phase 1 complete

All 14 calculators are built and working, fully local (no backend, no
login required):

SIP, Lumpsum, SIP vs Lumpsum, Goal-based SIP, Tenure, PPF, EPF, SSY, NPS,
EMI (with prepayment feasibility), STP, SWP, Inflation-adjusted Goal, and
FIRE (4 variants: Traditional/Lean/Fat/Coast).

Also built: Goal Planning screen (calculate-only), theming (5 curated
presets + light/dark/system + randomize), full localization, home
dashboard grid, "Coming Soon" gating for account-dependent actions, a
3-step app tour, and a disclaimer banner.

**This has NOT yet been compiled/run by Claude (the chat assistant that
built it) — only by the user in Android Studio.** Two Kotlin 2.0 Gradle
issues were already found and fixed (missing Compose Compiler plugin,
deprecated `kotlinOptions` block). Treat any further build errors as
likely real issues to fix, not assumptions to second-guess.

## Architecture decisions worth knowing

- **MVVM + Repository pattern**, even though Phase 1 has no backend — this
  is the seam where Phase 2 swaps `GoalRepository`'s local-only
  implementation for one that calls the Spring Boot API, without touching
  ViewModels or UI.
- **Domain layer (`domain/model`, `domain/usecase`) is pure Kotlin**, no
  Android dependencies — keeps calculators unit-testable and portable.
- **`FinanceMath`** (`domain/util/FinanceMath.kt`) holds every shared
  formula (compound interest, SIP/lumpsum/PPF/SSY future value, EMI
  amortization, STP/SWP simulation, inflation adjustment). New calculators
  should reuse these before adding new formulas.
- **Calculators are local-only through Phase 2 as well** — no calculator
  API is planned; only goals/auth/preferences call the backend.
- **`FundingSource` on `Goal` is a single nullable field**, not a list —
  intentional seam for Phase 3's multi-funding-source upgrade.
- **NPS annuity split is a configurable input**, not hardcoded — PFRDA
  rules shift periodically.
- **Every screen follows the same pattern**: UseCase → ViewModel (sealed
  `ValidationError` type, `UiState` data class) → Compose screen (settings
  icon → `ThemeLanguageSheet`, back button, animated result card). Look at
  `ui/calculators/sip/` as the canonical example before building anything
  new.
- **All UI strings are resources**, translated in all 5 languages
  (`res/values*/strings.xml`) — never hardcode user-facing text. Check
  string count parity across languages after any addition.

## What's next (not yet started)

- Diagrams (architecture, ER, sequence flows) — deferred, not blocking
- **Phase 2**: Spring Boot backend, JWT auth, goal saving/tracking,
  WebSocket sync — full API contract already designed in
  `docs/finance-app-technical-design.md` §4
- Phase 3: analytics, feedback form backend, calculation history, security
  hardening, Play Store readiness

## Repo conventions

- Branches: `develop` (daily work) → `qa` → `main`
- This repo (Android) is separate from the backend repo, planned as
  `finance-planner-backend` once Phase 2 starts
