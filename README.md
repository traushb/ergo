# Ergo

A native Android app (Kotlin + Jetpack Compose) for learning informal logic, in Russian. Learners use their own OpenRouter key and pick the model. It is built from the Claude Design prototype in [`design/`](design/). The primary design is `design/project/Ergo.dc.html` and the intent is in `design/chats/`.

## What's in it

Everything works offline on a built-in Russian content bank (`app/src/main/assets/content/`). An OpenRouter key adds model-written exercises, explanations, full argument analysis and a live debate opponent.

| Tab | What it does |
| --- | --- |
| **Учёба** (Learn) | 26 lessons in six units, from argument anatomy to rhetoric. Each lesson has five steps: definition, how it works (with the telltale phrase and how to answer), an example to mark up, a check question, and done. Finished lessons enter spaced repetition. |
| **Практика** (Practice) | Pick a topic: everything, a unit, or one fallacy. **Тренировка** is 6–8 exercises with scoring and a summary. Exercise types: find and name the fallacy (with "clean" snippets where the right answer is "there's no error"), find the conclusion, a premise and the hidden assumption, and logic puzzles. **Блиц** is 60 seconds, with combos and a 3-second penalty. **Повторение** brings topics back after 1, 3, 7 and 21 days. **Свежие задания** has the model write exercises for the chosen topic. Weak spots and per-topic progress are listed, and every topic has a reference card. |
| **Анализ** (Analyze) | Paste an argument and get flaws marked in the text, its skeleton (conclusion, premises, assumptions), notes and a verdict. Five worked samples are included. Offline, a keyword pass marks possible problems. |
| **Спор** (Spar) | Defend one of eight motions. Ergo argues against you and flags fallacies, and you get a round-up at the end. Offline, it replies with prepared counter-arguments and calls out fallacies it spots by key phrases. |
| **Профиль** (You) | Streak, lessons, review queue, solved count, accuracy and blitz record. Key status and model. Settings: theme (auto, light, dark), mark style, cost display. Progress reset. |

### Content bank

| File | Contents |
| --- | --- |
| `topics.json` | Units and 26 topics: definition, steps, tell, counter, formula; hand-written lesson examples and checks where needed |
| `drills.json` | 72 find-and-name snippets (65 flawed, 7 clean lookalikes) with explanations and "why not X" notes |
| `structure.json` | Conclusion / premise / hidden-assumption exercises |
| `quizzes.json` | Validity puzzles and base-rate, anchoring, framing and availability questions |
| `motions.json` | Debate motions with offline replies, plus call-outs per fallacy |
| `samples.json` | Worked analyses for the Analyze tab |

`BankTest` checks that every reference in the bank resolves, so new content can be added by editing JSON.

## Layout

```
app/src/main/java/app/ergo/
  MainActivity.kt        edge-to-edge host; applies the light or dark theme
  ErgoViewModel.kt       navigation, onboarding, key/model, settings, lessons; wires the controllers
  Practice.kt            exercise runs, sessions, scoring, blitz, review (PracticeController)
  Lesson.kt              five-step lessons, built from the bank
  Debate.kt              Spar and Analyze controllers
  Ai.kt                  OpenRouter calls and topic-targeted exercise generators
  data/Bank.kt           content bank model and JSON parser
  data/Progress.kt       spaced repetition (1/3/7/21 days), streak, stats
  data/Offline.kt        offline cue detection, keyword analysis, debate replies
  data/OpenRouter.kt     HTTP client: /key, /models, /chat/completions
  data/Store.kt          SharedPreferences; the API key is AES-GCM encrypted with an Android Keystore key
  ui/Theme.kt            light and dark palettes, Literata / Onest / JetBrains Mono, text-style helpers
  ui/Components.kt       marked text (red pencil / blue structure), options, sheets, dialogs
  ui/*.kt                one file per screen, plus exercises and sheets
```

Fonts are bundled from Google Fonts under the SIL OFL, with licenses in `licenses/`.

## Build

Requires the Android SDK (compileSdk 35) and an installed **JDK 21**. `gradle/gradle-daemon-jvm.properties` makes Gradle (and Android Studio) run the build on JDK 21 even when the default Java is newer. Gradle 8.14 can't run on JDK 25, which recent Fedora and Android Studio ship. On Fedora: `sudo dnf install java-21-openjdk-devel`.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The OpenRouter key never leaves the device except in requests to `openrouter.ai`. `allowBackup` is off so the encrypted key isn't restored onto another device, where its Keystore key wouldn't exist.
