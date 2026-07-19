# Personal Finance Planning App — Requirements Document

**Version:** 1.0 (Locked)
**Platform:** Android
**Backend:** Spring Boot + PostgreSQL
**Market:** India (INR)

---

## 1. Overview

An Android application for financial goal planning, retirement/FIRE planning, and a comprehensive suite of investment calculators, tailored to Indian financial instruments (SIP, PPF, EPF, SSY, NPS). The app is designed to work fully offline for its core calculators, with an optional backend for account creation, goal saving/tracking, and cross-device sync.

---

## 2. Guiding Principles

- **Local-first for calculators.** No login or network required to use any calculator.
- **Progressive disclosure of login.** Full navigation is visible from day one; features that need an account show a "Coming Soon" / "Sign in to unlock" state rather than being hidden, so users always know what's available.
- **India-specific by default.** INR formatting (lakh/crore), Indian financial instruments, regional languages.
- **Not investment advice.** All outputs are projections based on user-supplied assumptions, with a disclaimer.

---

## 3. Functional Requirements

### 3.1 Calculators (all local-computable, no login required)

| # | Calculator | Key Inputs | Key Outputs |
|---|---|---|---|
| 1 | SIP | Monthly amount, expected return %, duration, step-up % (optional), inflation % (optional) | Maturity value, invested amount, wealth gained, inflation-adjusted value |
| 2 | Lumpsum | Principal, expected return %, duration, inflation % (optional) | Maturity value, inflation-adjusted value |
| 3 | SIP vs Lumpsum Comparator | Both sets of inputs | Side-by-side comparison, chart |
| 4 | Goal-based Reverse SIP | Target amount, duration, expected return %, inflation % | Required monthly SIP |
| 5 | Tenure Calculator | Target amount, monthly investment, expected return % | Time required to reach goal |
| 6 | PPF | Yearly contribution, duration, current rate | Maturity value |
| 7 | EPF | Basic salary, contribution %, duration, expected return | Maturity value |
| 8 | Sukanya Samriddhi Yojana (SSY) | Girl's age at opening, yearly deposit | Maturity value; enforces eligibility rules (account opens only before girl turns 10; matures 21 years from opening or on marriage after 18) |
| 9 | NPS | Monthly/yearly contribution (self only), current age, expected return, annuity % (configurable, not hardcoded) | Corpus at 60, lump-sum withdrawal, annuity portion, estimated monthly pension |
| 10 | EMI (with prepayment feasibility) | Loan amount, rate, tenure; optional one-time or recurring prepayments | EMI, amortization schedule, interest/tenure saved from prepayment, "reduce EMI vs reduce tenure" comparison |
| 11 | STP | Source lumpsum, transfer amount/frequency, source & target returns, duration | Value in target fund, value remaining in source |
| 12 | SWP | Initial corpus, withdrawal amount/frequency, expected return | Depletion timeline, total withdrawn, final balance |
| 13 | Inflation-adjusted Goal | Current cost, inflation %, years | Future cost |
| 14 | Retirement / FIRE (4 variants) | Current age, retirement age, life expectancy, expenses, inflation, pre/post-retirement return, existing corpus | Required corpus, monthly SIP needed. Variants: **Traditional FIRE** (25x expenses, 4% SWR), **Lean FIRE** (bare-minimum expenses), **Fat FIRE** (comfortable/luxury lifestyle), **Coast FIRE** (existing corpus alone will grow to target by retirement age). Each variant includes an in-app explainer of what it means. |

Additional tools layered on SIP calculator:
- **SIP Comparison** — compare 2+ hypothetical SIP scenarios side by side
- **SIP Analysis** — analyze a hypothetical SIP's projected performance (not tied to actual tracked investments, for now)

### 3.2 Goal Planning

- Users (including guests) can create and calculate a goal plan: name, type, target amount, target date, linked calculator result
- **v1: one funding source per goal.** Data model should allow extending to multiple funding sources (e.g. SIP + lumpsum feeding one goal) in a later phase without a schema rework
- Saving a goal or logging progress requires login
- Manual progress logging only (no external NAV/price feed integration for now)
- Dashboard: % progress, projected completion date, shortfall/surplus vs. plan

