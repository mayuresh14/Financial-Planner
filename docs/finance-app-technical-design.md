# Personal Finance Planning App — Technical Design Document

**Version:** 1.0
**Companion to:** finance-app-requirements.md
**Platform:** Android (Kotlin + Jetpack Compose)
**Backend:** Spring Boot + PostgreSQL

---

## 1. Android App Architecture (Phase 1 Focus)

### Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3 / Material You)
- **Architecture pattern:** MVVM + Repository layer
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Local storage:** Jetpack DataStore (Preferences) for settings/theme/language

### Layered Structure

```
app/
├── ui/                     → Compose screens, organized by feature
│   ├── calculators/
│   │   ├── sip/
│   │   ├── lumpsum/
│   │   ├── emi/
│   │   ├── fire/           → 4 sub-variants share one screen shell + explainer content
│   │   └── ... (one package per calculator)
│   ├── goals/
│   ├── settings/
│   ├── onboarding/         → app tour / hints
│   └── common/             → shared components (result cards, charts, "Coming Soon" sheet)
│
├── domain/
│   ├── model/               → Goal, CalculatorInput, CalculatorResult, UserPreferences
│   └── usecase/              → one UseCase per calculator (e.g. CalculateSipUseCase) — pure Kotlin, no Android deps
│
├── data/
│   ├── local/                → DataStore repository (prefs, theme, language)
│   └── repository/           → CalculatorRepository, GoalRepository (local-only impl for Phase 1, swappable for Phase 2)
│
└── di/                        → Hilt modules
```

**Repository pattern rationale:** in Phase 2, `GoalRepository`'s local-only implementation gets swapped for one that calls the Spring Boot API — ViewModels and UI never need to change. This is the seam that makes Phase 1 → Phase 2 painless.

### Local Calculation Engine

- Each calculator has a `UseCase` (e.g. `CalculateSipUseCase`, `CalculateFireUseCase`) — pure functions, typed inputs/outputs, unit-testable
- Shared `FinanceMath` utility object: compound interest, present/future value, inflation adjustment, amortization schedule generator
- FIRE variants (Lean/Fat/Coast/Traditional) share one `CalculateFireUseCase` with a `FireVariant` enum parameter

### Navigation & "Coming Soon" Gating

- Single `NavHost` with all destinations registered upfront (calculators, goals, login, settings)
- Shared `requiresAccount: Boolean` flag on gated routes (goal save/track, calculation history)
- If user taps a gated feature while logged out → bottom sheet: *"This feature needs an account — coming soon"*
- Same gating mechanism becomes the real login-prompt trigger in Phase 2 — just flip the condition source

### Onboarding / App Tour

- `TourOverlay` composable — reusable spotlight/tooltip component driven by `TourStep(target, title, description)` list per screen
- Tour state (seen/not seen) persisted in DataStore, shown once per feature area

---

## 2. Domain Data Model

### Calculator Input/Result (pattern, illustrated with 3 examples)

```kotlin
// SIP
data class SipInput(
    val monthlyAmount: Double,
    val expectedReturnPercent: Double,
    val durationYears: Int,
    val stepUpPercent: Double? = null,
    val inflationPercent: Double? = null
)

data class SipResult(
    val maturityValue: Double,
    val totalInvested: Double,
    val wealthGained: Double,
    val inflationAdjustedValue: Double? = null
)

// FIRE (shared across 4 variants)
enum class FireVariant { TRADITIONAL, LEAN, FAT, COAST }

data class FireInput(
    val variant: FireVariant,
    val currentAge: Int,
    val retirementAge: Int,
    val lifeExpectancy: Int,
    val currentAnnualExpenses: Double,
    val inflationPercent: Double,
    val preRetirementReturnPercent: Double,
    val postRetirementReturnPercent: Double,
    val existingCorpus: Double = 0.0
)

data class FireResult(
    val requiredCorpus: Double,
    val monthlySipNeeded: Double,
    val isCoastFireAchieved: Boolean? = null,
    val explainerText: String
)

// EMI (most complex — prepayment feasibility)
data class EmiInput(
    val loanAmount: Double,
    val interestRatePercent: Double,
    val tenureMonths: Int,
    val prepayments: List<Prepayment> = emptyList()
)

data class Prepayment(
    val amount: Double,
    val afterMonth: Int,
    val isRecurring: Boolean,
    val strategy: PrepaymentStrategy // REDUCE_EMI or REDUCE_TENURE
)

data class EmiResult(
    val emi: Double,
    val totalInterest: Double,
    val amortizationSchedule: List<AmortizationEntry>,
    val interestSaved: Double? = null,
    val tenureReducedByMonths: Int? = null
)
```

### Goal

