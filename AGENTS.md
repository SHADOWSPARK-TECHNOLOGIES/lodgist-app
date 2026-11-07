# Codex repository instructions: Lodgist Android

This is the **Lodgist Android Kotlin/Compose application**, not Moronto Mornica. Its canonical working branch for this assignment is `feat/live-listings-api-client` (PR #2). Never rename or alter package `com.aistudio.lodgist.xrkwqv`, release signing/keystores or Play Console without separate owner authorization.

At session start read `docs/codex/EXECUTION_CONTRACT_2026-11-07.md`, `docs/codex/KICKOFF.md`, and `.agents/skills/codex-evidence-loop/SKILL.md`. Inspect remote/branch/HEAD, Git worktree cleanliness, current API endpoint contracts, Android SDK/toolchain and Actions startup status before writing.

Use the repository Gradle wrapper 9.3.1 and Android API 36.1. Independent QA: unit tests, lint XML and debug APK build on a clean isolated worktree. The module currently sets `lint.abortOnError=false`, which must not be mistaken for an error-free lint pass. CI PR #3 startup_failure with 0 jobs is NOT a Kotlin failure.

No `git reset --hard`, `git clean`, stash, force push, merge, production API/config edits, physical-device install, app publication or signing changes. Preserve user's local modifications. Make a single scoped draft PR per verified objective; stop after 2 no-progress failures or 8 total attempts.
