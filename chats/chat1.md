# Mobile logic learning app

_Started 2026-09-30 14:44 UTC_

---

## User

Design a mobile app for learning informal logic, with OpenRouter API intergration

## Assistant



## User

Answered — audience: University students in a critical-thinking course, Curious adults: media literacy, arguing better online; core_loop: Bite-sized lessons → quiz along a learning path, Spot the fallacy in realistic snippets (posts, ads, op-eds), Build argument maps (premises → conclusion), Spar: defend a position against an AI oppone…; ai_role: Generate endless fresh exercises, Explain why an answer was wrong, Analyze any argument I paste or share; api_key: BYOK: user pastes their OpenRouter key, picks any model; scope: Argument anatomy (premises, conclusions, hidden assumptions), Fallacies…

## Assistant

_[tool: ask_user]_

_[tool: read_skill_prompt]_

_[tool: read_skill_prompt]_

_[tool: read_skill_prompt]_

_[tool: copy_starter_component]_

_[tool: web_fetch]_

_[tool: read_file]_

_[tool: web_search]_

_[tool: web_search]_

_[tool: ask_user]_

_[tool: copy_starter_component]_

_[tool: delete_file]_

_[tool: grep]_

_[tool: read_file]_

_[tool: read_file]_

_[tool: str_replace_edit]_

_[tool: dc_write]_

_[tool: ready_for_verification]_

I've built **Ergo**, a clickable Android prototype with a real OpenRouter connection. You skipped the visual questions, so I chose the look: warm paper and ink with "red pencil" corrections. Serif for reading text, a plain sans for the interface, red marks for flaws and blue for argument structure. I haven't checked it on screen yet; a review is running now.

- **Onboarding:** pick what brings you here, then paste an OpenRouter key. It's checked against your OpenRouter account and saved only on the device. The model picker loads OpenRouter's current model list with prices and search. The default is OpenRouter's Auto router, which picks a model for each request.
- **Learn:** the syllabus covers six units. Straw man is the one full sample lesson: a definition, the pattern, a real example you mark up yourself, and a check question. The other lessons show a note instead of opening.
- **Drill:** tap the sentence where the reasoning breaks, then name the fallacy. "Generate fresh" has the model write a new exercise, and "Why isn't it X?" asks it to explain your wrong answer.
- **Analyze:** paste any argument. It comes back with the flaws marked in the text, a map of conclusion, premises and unstated assumptions, numbered notes on each problem, and an overall verdict.
- **Spar:** you argue for a motion and the model argues against. Any fallacy you commit gets marked under your message.
- **You:** key status, spending and remaining credit on the key, cost for this session, the model, and disconnect.

Without a key, everything runs in demo mode on built-in content. In demo mode, Analyze only works on the two built-in examples, and Spar spots fallacies with simple word matching rather than the model.

The Tweaks panel has three settings: which screen to start on, red-pencil or highlighter marks, and whether costs are shown.

Next steps could be a dark theme, streaming replies, or the other lessons.

## User

Found issues — fixing…

## User

Found issues — fixing…

## User

I want interface in russian

## Assistant

_[tool: dc_write]_

_[tool: ready_for_verification]_

The whole interface is now in Russian: every screen, the lesson, the drills, the sample texts, the debate topics and the error messages. The model is also told to write its exercises, explanations and argument maps in Russian. A review of the new version is running now.

- **Fonts:** I switched to Literata and Onest, because the previous fonts don't include Cyrillic letters.
- **Examples:** the names and situations are adapted for Russian readers.
- **Tone:** the app addresses the user formally («вы»).
- **Demo mode:** the offline fallfor spotting fallacies in the debate now matches Russian phrases.
- **Prices:** still shown in dollars, because OpenRouter bills in USD.

