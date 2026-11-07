# Codex kickoff: Lodgist Android
Owner-approved scope: isolated Android fixes and tests, no Play Store release or device overwrite.
Repository: SHADOWSPARK-TECHNOLOGIES/lodgist-app
Read `AGENTS.md`, `docs/codex/EXECUTION_CONTRACT_2026-11-07.md` and `.agents/skills/codex-evidence-loop/SKILL.md` from branch `agent/codex-evidence-loop-20261107`.
Start **Loop 0** read-only. Verify PR #2 head, CI PR #3 workflow and run 42375935727 startup_failure with 0 jobs, environment settings, exact Gradle wrapper, SDK and existing test count. This is NOT the music application.
Do not run Gradle on a dirty owner worktree; first preserve and isolate. No phone install or signing-key changes. Work on workflow startup root cause only when verifiable, then assess listing API compatibility and run unit/lint/debug checks on a safe runner.
Report current test counts and remaining integration / device gates accurately.
