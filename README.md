# Gym Coach

An offline-first Android workout log with a built-in coach. Everything is stored on the phone; no account or internet needed. Units are metric (kg).

## Features

- **Workout log:** start a session, add exercises, log each set with weight, reps and how hard it felt (1–10).
- **Exercise library:** ~30 common gym exercises with step-by-step instructions, form tips, and easier/harder variations.
- **Goals:** strength goals ("Bench Press 80 kg") and consistency goals ("3 workouts per week") with progress bars.
- **6-week program:** pick 2, 3 or 4 workouts a week and a focus (strength, muscle or fitness). Weeks 1–5 build up, week 6 is a recovery week. Weights come from the coach's targets, so the plan keeps adapting.
- **Onboard plan:** the 6-week ship plan (3 strength + 2 cardio sessions per 7-day cycle, Learn/Build/Push phases) with rower intervals, crosstrainer sessions, a "shift one day" button for watch switches, and a guide with watch times, warm-up, safety and recovery.
- **Reminders:** a daily notification with today's session, timed for your watch (day watch 18:30, night watch 14:00, or custom). Rest days stay quiet.
- **Hero (level-up system):** earn XP for workouts, sets, records and streaks; rank up from Peasant to Legend of the Realm with a pixel-art hero that gains gear; complete quests for badges. Everything is computed from your log.
- **Attributes and classes:** STR from strength sets, VIT from cardio, AGI from mobility holds, WIS (discipline) from plan workouts and contracts, CON from consistent weeks and your best bonfire. Your class (Warrior, Ranger, Rogue, Paladin or a hybrid) follows your training mix. Hero tiers: Recruit, Adventurer, Knight, Legend.
- **Gear:** 23 pieces across weapon, armour, head, shield and cape, unlocked by achievements (never bought) and shown on your pixel hero.
- **Royal market:** village gold buys pets (wolf, raven, fox, ship's cat, dragon hatchling), hair colours and Ember Charms that save a bonfire streak. Rare royal tokens from bonfire milestones and contracts pay for real-life rewards you set yourself.
- **Bonfire streak:** a day streak that never asks you to train every day. Workouts and planned rest days feed the fire; without a calendar plan two rest days after a workout are fine. Miss a day and a Second Wind (once a week) saves the streak if you train the next day. Milestones at 3, 7, 14, 30 and 100 days give XP and royal tokens.
- **Guild contracts:** pick one contract a week (3 workouts, 2 cardio sessions, or your weekly target) for 250 XP and 50 village gold.
- **Boss campaign:** six weeks, six bosses, from the Bilge Rat King to the Dragon of the Deep. Workouts deal damage; beat each boss before Sunday ends to claim its loot.
- **Voyage map:** cardio km sail your ship across a pixel sea to ten islands; strength workouts open their treasure chests.
- **Village builder:** a 9 × 12 pixel map with forests, rocks and a river. Training fills the treasury (gold and food per workout, stone per strength set, wood per cardio minute). Place and upgrade 15 buildings: farms, mills, quarries, houses, markets and taverns make more for every workout after they are built, with bonuses for good placement; walls, towers, smiths, temples, stables, libraries and mage towers power Castle Defender. The village is saved on the phone.
- **Castle Defender:** a tap-to-shoot mini-game. Waves of goblins, bats, orcs, trolls and bosses attack your wall. Your village gives the wall HP, archers, repairs, lightning and slowing patrols; your level, slain bosses and blacksmiths make arrows stronger, and training that day doubles your damage. Plunder goes back to the village.
- **Retro look:** every screen uses the pixel font, square pixel borders and an RPG colour palette.
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

## Credits

Pixel font: [Press Start 2P](https://fonts.google.com/specimen/Press+Start+2P) by CodeMan38, SIL Open Font License 1.1 (see `licenses/PressStart2P-OFL.txt`).