```kotlin
enum class GoalType { RETIREMENT, HOUSE, EDUCATION, CAR, CUSTOM }

data class Goal(
    val id: String,                 // UUID, client-generated in Phase 1, server-confirmed in Phase 2
    val userId: String?,             // null in Phase 1 (no accounts yet)
    val name: String,
    val type: GoalType,
    val targetAmount: Double,
    val targetDate: LocalDate,
    val currentAmount: Double = 0.0,
    val fundingSource: FundingSource?, // single source in v1
    val createdAt: Instant
)

data class FundingSource(
    val calculatorType: String,
    val calculatorInputSnapshot: String // serialized input, so the plan is reproducible later
)

data class GoalProgressEntry(
    val id: String,
    val goalId: String,
    val amount: Double,
    val note: String? = null,
    val loggedAt: Instant
)
```

### UserPreferences

```kotlin
data class UserPreferences(
    val defaultExpectedReturnPercent: Double = 12.0,
    val defaultInflationPercent: Double = 6.0,
    val defaultPostRetirementReturnPercent: Double = 7.0,
    val retirementAgeTarget: Int = 60,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    val hasCompletedOnboarding: Boolean = false,
    val hasSeenAppTour: Boolean = false
)
```

### Design Notes

- **`userId` nullable on `Goal`** — Phase 1 goals are local/anonymous; Phase 2 assigns a real `userId` on first sync after login.
- **`FundingSource` as its own class** (not flat fields) — Phase 3's multi-source upgrade just changes this to `List<FundingSource>` without touching the rest of `Goal`.
- **`calculatorInputSnapshot`** stores exact inputs used, so a saved goal's plan can be recalculated or edited later without losing original assumptions.

---

## 3. PostgreSQL Schema (Phase 2 Backend)

```sql
-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    consent_given_at TIMESTAMPTZ,           -- DPDP consent timestamp
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User preferences (1:1 with users)
CREATE TABLE user_preferences (
    user_id                             UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    default_expected_return_percent     NUMERIC(5,2) NOT NULL DEFAULT 12.0,
    default_inflation_percent           NUMERIC(5,2) NOT NULL DEFAULT 6.0,
    default_post_retirement_return_pct  NUMERIC(5,2) NOT NULL DEFAULT 7.0,
    retirement_age_target               SMALLINT NOT NULL DEFAULT 60,
    language                            VARCHAR(10) NOT NULL DEFAULT 'en',
    theme                               VARCHAR(20) NOT NULL DEFAULT 'SYSTEM_DEFAULT',
    date_of_birth                       DATE,
    has_completed_onboarding            BOOLEAN NOT NULL DEFAULT false,
    updated_at                          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Goals
CREATE TABLE goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    target_amount   NUMERIC(15,2) NOT NULL,
    target_date     DATE NOT NULL,
    current_amount  NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Funding source — separate table from day one, so Phase 3's multi-source
-- upgrade only requires dropping the unique constraint below.
CREATE TABLE goal_funding_sources (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id                     UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    calculator_type             VARCHAR(50) NOT NULL,
    calculator_input_snapshot   JSONB NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT one_source_per_goal_v1 UNIQUE (goal_id)   -- drop this in Phase 3
);

-- Manual progress logging
CREATE TABLE goal_progress_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id     UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    amount      NUMERIC(15,2) NOT NULL,
    note        VARCHAR(500),
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Reserved for Phase 3 — saved calculator history
CREATE TABLE calculator_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    calculator_type     VARCHAR(50) NOT NULL,
    input_snapshot      JSONB NOT NULL,
    result_snapshot     JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Feedback
CREATE TABLE feedback (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,  -- nullable: guest feedback allowed
    message     TEXT NOT NULL,
    app_version VARCHAR(20),
    device_info VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    emailed_at  TIMESTAMPTZ
);

-- Refresh tokens (auth)
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_progress_goal_id ON goal_progress_entries(goal_id);
CREATE INDEX idx_calc_history_user_id ON calculator_history(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

### Design Notes

- **`JSONB`** for input/result snapshots — calculator inputs vary widely in shape (SIP vs EMI vs FIRE), so JSONB keeps this flexible while still queryable.
- **`goal_funding_sources` as its own table** even though 1:1 in v1 — avoids an awkward migration when Phase 3 makes it 1:many.
- **`NUMERIC(15,2)`** for money fields — avoids floating-point rounding issues.
- **`refresh_tokens`** stored hashed, with revocation support for logout/password-change flows.

---

## 4. API Contract (Spring Boot REST + WebSocket)

### Auth

```
POST   /api/v1/auth/register        Body: { email, password }               → 201 { userId, accessToken, refreshToken }
POST   /api/v1/auth/login           Body: { email, password }               → 200 { userId, accessToken, refreshToken }
POST   /api/v1/auth/refresh         Body: { refreshToken }                  → 200 { accessToken, refreshToken }
POST   /api/v1/auth/logout          Body: { refreshToken }                  → 204
```
- Access token: short-lived JWT (~15 min)
- Refresh token: longer-lived (~30 days), stored hashed, revocable

### User Preferences

```
GET    /api/v1/preferences                                                  → 200 UserPreferences
PUT    /api/v1/preferences          Body: UserPreferences (partial)         → 200 UserPreferences
POST   /api/v1/onboarding/complete  Body: { dateOfBirth, defaultExpectedReturnPercent,
                                             defaultInflationPercent, retirementAgeTarget,
                                             riskAppetite }                  → 200 UserPreferences
