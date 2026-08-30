# Context Log

Running log of all development activity in this repository.

---

## 2026-08-22

### Session Start
- Branch: `claude/android-java-app-e60ews`
- User: rabinabdian

### Activity
- `CLAUDE.md` — Improved with expanded package table, corrected Android App pipeline order, added conventions and non-obvious implementation details
- `CONTEXT_LOG.md` — Created this file to track ongoing development activity
- `Logger.kt` — Added rotating on-device file logging (`filesDir/logs/activity.log`, 512 KB max, 1 backup); all existing Room events now also written to file
- `EditorActivity.kt` — Added Logger calls for: file opened, file saved (auto-save), tab closed

---

## 2026-08-30

### Session Start
- Branch: `claude/java-ide-interview-baseline-i1g39p`
- Task: Milestone 0 (Baseline, Backup and Project Analysis) of the Interview Practice Expansion plan

### Activity
- `docs/BASELINE.md` — Created: records the current exercise inventory (30 exercises, 10
  categories, confirmed against `InterviewExercises.kt`/`PracticeCategories.kt`), the
  compile/dex/run pipeline, the current Run & Check flow (exact-string match, no hidden tests),
  the existing unit/instrumented test suites (208 `@Test` methods total), the CI workflow, and
  the documented current limitations
- `app/build.gradle.kts` — Fixed `./gradlew lint`, which previously failed outright with a Gradle
  task-validation error: `generateDebugLintReportModel`/`lintAnalyzeDebug` read
  `build/generated/androidJarAsset` without a declared dependency on the `copyAndroidJar` task
  that populates it. Extended the existing `merge*Assets` `dependsOn` wiring to also cover the
  lint tasks
- `.gitignore` — Added `.kotlin/` (local Gradle build cache directory)
- Ran baseline validation in this sandboxed environment (no Android SDK preinstalled here;
  provisioned `cmdline-tools` + `platform-tools` + `platforms;android-34` + `build-tools;34.0.0`
  locally to match CI): `./gradlew testDebugUnitTest` (160 tests, all passing),
  `./gradlew lint` (0 errors, 93 warnings, after the fix above),
  `./gradlew assembleDebug` (debug APK builds successfully). Instrumented tests were not run —
  no emulator/device available here

---
