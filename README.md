# Finance Planner — Phase 1 Scaffold

This is the initial project skeleton for the Personal Finance Planning App
(see `finance-app-requirements.md` and `finance-app-technical-design.md`
for full context).

## What's included

- Gradle project setup (Kotlin, Jetpack Compose, Hilt, Coroutines, DataStore,
  kotlinx.serialization) using a version catalog (`gradle/libs.versions.toml`)
- `FinancePlannerApp` (Hilt entry point) and `MainActivity` (Compose entry point)
- Package structure matching the architecture doc:
  - `ui/calculators/{sip,lumpsum,emi,fire}` — one package per calculator (more
    to be added as they're built)
  - `ui/{goals,settings,onboarding,common,theme,navigation}`
  - `domain/{model,usecase}` — pure Kotlin, no Android dependencies
  - `data/{local,repository}`
  - `di/` — Hilt modules
- Localization scaffolding for English, Hindi, Marathi, Tamil, Telugu
  (`res/values-{hi,mr,ta,te}/` — currently placeholders, translations pending)
- ProGuard rules with a rule to strip debug logging from release builds
  (security requirement)

## What's NOT included yet

This is project setup only — no calculators, no UI beyond a placeholder
screen, no ViewModels or UseCases yet. Those come next.

## How to open this

1. Missing from this scaffold: the Gradle wrapper (`gradlew`, `gradlew.bat`,
   `gradle/wrapper/`). Android Studio will offer to generate this
   automatically on first open ("Gradle wrapper not found — generate?"), or
   run `gradle wrapper --gradle-version 8.9` if you have Gradle installed
   locally.
2. Open the project root folder in Android Studio (Koala/2024.1+ recommended
   for AGP 8.6 / Kotlin 2.0 compatibility).
3. Let Gradle sync — it will pull all dependencies from the version catalog.
4. Run on an emulator or device (minSdk 26 / Android 8.0+).

## Next step

Build the SIP calculator end-to-end (UseCase → ViewModel → Compose UI) as
the template pattern the other 13 calculators will follow.
