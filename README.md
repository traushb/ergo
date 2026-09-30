# Ergo

A native Android app (Kotlin + Jetpack Compose) for learning informal logic, in Russian. Learners use their own OpenRouter key and pick the model. It is built from the Claude Design prototype in [`design/`](design/). The primary design is `design/project/Ergo.dc.html` and the intent is in `design/chats/`.

## What's in it

| Tab | What it does |
| --- | --- |
| **Учёба** (Learn) | The six-unit syllabus, a "continue" card and the review entry. The Straw Man lesson is fully built: definition, pattern, an example you mark yourself, a check question and a finish screen. |
| **Практика** (Drill) | Tap the sentence where the reasoning breaks, then name the fallacy. **Сгенерировать** has the model write a fresh exercise, and **Почему не «…»?** asks it why your answer was wrong. |
| **Анализ** (Analyze) | Paste any argument. You get flaws marked in the text, a skeleton (conclusion, premises, unstated assumptions), numbered notes and a verdict. |
| **Спор** (Spar) | You defend a motion and the model argues against it. Any fallacy you commit is flagged under your message. |
| **Профиль** (You) | Key status, spend and remaining limit (from `GET /key`), session cost, model picker, settings, and disconnect. |

Without a key, the app runs in **demo mode** on built-in content, as in the prototype. Analyze only works on the two sample texts, and Spar flags fallacies with simple phrase matching.

### Differences from the prototype

- The Android device frame is gone; the app runs edge to edge.
- Two of the prototype's Tweaks are now user settings in **Профиль → Настройки**: mark style (*Карандаш*, a red wavy underline, or *Маркер*, a highlighter) and whether to show request costs. The "start screen" tweak is dropped. The app opens on onboarding the first time and on Learn after that.
- State that the prototype kept in memory now persists: onboarding done, goal, Straw Man progress, model, and settings.
- The system back button closes the sheet or lesson, steps back through onboarding, leaves a debate, or returns to Learn.
- The tab bar hides while the keyboard is open.

## Layout

```
app/src/main/java/app/ergo/
  MainActivity.kt        edge-to-edge host
  ErgoViewModel.kt       all screen state + actions (port of the prototype's Component class)
  data/Content.kt        syllabus, drills, samples, topics, prompts, formatting helpers
  data/OpenRouter.kt     HTTP client: /key, /models, /chat/completions (usage.include for cost)
  data/Store.kt          SharedPreferences; the API key is AES-GCM encrypted with an Android Keystore key
  ui/Theme.kt            colour tokens, Literata / Onest / JetBrains Mono, text-style helpers
  ui/Components.kt       MarkedText (wavy underline / highlighter), Dots, ErgoMark, buttons
  ui/*.kt                one file per screen, plus the model sheet and the app shell
```

Fonts are bundled from Google Fonts under the SIL OFL, with licenses in `licenses/`. Literata is a variable font. It comes in two optical-size cuts, because Android doesn't choose `opsz` automatically the way browsers do.

## Build

Requires the Android SDK (compileSdk 35) and JDK 17+.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The OpenRouter key never leaves the device except in requests to `openrouter.ai`. `allowBackup` is off so the encrypted key isn't restored onto another device, where its Keystore key wouldn't exist.
