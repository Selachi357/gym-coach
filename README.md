# Gym Coach

An offline-first Android workout log with a built-in coach. Everything is stored on the phone; no account or internet needed. Units are metric (kg).

## Features

- **Workout log:** start a session, add exercises, log each set with weight, reps and how hard it felt (1–10).
- **Exercise library:** ~30 common gym exercises with step-by-step instructions, form tips, and easier/harder variations.
- **Goals:** strength goals ("Bench Press 80 kg") and consistency goals ("3 workouts per week") with progress bars.
- **History:** every finished workout with sets, total volume and duration.
- **Coach:** after each workout the coach reviews every exercise and sets your next target:
  - All sets done at effort 8 or lower → +2.5 kg (or +1 rep for bodyweight moves; harder variant at 15 reps).
  - Missed reps or very high effort two sessions in a row → weight drops ~10%, or it switches you to an easier variant.
  - Press **Too hard** during a set and the coach adjusts right away.

## Install on your phone

Every push to `main` builds an APK with GitHub Actions and publishes it under **Releases → Latest build**.
Open that page on your phone, download `GymCoach.apk`, and allow installing from your browser when Android asks.

## Development

Kotlin, Jetpack Compose, Material 3, Room. Coach rules live in `app/src/main/java/com/gymcoach/coach/Coach.kt` with unit tests in `app/src/test`.

```
./gradlew testDebugUnitTest assembleRelease
```