```

### Goals

```
GET    /api/v1/goals                                                        → 200 [ Goal ]
POST   /api/v1/goals                Body: { name, type, targetAmount, targetDate,
                                             fundingSource }                 → 201 Goal
GET    /api/v1/goals/{goalId}                                               → 200 Goal (with fundingSource + progress summary)
PUT    /api/v1/goals/{goalId}       Body: { name?, targetAmount?, targetDate? } → 200 Goal
DELETE /api/v1/goals/{goalId}                                               → 204
POST   /api/v1/goals/{goalId}/progress  Body: { amount, note? }             → 201 GoalProgressEntry
GET    /api/v1/goals/{goalId}/progress                                      → 200 [ GoalProgressEntry ]
```

### Feedback

```
POST   /api/v1/feedback   (auth optional — guest feedback allowed)
Body:  { message, appVersion, deviceInfo }                                  → 201 { id, status: "received" }
```
Backend processes async and emails the developer (SES/SendGrid/JavaMail), updating `emailed_at` on success.

### WebSocket — Goal Sync

```
Endpoint: /ws/goals   (STOMP over WebSocket)
Auth: JWT in CONNECT frame header
Client subscribes to: /user/queue/goals/updates

Server pushes on: goal created/updated/deleted, progress entry added (from another device)

Message payload:
{
  "eventType": "GOAL_UPDATED" | "GOAL_DELETED" | "PROGRESS_ADDED",
  "goalId": "...",
  "payload": { ...Goal or GoalProgressEntry... },
  "timestamp": "..."
}
```
Fallback: app does a `GET /api/v1/goals` refresh on reconnect/app-resume in case a WS message was missed.

### Cross-Cutting API Notes

- JWT validated via Spring Security filter; expired token → `401` with a specific error code that triggers silent refresh client-side
- Standard error envelope: `{ "error": { "code": "...", "message": "...", "timestamp": "..." } }`
- Rate limiting on `/auth/login` (e.g. 5 attempts / 15 min per IP or email)
- **Calculators have no API in Phase 1/2** — computation stays local per the architecture decision; revisit only if a future web client needs shared calculation logic

---

## 5. Cross-Cutting Implementation Details

### Localization

- Standard Android resource qualifiers: `values/` (English default), `values-hi/`, `values-mr/`, `values-ta/`, `values-te/`
- All calculator labels, result text, FIRE explainer copy, and error messages via string resources
- Indian digit-grouping (lakh/crore) number formatting, independent of selected language
- Language selector in Settings; applied via `AppCompatDelegate.setApplicationLocales()` (no activity restart needed on Android 13+, compat path for older versions)

### Theming

- Material 3 with 4–5 curated `ColorScheme` presets + Light/Dark/System-default toggle (orthogonal to color preset)
- "Randomize theme" — picks a random preset from the curated set on trigger
- Persisted in DataStore, applied via `CompositionLocalProvider` at the app root

### Security Implementation

- **Token storage:** `EncryptedSharedPreferences` (Android Keystore-backed) — never plain `SharedPreferences`
- **Transport:** OkHttp with certificate pinning for the production API domain; TLS 1.2+ enforced
- **Password handling:** never stored/logged client-side beyond the in-memory login form field; backend hashes with bcrypt (cost 12) or Argon2id
- **Session:** silent token refresh via OkHttp `Authenticator` on `401`; forced logout if refresh token itself is expired/revoked
- **Auto-logout:** configurable inactivity timeout (e.g. 15 min)
- **Sensitive screens:** `FLAG_SECURE` on goal/financial detail screens (blocks screenshots/recording)
- **Logging:** ProGuard/R8 strips debug logs in release builds; no financial figures or tokens ever logged
- **DPDP consent:** explicit consent checkbox at registration, timestamped in `users.consent_given_at`
- **Data Safety disclosure:** maintained as a checklist against actual data collected, reviewed before each Play Store submission

---

## Next Steps

This document, together with `finance-app-requirements.md`, is sufficient to begin Phase 1 scaffolding (Android project setup, calculator UseCases, and Compose UI).
