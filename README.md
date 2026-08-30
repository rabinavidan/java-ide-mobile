# Java IDE Mobile

A single-module Android app that turns a phone or tablet into a lightweight Java development environment. It lets users create Java console projects and Android app projects, edit source files with a code editor, compile with ECJ, dex artifacts with D8, run them on-device, and practice coding exercises directly inside the app.

This project is designed to feel like a portable developer workstation: project management, file browsing, syntax-aware editing, build output, Git-style history, and interview practice all live in one place.

## Why this project matters

Most mobile coding tools either stop at syntax highlighting or rely on an external server. This project takes a different approach:

- Compilation happens entirely on-device
- Java sources are compiled and dexed without a desktop toolchain
- Android and console Java projects can be built and run directly from the app
- A built-in interview practice flow helps validate algorithmic problem-solving in Java
- The app is structured like a real developer workflow, not a toy demo

It blends systems programming, Android UI, compiler plumbing, and developer tooling into one cohesive project.

## Highlights

- Create Java console projects and Android app projects from the app itself
- Browse project files and edit them with a multi-tab editor experience
- Compile Java sources with the Eclipse Compiler for Java (ECJ)
- Convert classes to DEX with D8
- Run Java programs in-process using DexClassLoader
- Build Android app projects into APKs and sign them with a reusable debug key
- Inspect compile logs and build output through dedicated result screens
- Practice curated interview-style Java exercises grouped by topic
- Persist editor state, project history, and usage metadata locally
- Support Git-based project history and commit tracking features

## Architecture

The app follows a compact but realistic build pipeline:

```text
Java source files
        ↓
JavaCompiler (ECJ)
        ↓
.class generation
        ↓
Dexer (D8)
        ↓
classes.dex
        ↓
JavaRunner / APK packaging / Android app build flow
```

For Java console projects, the pipeline is:

```text
ECJ → .class → D8 → classes.dex → DexClassLoader + reflection → main()
```

For Android app projects:

```text
ResourceCompiler (ARSCLib) → JavaCompiler (ECJ) → Dexer (D8) → Packager (apksig) → APK install
```

The flow is orchestrated from the project UI layer, with logs captured at each stage and surfaced in the build output experience.

## Tech stack

- Android SDK / Jetpack / AndroidX
- Kotlin
- Java 17
- Room for local persistence
- Sora Editor for code editing
- ECJ for Java compilation
- R8 / D8 for dexing
- ARSCLib for Android resource compilation
- apksig for APK signing
- JGit + BouncyCastle for Git/PGP functionality
- JUnit + Allure for test reporting

## Project structure

```text
java-ide-mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/javaide/mobile
│   │   │   │   ├── compiler/
│   │   │   │   ├── completion/
│   │   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   ├── ui/
│   │   │   │   ├── util/
│   │   │   │   └── vcs/
│   │   │   └── ...
│   │   ├── test/
│   │   ├── testShared/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── ...
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle.properties
├── local.properties
├── .gitignore
└── README.md
```

## Core feature areas

### 1. Project lifecycle
The app can create, rename, delete, and browse project folders. Each project is stored locally in the app's file system and scaffolded with the appropriate structure for either a Java console app or an Android app.

### 2. Code editor and file management
Users can open files in a multi-tab editor, navigate project trees, and work with Java source files directly. Editor state is persisted so cursor position and recent files can be restored after reopening the project.

### 3. Build and run engine
The compiler toolchain is embedded in the app and runs on-device. Logs and diagnostics are surfaced so a user can quickly identify issues during compilation or runtime failures.

### 4. Interview practice library
The project includes a searchable library of interview-style coding challenges and grouped practice categories. This makes the app useful both as a coding environment and as a Java algorithm practice workspace.

### 5. Developer tooling
The repo also includes Git-related capabilities, commit history, logging, resource handling, and project search tools that make it feel like a real mobile IDE rather than a simple code runner.

## Getting started

### Prerequisites

- Android Studio
- JDK 17+
- Android SDK configured for compileSdk 34
- An emulator or Android device for running the app

### Clone

```bash
git clone https://github.com/rabinavidan/java-ide-mobile.git
cd java-ide-mobile
```

### Build debug APK

```bash
./gradlew assembleDebug
```

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

### Run a single local test

```bash
./gradlew testDebugUnitTest --tests "com.javaide.mobile.compiler.InterviewExercisesCompileDexTest.fizzBuzz"
```

### Generate an Allure report

```bash
./gradlew allureReport
```

## Testing strategy

This project includes both local JVM tests and Android instrumentation coverage:

- Local unit tests compile and dex Java interview exercises in a JVM environment
- Instrumented tests validate behavior on-device
- Shared fixtures are included in the main source set to avoid duplicate-class issues and keep the test data reusable

## Build pipeline notes

The project includes several design decisions that are worth calling out:

- `android.jar` is copied into the app assets at build time instead of being committed as a binary blob
- Java compiler and D8 tools run in-process for a fully local workflow
- Output logs are preserved so build failures are visible and actionable
- The app intentionally keeps project generation simple and fast for experimentation

## Example workflow

1. Launch the app
2. Create a new Java console or Android project
3. Add or edit Java source files
4. Compile and dex the project
5. Run the project and inspect stdout/stderr
6. Iterate on code using build logs and diagnostics
7. Use the practice section to solve algorithmic interview questions

## Why this stands out

This project sits at the intersection of several disciplines:

- Android application development
- Low-level compiler tooling
- On-device execution and packaging
- Developer workflow design
- Code quality and automated validation

For an interview or portfolio context, it demonstrates the ability to build a full product-like application rather than a single isolated feature.

## License

This repository does not currently declare a license file. If you intend to distribute or reuse the project, add an appropriate license before public release.

## Future ideas

- Improve diagnostics quality and in-editor suggestions
- Add richer Java language features and completions
- Support more project templates and package management
- Add APK install and run flows from a cleaner project dashboard
- Expand interview exercises and performance tracking
- Add project export/import for sharing code between devices

## Summary

Java IDE Mobile is a practical, ambitious Android project that simulates a compact developer environment on mobile hardware. It combines code editing, Java compilation, execution, project management, and interview preparation into a single application.

It is the kind of project that communicates strong engineering instincts: systems thinking, tooling familiarity, release readiness, and a product mindset.