### 3.3 User Account

- Register/login via email + password, JWT-based auth
- Post-login onboarding: collect age/DOB, default expected return %, default inflation %, retirement age target, risk appetite — so future forms/calculators need minimal re-entry. Skippable but nudged.
- Settings: edit default return %/inflation % anytime; these prefill calculators but remain editable per use
- Saved calculation history — **Phase 3 (deferred but high priority)**

### 3.4 Localization

- Languages at launch: **English, Hindi, Marathi, Tamil, Telugu**
- Numbers formatted in lakh/crore regardless of language

### 3.5 Theming

- Light, Dark, System-default
- Multiple curated color themes
- "Randomize theme" option

### 3.6 Onboarding / App Tour

- First-launch app tour or contextual hints highlighting key features (calculators, goal planning, settings)

### 3.7 UI/UX Standards

- Material Design (Material 3 / Material You): elevated cards, ripple/click effects, rounded corners, shadows
- Animations: screen transitions, number count-up on results, progress bar fills

### 3.8 Analytics

- Firebase Analytics for usage tracking

### 3.9 Feedback

- In-app feedback form → backend service → emails developer (not a plain `mailto:` intent)

### 3.10 Security & Compliance

- DPDP Act 2023 alignment: explicit consent for data collection, purpose limitation, right to erasure
- Password hashing (bcrypt/argon2), short-lived JWT + refresh tokens, login rate-limiting
- HTTPS/TLS everywhere; certificate pinning in-app
- Encryption for sensitive fields at rest in PostgreSQL
- No sensitive data in logs; secure token storage via Android Keystore/EncryptedSharedPreferences
- Privacy Policy & Terms of Service (required for Play Store)
- Accurate Play Store Data Safety disclosure (including Firebase Analytics data collection)
- Session auto-logout after inactivity
- In-app disclaimer: projections only, not investment advice (SEBI advisory-rule ambiguity avoidance)

---

## 4. Phased Delivery Plan

### **Phase 1 — Local-Only MVP (no backend dependency)** ✅ COMPLETE
Goal: fully usable, offline-capable app that demonstrates the complete feature set.

- All 14 calculators + SIP comparison/analysis tools, computed **locally in-app** (Kotlin)
- Full navigation/menu visible upfront, including goal planning, login, and settings — features requiring backend show a **"Coming Soon"** state if tapped
- Goal planning UI works locally in calculate-only mode (no save/track yet)
- Multi-language support (5 languages)
- Theming (light/dark/custom/random)
- Material Design UI with animations
- App tour / onboarding hints
- In-app disclaimer

*Explicitly out of scope for Phase 1: login, goal saving/tracking, backend calls, WebSocket sync, analytics, feedback form, default-preferences prefill.*

### **Phase 2 — Backend & Accounts**
Goal: bring the backend online and unlock account-dependent features.

- Spring Boot + PostgreSQL backend stood up
- JWT auth: register/login
- Post-login onboarding (defaults for return %/inflation %)
- Goal saving & manual progress tracking (single funding source per goal)
- Goals dashboard with progress tracking
- WebSocket sync for goals across devices
- Migrate "Coming Soon" placeholders to live functionality

### **Phase 3 — Polish, Compliance & Scale**
Goal: production-readiness and deferred high-priority items.

- Saved calculation history for logged-in users
- Firebase Analytics integration
- In-app feedback form wired to backend email service
- Full security/compliance hardening (encryption at rest, cert pinning, DPDP consent flows, Play Store Data Safety disclosures)
- Multi-funding-source support per goal (extending Phase 2's single-source model)
- Privacy Policy & Terms of Service finalized for Play Store submission

---

## 5. Explicitly Deferred / Out of Scope (for now)

- External mutual fund NAV / stock price integration
- Employer contribution modeling in NPS calculator
- Multiple funding sources per goal (until Phase 3)
