# Java IDE Mobile — Interview Practice Expansion: Full Development Milestones Plan

This is the canonical, in-repo copy of the full milestone plan driving the expansion of Java IDE
Mobile from a 30-exercise practice catalog into a structured interview-training platform. It was
originally handed down as task context rather than a repo file; this document makes it durable and
diffable, and is where new milestones (like Milestone 23 below) get appended as they're identified.

**Repository:** https://github.com/rabinavidan/java-ide-mobile

## Product goal

Transform the existing Android Java IDE into a structured mobile interview-training platform for
Java developers, SDETs and Automation Engineers.

**Target content:**
- 100 curated coding challenges
- 60 algorithms and data-structure challenges
- 20 Java-specific challenges
- 20 SDET and automation coding challenges

**Target learning modes:** Learn Mode · Practice Mode · Interview Mode

---

## Milestone 0 — Baseline, backup and project analysis

**Status: ✅ done** — see [`BASELINE.md`](BASELINE.md) and PR
[#39](https://github.com/rabinavidan/java-ide-mobile/pull/39).

**Goal:** Create a safe development baseline before changing the current exercise system.

**Tasks:**
1. Create a feature branch: `feature/interview-practice-v2`
2. Record the current state: exercise count, categories, compiler flow, Run & Check flow, unit
   tests, instrumented tests, CI workflow
3. Run the existing validation: `./gradlew test`
4. Run static checks: `./gradlew lint`
5. Build the debug APK: `./gradlew assembleDebug`
6. Run instrumented tests when an emulator is available: `./ci/run-instrumented-tests.sh`
7. Record current limitations: complete solutions instead of starter code, console-output-only
   checking, no hidden test cases, no difficulty levels, no progress tracking, no problem
   descriptions, no hints, no interview timer, no filtering/search, all exercises in one file, no
   README

**Deliverables:** baseline test report, list of current exercise IDs/categories, feature branch,
documented architecture notes.

**Acceptance criteria:** existing unit tests pass; existing debug APK builds; no current feature
removed; current 30 exercises still compile and run.

**Estimated effort:** 1–2 development days

---

## Milestone 1 — Repository documentation

**Status: ✅ done** — see [`README.md`](../README.md), [`CONTRIBUTING.md`](../CONTRIBUTING.md),
[`ARCHITECTURE.md`](ARCHITECTURE.md), [`PRACTICE_ENGINE.md`](PRACTICE_ENGINE.md),
[`ADDING_EXERCISES.md`](ADDING_EXERCISES.md), [`TESTING.md`](TESTING.md), and PR
[#40](https://github.com/rabinavidan/java-ide-mobile/pull/40).

**Goal:** Make the repository understandable to developers, contributors and recruiters.

**Tasks:**
1. Create `README.md`
2. README sections: project overview, product purpose, main features, supported project types,
   Java compilation flow, practice system, existing exercise categories, screenshots, local build
   instructions, emulator requirements, test commands, CI workflow, project architecture, roadmap,
   contribution instructions, known limitations
3. Architecture diagram: user selects challenge → challenge catalog loads metadata → editor opens
   starter code → Java compiler compiles code → Java runner executes code → test evaluator checks
   results → progress repository stores the result
4. Create `CONTRIBUTING.md`
5. Create `docs/` directory
6. Add `docs/ARCHITECTURE.md`, `docs/PRACTICE_ENGINE.md`, `docs/ADDING_EXERCISES.md`, `docs/TESTING.md`
7. Document how a contributor can add a new challenge

**Deliverables:** README.md, CONTRIBUTING.md, architecture documentation, exercise contribution guide.

**Acceptance criteria:** a new developer can build the app using README instructions; a
contributor can add a challenge without reading the entire codebase; every documented command
works; README displays the current challenge count correctly.

**Estimated effort:** 2–3 development days

---

## Milestone 2 — Practice domain model V2

**Goal:** Replace the minimal exercise model with a model that supports real interview practice.

**Current model:**
```kotlin
InterviewExercise(className, source, expectedOutput)
```

**New model:**
```kotlin
enum class Difficulty { EASY, MEDIUM, HARD }
enum class PracticeMode { LEARN, PRACTICE, INTERVIEW }

data class ExerciseExample(val input: String, val output: String, val explanation: String)

data class ExerciseTestCase(
    val id: String,
    val input: String,
    val expectedOutput: String,
    val visible: Boolean,
    val description: String
)

data class InterviewExercise(
    val id: String,
    val title: String,
    val className: String,
    val categoryId: String,
    val difficulty: Difficulty,
    val description: String,
    val constraints: List<String>,
    val examples: List<ExerciseExample>,
    val starterCode: String,
    val solutionCode: String,
    val hints: List<String>,
    val testCases: List<ExerciseTestCase>,
    val timeComplexity: String,
    val spaceComplexity: String,
    val patterns: Set<String>,
    val tags: Set<String>,
    val estimatedMinutes: Int
)
```

**Tasks:**
1. Add the new model classes
2. Give every challenge a stable ID (e.g. `arrays-two-sum`, `linked-list-reverse`, `tree-max-depth`)
3. Add category IDs instead of relying only on visible category names
4. Add validation rules: unique exercise ID; valid Java class name; at least one test case; at
   least one visible example; starter code compiles after implementation; solution code compiles
   and passes all tests; estimated time > 0
5. Create an `ExerciseValidator`
6. Create migration adapters for the existing 30 exercises
7. Keep the old functionality working during migration
8. Unit tests for: duplicate IDs, invalid class names, missing test cases, missing descriptions,
   unsupported difficulty, empty starter code, invalid estimated time

**Deliverables:** Practice model V2, ExerciseValidator, migration adapter, model unit tests.

**Acceptance criteria:** existing 30 exercises load through the new model; every exercise has a
unique ID; invalid exercise definitions fail during tests; no exercise depends on its display
title as an identifier.

**Estimated effort:** 3–5 development days

---

## Milestone 3 — Modular exercise catalog

**Goal:** Split the large `InterviewExercises.kt` file into maintainable files.

**Target structure:**
```
app/src/main/java/com/javaide/mobile/practice/
    model/
        Difficulty.kt
        PracticeMode.kt
        InterviewExercise.kt
        ExerciseExample.kt
        ExerciseTestCase.kt
    catalog/
        ExerciseCatalog.kt
        FundamentalsExercises.kt
        ArraysExercises.kt
        HashMapExercises.kt
        TwoPointersExercises.kt
        SlidingWindowExercises.kt
        LinkedListExercises.kt
        StackQueueExercises.kt
        TreeExercises.kt
        GraphExercises.kt
        HeapExercises.kt
        BinarySearchExercises.kt
        BacktrackingExercises.kt
        DynamicProgrammingExercises.kt
        BitManipulationExercises.kt
        JavaExercises.kt
        AutomationExercises.kt
    validation/
        ExerciseValidator.kt
        CatalogValidator.kt
```

**Tasks:**
1. Create `ExerciseCatalog` interface
2. Create a catalog registry
3. Move the existing 30 exercises into topic files
4. Replace direct references (`InterviewExercises.TWO_SUM`) with catalog lookup
   (`exerciseCatalog.findById("arrays-two-sum")`)
5. Add catalog operations: `getAll()`, `findById()`, `findByCategory()`, `findByDifficulty()`,
   `findByPattern()`, `search()`
6. Add catalog validation during unit tests
7. Tests confirming: all exercises registered; no duplicate IDs; no duplicate class names;
   categories contain valid exercises; search is case-insensitive; filters return correct results

**Deliverables:** modular catalog, catalog registry, migrated existing exercises, catalog unit tests.

**Acceptance criteria:** no single catalog file becomes excessively large; all 30 existing
challenges remain available; practice categories load from the catalog; exercise count is
calculated automatically; help text does not hard-code "30 exercises".

**Estimated effort:** 3–4 development days

---

## Milestone 4 — Challenge execution and test engine

**Goal:** Replace single-output comparison with structured test execution.

**Tasks:**
1. Create `ExerciseRunner`
2. Create `TestCaseRunner`
3. Create `ExerciseResultEvaluator`
4. Execution models:
```kotlin
data class TestCaseResult(
    val testCaseId: String,
    val passed: Boolean,
    val expected: String,
    val actual: String,
    val executionTimeMs: Long,
    val errorMessage: String?
)

data class ExerciseRunResult(
    val exerciseId: String,
    val compiled: Boolean,
    val passed: Boolean,
    val passedTests: Int,
    val totalTests: Int,
    val compilationErrors: List<String>,
    val testResults: List<TestCaseResult>,
    val totalExecutionTimeMs: Long
)
```
5. Support: compilation failure, runtime exception, timeout, wrong result, partial success, full success
6. Per-test timeout protection
7. Capture stdout and stderr separately
8. Normalize output safely: normalize line endings, remove trailing whitespace, preserve
   meaningful whitespace, don't hide incorrect output
9. Show visible test results
10. Hide expected values for hidden tests in Interview Mode
11. Prevent one failed test from stopping all remaining tests
12. Tests for: correct solution, incorrect solution, compilation error, runtime exception,
    infinite loop/timeout, multiple failing test cases, extra console output, empty output

**Deliverables:** structured challenge runner, multiple test-case support, timeout protection,
detailed run-result model, runner unit and integration tests.

**Acceptance criteria:** a challenge can contain multiple tests; the user sees exactly which
visible tests failed; hidden tests don't expose expected answers; a timeout doesn't freeze the
app; existing Run functionality remains available for normal Java projects.

**Estimated effort:** 5–8 development days

---

## Milestone 5 — Starter code and solution separation

**Goal:** Stop opening every challenge with the complete answer.

**Tasks:**
1. Store `starterCode` and `solutionCode` separately
2. Convert every existing exercise into: problem description, starter code, reference solution,
   examples, test cases, complexity explanation, hints
3. Starter code should contain: required class, method signature, TODO marker, minimal test
   harness when required
4. Add actions: Open Starter Code, Reset Code, Reveal Hint, View Solution, Copy Solution, Run Tests
5. Require confirmation before: resetting edited code, replacing code with the solution
6. Track whether the user revealed the solution
7. Mark a challenge as independently solved only when all tests pass AND the solution was not
   revealed before passing
8. Tests for: starter code loads correctly, reset restores starter code, solution does not appear
   automatically, revealing solution updates progress, user changes are not overwritten without
   confirmation

**Deliverables:** separate starter/solution code, reset flow, hint flow, solution reveal flow,
independent-solve tracking.

**Acceptance criteria:** Practice Mode opens starter code; Learn Mode can show the solution;
Interview Mode blocks solution access; resetting code requires confirmation; existing user
projects remain untouched.

**Estimated effort:** 4–6 development days

---

## Milestone 6 — Core interview pattern expansion

**Goal:** Add the most common missing interview patterns.

- **Section A — Hash Maps and Sets (6):** First Non-Repeating Character, Intersection of Two
  Arrays, Isomorphic Strings, Happy Number, Longest Consecutive Sequence, Top K Frequent Elements
- **Section B — Two Pointers (6):** Valid Palindrome, Two Sum II, Remove Duplicates From Sorted
  Array, Squares of a Sorted Array, Container With Most Water, Three Sum
- **Section C — Sliding Window (6):** Best Time to Buy and Sell Stock, Maximum Sum Subarray of
  Size K, Longest Substring Without Repeating Characters, Minimum Size Subarray Sum, Permutation
  in String, Minimum Window Substring
- **Section D — Intervals (6):** Merge Intervals, Insert Interval, Meeting Rooms, Meeting Rooms
  II, Non-Overlapping Intervals, Interval List Intersections

**Content requirements per challenge:** unique ID, clear problem statement, difficulty, estimated
completion time, ≥2 examples, constraints, starter code, reference solution, ≥3 tests, ≥1 hidden
test, ≥2 hints, time complexity, space complexity, pattern tags.

**Testing tasks:** compile every reference solution; execute every test case; verify expected
results; add negative cases; add empty-input cases where valid; add boundary cases; add
duplicate-value cases.

**Deliverables:** four new categories, 24 new challenges, full content and tests.

**Acceptance criteria:** every reference solution passes all tests; every challenge appears under
the correct category; difficulty filters work; search finds challenges by title and pattern; no
challenge depends on non-standard Java libraries.

**Estimated effort:** 8–12 development days

---

## Milestone 7 — Advanced data structures

**Goal:** Build stronger coverage for mid-level and senior interviews.

- **Linked Lists (7):** Middle of Linked List, Remove Nth Node From End, Intersection of Two
  Linked Lists, Palindrome Linked List, Reorder List, Copy List With Random Pointer, LRU Cache
- **Stacks and Queues (7):** Queue Using Stacks, Stack Using Queues, Evaluate Reverse Polish
  Notation, Daily Temperatures, Next Greater Element, Simplify Path, Largest Rectangle in Histogram
- **Trees and BST (8):** Invert Binary Tree, Validate Binary Search Tree, Balanced Binary Tree,
  Diameter of Binary Tree, Lowest Common Ancestor, Kth Smallest Element in BST, Build Tree From
  Traversals, Serialize and Deserialize Binary Tree
- **Heaps and Priority Queues (5):** Kth Largest Element, K Closest Points to Origin, Merge K
  Sorted Lists, Task Scheduler, Find Median From Data Stream

**Tasks:**
1. Reusable `ListNode`/`TreeNode` templates
2. Ensure generated user code doesn't create class-name conflicts
3. Deterministic serializers for linked lists, binary trees, integer arrays, nested lists
4. Input builders for test cases
5. Cycle-safe linked-list output logic
6. Tree comparison utilities
7. Tests for null roots and empty lists

**Deliverables:** expanded linked-list/stacks-and-queues/tree-and-BST catalogs, new heap category,
shared data-structure testing utilities.

**Acceptance criteria:** complex structures testable automatically; tree results compared
structurally; linked-list cycles can't freeze result rendering; LRU Cache operations testable as
sequences; all reference solutions pass CI.

**Estimated effort:** 10–15 development days

---

## Milestone 8 — Graphs, tries and union-find

**Goal:** Add graph problems that represent real interview difficulty.

- **Graph (10):** Flood Fill, Number of Islands, Clone Graph, Connected Components, Course
  Schedule, Detect Cycle in Directed Graph, Shortest Path With BFS, Network Delay Time, Dijkstra's
  Shortest Path, Redundant Connection
- **Union-Find (4):** Implement Disjoint Set Union, Count Connected Components, Number of
  Provinces, Accounts Merge
- **Trie (4):** Implement Trie, Design Add and Search Words, Replace Words, Word Search II

**Tasks:**
1. Graph input representations: adjacency list, edge list, matrix, grid
2. Graph output normalization
3. Avoid result failures caused only by traversal ordering
4. Compare unordered graph results safely where order is irrelevant
5. Reusable `UnionFind` starter template
6. Reusable `TrieNode` starter template
7. Timeout tests for cyclic graphs

**Deliverables:** expanded graph category, Union-Find category/pattern, Trie category, graph
testing utilities.

**Acceptance criteria:** graph cycles don't create infinite traversal; order-independent results
compare correctly; directed/undirected graphs clearly identified; every challenge contains
complexity analysis; hidden tests include disconnected and cyclic graphs.

**Estimated effort:** 8–12 development days

---

## Milestone 9 — Dynamic programming and backtracking

**Goal:** Provide a structured learning path for harder interview techniques.

- **Dynamic Programming (9):** House Robber, Unique Paths, Word Break, Decode Ways, Longest
  Increasing Subsequence, Longest Common Subsequence, Partition Equal Subset Sum, 0/1 Knapsack,
  Edit Distance
- **Backtracking (6):** Combination Sum, Generate Parentheses, Letter Combinations of Phone
  Number, Palindrome Partitioning, Word Search, N-Queens

**Tasks:**
1. Tag DP problems by sub-pattern: one-dimensional, two-dimensional, knapsack, sequence, grid
2. Explain state, transition and base case for every DP solution
3. Optional brute-force examples in Learn Mode
4. Compare brute-force and optimized complexity
5. Recursion-depth protection
6. Result normalization for nested collections

**Deliverables:** expanded Dynamic Programming and Backtracking sections, learning explanations,
complexity comparisons.

**Acceptance criteria:** each DP challenge explains its state definition; each backtracking
problem explains choose/explore/unchoose; nested output results compare correctly; hard
challenges include ≥3 hints; reference solutions stay within safe mobile execution limits.

**Estimated effort:** 8–12 development days

---

## Milestone 10 — Java interview practice

**Goal:** Add challenges that test Java knowledge, not only algorithms.

- **Java Collections (7):** Sort Objects With Comparator, Group Objects by Property, Count Values
  With a Frequency Map, Remove Duplicates While Preserving Order, Convert List to Map, Build an
  Immutable Result, Implement equals/hashCode
- **Java Streams (7):** Filter and Transform a List, Group Employees by Department, Find Duplicate
  Values, Flatten Nested Lists, Partition Values, Find Maximum by Property, Calculate Aggregated
  Statistics
- **OOP Design (5):** Builder Pattern, Factory Pattern, Strategy Pattern, Notification Service
  design, Parking Lot Model design
- **Concurrency (6):** Thread-Safe Counter, Producer–Consumer With BlockingQueue, Parallel Task
  Execution With ExecutorService, CompletableFuture Aggregation, Prevent a Race Condition,
  Read-Write Lock Example
- **Core Java (7):** String Immutability, Defensive Copying, Comparable vs Comparator, Checked vs
  Unchecked Exceptions, Generics and Bounded Types, Optional Usage, Try-With-Resources

**Tasks:**
1. Java topic tags
2. Separate coding problems from knowledge demonstrations
3. Concurrency execution timeouts
4. Prevent background threads from continuing after tests finish
5. Explain common Java interview follow-up questions
6. Add "What interviewer may ask next" to Learn Mode

**Deliverables:** Java Collections, Streams, OOP design, Concurrency, and Core Java sections.

**Acceptance criteria:** Java challenges compile using the app's supported Java version;
concurrency challenges terminate correctly; each challenge explains the Java API being tested;
every challenge includes common follow-up questions; no challenge requires network access.

**Estimated effort:** 10–15 development days

---

## Milestone 11 — SDET and automation interview practice

**Goal:** Create a unique interview section for QA Automation and SDET roles.

- **API and Async (7):** Poll an API Until Completion, Retry With a Fixed Delay, Retry With
  Exponential Backoff, Add Retry Jitter, Handle HTTP Status Groups, Validate a Response Schema,
  Compare Expected and Actual Payloads
- **JSON and Test Data (7):** Remove Dynamic JSON Fields, Compare JSON Documents, Find Missing
  JSON Fields, Generate Unique Test Data, Mask Sensitive Values, Flatten Nested JSON, Merge Test
  Configuration
- **Test Result Analysis (8):** Calculate Pass Rate, Group Failures by Error, Detect Duplicate
  Failures, Identify Flaky Tests, Find Slowest Tests, Compare Two Test Runs, Build a Failure
  Fingerprint, Classify Known Failure vs Regression
- **Logs and Files (6):** Parse a Test Log, Count Errors by Service, Find Events Inside a Time
  Window, Read a Large File Efficiently, Compare CSV Test Results, Remove Dynamic Values From Log
  Messages
- **Concurrency and Infrastructure (7):** Thread-Safe Result Collector, Parallel Test Executor,
  Rate Limiter, Bounded Work Queue, Timeout Wrapper, Circuit Breaker, LRU Response Cache
- **Design Challenges (7):** Test Data Builder, Page Object Model, API Client Abstraction, Retry
  Policy Strategy, Reporter Factory, Test Listener, Result Aggregator

**Tasks:**
1. Realistic but local mock objects
2. Do not require a live API
3. Represent API responses with Java objects or JSON fixtures
4. Add failure scenarios
5. Explain how each problem connects to real automation work
6. Follow-up discussion: thread safety, idempotency, timeouts, retries, error handling,
   observability, test isolation

**Deliverables:** SDET and Automation category, realistic practice fixtures, local API simulation
utilities, interview follow-up content.

**Acceptance criteria:** exercises run offline; challenges represent realistic automation tasks;
async exercises have strict timeouts; sensitive data examples use fake values; each challenge
describes its real-world use.

**Estimated effort:** 10–15 development days

---

## Milestone 12 — Learn Mode

**Goal:** Help users understand patterns before attempting challenges.

**Learn Mode sections:** problem description, pattern explanation, input/output examples,
constraints, step-by-step approach, brute-force approach, optimized approach, time complexity,
space complexity, common mistakes, interview follow-up questions, reference solution.

**Tasks:**
1. Create Learn Mode screen
2. Expandable content sections
3. Syntax-highlighted Java solution
4. "Open in Editor" action
5. Progressive hints
6. Previous/next challenge navigation
7. Save the last opened learning section
8. Accessibility labels
9. Support scrolling on small screens

**Deliverables:** Learn Mode UI, hint system, explanation renderer, editor integration.

**Acceptance criteria:** users can study without changing their saved attempt; long explanations
remain readable on mobile; code remains copyable; hints reveal individually; reference solutions
clearly separated from starter code.

**Estimated effort:** 5–8 development days

---

## Milestone 13 — Practice Mode

**Goal:** Create the main coding workflow.

**Practice flow:** select category → select challenge → read description → open starter code →
write implementation → run visible tests → review failures → request hint if needed → run all
tests → mark challenge complete.

**Tasks:**
1. Split or tab-based problem/editor screen
2. Keep problem details accessible while editing
3. Run Visible Tests
4. Run All Tests
5. Display passed/failed tests, compilation errors, runtime errors, execution time
6. Auto-save user code
7. Restore unfinished attempts
8. Reset Code
9. Reveal Hint
10. View Solution with confirmation
11. Record attempt count
12. Record completion time
13. Record whether hints or solutions were used

**Deliverables:** complete Practice Mode workflow, auto-save, attempt restoration, detailed test
feedback.

**Acceptance criteria:** leaving the screen doesn't lose code; compilation errors display useful
line information; failed tests show useful visible input; hidden tests don't reveal protected
data; passing all tests updates progress.

**Estimated effort:** 7–10 development days

---

## Milestone 14 — Interview Mode

**Goal:** Simulate real coding interviews.

**Interview options:** 30/45/60-minute session, single challenge, mixed challenge set,
category-specific session, difficulty-specific session.

**Tasks:**
1. Interview configuration screen
2. Countdown timer
3. Select challenges by difficulty, category, unsolved status, previous failures, estimated duration
4. Disable hints, solution view, complexity answer, reset-to-solution
5. Allow starter-code reset
6. Auto-save during the session
7. Warn before ending the session
8. Final report: challenges attempted, challenges passed, test pass percentage, time per
   challenge, compilation attempts, runtime failures, unfinished challenges, patterns needing
   practice
9. Session review after completion

**Deliverables:** interview setup, timer, challenge selection, final performance report.

**Acceptance criteria:** timer survives screen rotation; app restart can restore an active
session; interview restrictions can't be bypassed through normal UI; session ending produces a
report; user code remains available after the session.

**Estimated effort:** 7–10 development days

---

## Milestone 15 — Progress tracking

**Goal:** Show measurable learning progress.

**Data to store:** exercise ID, attempt count, first/last-attempt date, solved status,
independent-solve status, best execution time, total coding time, hints used, solution revealed,
last saved code, failed patterns, interview-session results.

**Tasks:**
1. Room entities and DAO operations
2. `PracticeProgressRepository`
3. Progress dashboard
4. Display: total solved, independently solved, in progress, not started, completion by
   category/difficulty, recent activity, weak patterns, current streak
5. Challenge status indicators: not started, attempted, solved with help, solved independently
6. Data migration support
7. Reset Progress option with confirmation
8. Export progress to JSON
9. Import progress from JSON

**Deliverables:** persistent progress database, progress dashboard, import/export, data migration
tests.

**Acceptance criteria:** progress survives app restart; app updates don't erase progress;
exported data can be imported; challenge ID changes are handled through migrations; Reset
Progress never deletes normal Java projects.

**Estimated effort:** 6–9 development days

---

## Milestone 16 — Search, filters and navigation

**Goal:** Make a 100-challenge catalog easy to navigate.

**Search fields:** title, category, pattern, difficulty, tags.
**Filters:** Easy/Medium/Hard, solved/unsolved/attempted, bookmarked, estimated time, data
structure, interview pattern, Java, automation.
**Sorting:** recommended, difficulty, title, estimated duration, recently attempted, lowest
success rate.

**Tasks:**
1. Search bar
2. Filter bottom sheet
3. Active-filter indicators
4. Clear filters
5. Bookmark support
6. Random Challenge
7. Challenge of the Day
8. Continue Last Challenge
9. Recently viewed list
10. Preserve filters during navigation

**Deliverables:** search, filters, sorting, bookmarks, quick navigation actions.

**Acceptance criteria:** search results update quickly; filters can be combined; empty results
show a helpful message; selected filters survive rotation; catalog remains responsive with 100+
exercises.

**Estimated effort:** 4–6 development days

---

## Milestone 17 — UI and mobile experience

**Goal:** Make coding and learning comfortable on a phone.

**Tasks:**
1. Improve Practice home screen
2. Category cards: name, solved count, total count, progress percentage
3. Difficulty colors with accessible text labels
4. Improve editor toolbar: Run, Run Tests, Save, Undo, Redo, Problem, Hint
5. Landscape layout
6. Improve tablet layout
7. Support dark mode
8. Improve console output readability
9. Loading and execution states
10. Prevent duplicate Run taps
11. Empty and error states
12. TalkBack labels
13. Check color contrast
14. Test large font settings

**Deliverables:** updated Practice UI, responsive layouts, accessibility improvements, dark-mode
support.

**Acceptance criteria:** UI works on small phones and in landscape; text remains usable with
large fonts; buttons can't start duplicate executions; important status is never shown by color
alone.

**Estimated effort:** 6–10 development days

---

## Milestone 18 — Test automation strategy

**Goal:** Protect the growing catalog and practice engine.

**Unit tests:** model validation, catalog validation, search, filters, sorting, progress
calculations, output normalization, result evaluation, timer calculations, challenge selection.

**Compiler tests:** every starter file compiles after TODO implementation; every solution
compiles; every solution passes its tests; invalid code produces useful diagnostics.

**Integration tests:** catalog → editor → compiler → runner → evaluator → progress repository.

**Android UI tests:** open Practice, select category, open challenge, edit code, run test, see
failure, fix code, pass challenge, verify progress, restore unfinished attempt, start/finish
interview session.

**Catalog contract tests:** unique IDs, unique class names where required, valid category IDs,
valid difficulties, non-empty descriptions, ≥1 test, hidden tests exist, valid starter/solution
code, valid complexity metadata.

**Performance tests:** load 100+ challenges, search catalog, filter catalog, open editor, run
challenge, save code, restore progress.

**Deliverables:** expanded test suite, catalog validation task, end-to-end Practice test, CI test
reports.

**Acceptance criteria:** a broken reference solution fails CI; a duplicate exercise ID fails CI;
missing metadata fails CI; Practice flow has automated UI coverage; critical runner logic has
strong unit-test coverage.

**Estimated effort:** 6–10 development days

> **Cross-reference:** [Milestone 23](#milestone-23--test-coverage-hardening-jacoco--closing-the-gaps),
> appended below, is the concrete first step toward this milestone's coverage goals — it
> instruments *current* code with JaCoCo and closes today's known gaps, ahead of the new
> model/catalog/engine code this milestone's contract tests will need to cover.

---

## Milestone 19 — CI/CD and release quality

**Goal:** Automate validation for every pull request and release.

**PR pipeline:** compile project → run unit tests → run catalog validation → compile all
reference solutions → execute all challenge tests → run lint → build debug APK → upload test
reports → upload debug APK.

**Main branch pipeline:** run all PR checks → run Android instrumented tests → generate challenge
catalog report → build signed release candidate when configured.

**Release pipeline:** validate version → build release APK → generate checksums → generate
release notes → attach APK to GitHub Release → publish challenge count and category summary.

**Tasks:**
1. Split CI into clear jobs
2. Gradle caching
3. Job timeouts
4. Test-report artifacts
5. Catalog validation report
6. Dependency review
7. Secret scanning
8. Release versioning
9. Changelog generation

**Deliverables:** improved GitHub Actions, build artifacts, test reports, release workflow.

**Acceptance criteria:** PRs can't merge with failing catalog tests; CI clearly identifies the
failing challenge; APK artifacts are downloadable; no signing secrets stored in the repository;
release builds are reproducible.

**Estimated effort:** 4–7 development days

---

## Milestone 20 — Content quality review

**Goal:** Ensure the training content is accurate and useful.

**Review checklist per challenge:** clear title? unambiguous statement? defined inputs/outputs?
realistic constraints? examples match tests? starter code avoids revealing the answer? solution
handles edge cases? correct complexity? progressive hints? meaningful hidden tests? deterministic
output? runs safely on mobile? follow-up questions included?

**Tasks:**
1. Review all challenges
2. Remove duplicate challenges
3. Standardize terminology
4. Standardize Java formatting
5. Confirm difficulty ratings
6. Confirm estimated completion times
7. Manual mutation checks on selected problems: remove boundary check, change comparison
   operator, return wrong default, skip duplicate handling, break null handling
8. Confirm tests detect these wrong solutions

**Deliverables:** reviewed challenge catalog, content-quality checklist, corrected tests and
explanations.

**Acceptance criteria:** tests detect common incorrect implementations; difficulty ratings are
consistent; no solution has unexplained complexity; no challenge depends on unstable output
ordering; all 100 target challenges pass final validation.

**Estimated effort:** 7–12 development days

---

## Milestone 21 — Beta release

**Goal:** Test the complete product with real users.

**Beta scope:** 100 curated challenges, Learn/Practice/Interview modes, progress tracking, search
and filtering, Java section, SDET section.

**Tasks:**
1. Create beta APK
2. In-app feedback action
3. Ask testers to complete: one Easy, one Medium, one Java, one Automation challenge, one timed
   interview
4. Collect feedback on: problem clarity, mobile editor usability, test feedback, timer behavior,
   performance, crashes, missing interview topics
5. Fix critical issues
6. Measure: challenge load time, compilation time, test execution time, crash-free sessions,
   challenge completion rate

**Deliverables:** beta APK, feedback report, bug-fix release, performance results.

**Acceptance criteria:** no critical crashes; no user-code loss; no frozen execution; active
interview sessions restore correctly; test feedback is understandable without external help.

**Estimated effort:** 5–10 development days

---

## Milestone 22 — Version 2.0 release

**Goal:** Publish the expanded interview-training platform.

**Tasks:**
1. Update README screenshots
2. Update challenge counts automatically
3. Create final architecture diagram
4. Update `CHANGELOG.md`
5. Create release notes
6. Build final release APK
7. Run the complete validation pipeline
8. Tag the release: `v2.0.0`
9. Publish the GitHub Release
10. Add a product demo GIF or video
11. Repository description: "Mobile Java IDE and interview trainer with algorithms, data
    structures, Java and SDET coding challenges."
12. GitHub topics: `java`, `android`, `coding-interviews`, `data-structures`, `algorithms`,
    `java-ide`, `sdet`, `automation-testing`, `interview-preparation`

**Release acceptance criteria:** 100 curated exercises available; all reference solutions pass;
Learn/Practice/Interview modes work; progress persists; search and filters work; CI passes;
release APK available; documentation matches the released application.

**Estimated effort:** 2–4 development days

---

## Milestone 23 — Test coverage hardening (JaCoCo + closing the gaps)

**Status: identified from a direct coverage audit** (see chat history around 2026-08-31) — no
coverage tool was configured in the project, so the audit was a static class-by-class comparison
of `app/src/main/java` against `app/src/test`/`app/src/androidTest`. It found roughly 24 of 55
main classes (~44%) with any dedicated test at all, and several structural gaps: the entire `data`
(Room persistence) package has zero tests; APK signing/packaging (`Packager`, `DebugSigningKey`,
`ResourceCompiler`, `ApkInstaller`) is untested; Git commit signing (`GitCommitSigner`,
`PgpKeyManager`) is untested; `JavaRunner` has no unit-level test of its own (only exercised
indirectly through the instrumented `InterviewExercisesRunTest`); and most of the UI layer —
including the Practice screens documented in Milestone 1 — has no test at all beyond five
instrumented flow tests.

**Goal:** Give the project an actual, automated coverage signal (not a manual file-matching
estimate), and close the highest-value gaps that signal will surface — before the model/catalog/
engine work in Milestones 2–4 adds a large volume of new, equally untested code on top of an
already-thin base.

**Tasks:**

1. Add the JaCoCo Gradle plugin to `app/build.gradle.kts`, wired to `testDebugUnitTest` (and, for
   the parts only exercisable on-device, `connectedDebugAndroidTest`) so coverage reports are
   generated from real runs, not estimated.
2. Produce merged unit + instrumented coverage where both exist for the same class (e.g.
   `ProjectStorage`, which already has both a JVM and an instrumented test).
3. Add a `jacocoTestReport` task producing an HTML + XML report, and wire it into CI as an
   uploaded artifact alongside the existing test-report artifacts.
4. Set an initial minimum-coverage gate scoped to non-UI, non-Activity code (the `compiler`,
   `util`, `completion`, `model`, and `data` packages) — start at a baseline that reflects current
   reality plus this milestone's new tests, not an aspirational number pulled from nowhere; ratchet
   it up in later milestones as coverage improves. Do not gate the `ui` package yet — Activities
   need Espresso/instrumented coverage, which is a larger, separate effort (see Milestone 18).
5. Close gaps, in priority order:
   - **`data/` (Room):** add `AppDao`/`AppDatabase` tests using an in-memory Room database
     (`Room.inMemoryDatabaseBuilder`) — covering `EditorState` upsert/read-by-path and `EventEntry`
     insert/query. This package currently has literally zero coverage and backs both cursor
     restoration and the activity log.
   - **`compiler/Packager` and `compiler/ResourceCompiler`:** unit tests around the APK
     build/signing path — at minimum, that `Packager` produces a validly-signed APK from known
     input classes/resources, and that `ResourceCompiler` generates `R.java` with the expected
     constants for a small fixture `res/` directory.
   - **`compiler/JavaRunner`:** today it's only exercised transitively via the instrumented
     `InterviewExercisesRunTest`. Where feasible without `DexClassLoader` (e.g. `findMain`'s
     reflection-based main-method discovery logic), add a JVM-level unit test; document plainly
     where a real on-device test remains necessary and out of scope for the JVM suite.
   - **`vcs/GitCommitSigner` and `vcs/PgpKeyManager`:** unit tests for key generation/loading and
     signature creation against a temp Git repo, independent of `GitRepositoryTest`'s existing
     (unsigned) coverage.
   - **`util/ApkInstaller`, `util/CrashHandler`, `util/IconUtils`:** add unit tests for the
     pieces of each that don't require a live install/crash/launcher-icon pipeline (e.g. intent
     construction, crash-log formatting, icon resource resolution logic).
   - **Practice UI (`PracticeActivity`, `PracticeDetailActivity`, `PracticeAdapter`):** at least
     one instrumented test exercising the Run & Check flow end-to-end (open an exercise, tap Run &
     Check, assert the pass banner), since this is the one flow the practice-expansion plan is
     actively about to rebuild — it should have a regression net in place first.
6. Document the coverage workflow in [`TESTING.md`](TESTING.md): how to generate the report
   locally (`./gradlew jacocoTestReport`), where it lands, and what the CI gate checks.

**Deliverables:** JaCoCo wired into the build and CI, an initial coverage gate on non-UI code, new
tests closing the `data`/`Packager`/`ResourceCompiler`/`JavaRunner`/`vcs`/`ApkInstaller`/
`CrashHandler`/`IconUtils` gaps, one instrumented Practice-flow regression test, and updated
`docs/TESTING.md`.

**Acceptance criteria:**
- `./gradlew jacocoTestReport` produces an HTML report with real (not estimated) numbers
- CI uploads the coverage report as a build artifact
- The non-UI coverage gate fails the build if coverage regresses below the set baseline
- Every class named in Tasks §5 has at least one direct unit or instrumented test
- No existing test is weakened, skipped, or deleted to hit a coverage number
- `docs/TESTING.md` accurately describes the new coverage workflow

**Estimated effort:** 4–6 development days

---

## Recommended challenge distribution

**Core Algorithms and Data Structures: 60**
Fundamentals: 6 · Arrays and Strings: 8 · Hash Maps and Sets: 6 · Two Pointers: 6 · Sliding
Window: 6 · Linked Lists: 7 · Stacks and Queues: 6 · Trees and BST: 8 · Graphs: 7

**Advanced Patterns: 20**
Heaps: 4 · Intervals: 4 · Binary Search Patterns: 4 · Dynamic Programming: 4 · Backtracking: 4

**Java-Specific Challenges: 20**
Collections: 5 · Streams: 5 · OOP and Design Patterns: 4 · Concurrency: 4 · Core Java: 2

**SDET and Automation Challenges: 20**
API and Async Workflows: 4 · JSON and Test Data: 4 · Failure Analysis: 4 · Logs and Files: 3 ·
Concurrency and Infrastructure: 3 · Automation Design: 2

Some challenges may carry multiple tags while belonging to one primary category.

## Recommended delivery phases

| Phase | Milestones | Outcome | Estimated duration |
|---|---|---|---|
| 1 — Foundation | 0, 1, 2, 3, 4, 5 | A maintainable catalog and proper multi-test execution engine | 3–5 weeks |
| 2 — Core interview content | 6, 7, 8, 9 | Strong algorithms and data-structure coverage | 6–10 weeks |
| 3 — Product experience | 12, 13, 14, 15, 16, 17 | A complete mobile interview-training experience | 6–9 weeks |
| 4 — Java and SDET differentiation | 10, 11 | A unique product for Java, SDET and Automation Engineer interviews | 4–7 weeks |
| 5 — Quality and release | 18, 19, 20, 21, 22 | A tested, documented and downloadable Version 2.0 release | 4–7 weeks |

Milestone 23 (test coverage hardening) slots into Phase 1, immediately after Milestone 1 and
before the Milestone 2 model rewrite — instrumenting and hardening the *current* code first means
the coverage gate is already in place before the new model/catalog/engine code from Milestones 2–4
lands.

## Total estimated delivery

- **Solo developer, part-time:** 5–8 months
- **Solo developer, full-time:** 3–5 months
- **Small team:** 2–3 months

**Recommended approach:** do not add all 100 challenges before building the new model and runner.

Correct order: 1) refactor the model, 2) split the catalog, 3) build multi-test execution,
4) separate starter and solution code, 5) add 10 pilot challenges, 6) build Learn and Practice
modes, 7) validate the complete workflow, 8) expand the catalog, 9) add Interview Mode, 10) add
progress and final quality controls.

## Definition of done for every challenge

A challenge is complete only when:

1. It has a stable unique ID
2. It has a clear title
3. It belongs to a valid category
4. It has a difficulty level
5. It has a clear problem description
6. It has defined constraints
7. It includes at least two examples
8. It has starter code
9. It has a reference solution
10. It includes at least three test cases
11. It includes at least one hidden test
12. It includes edge-case tests
13. It includes progressive hints
14. It explains time complexity
15. It explains space complexity
16. It contains relevant pattern tags
17. Its reference solution compiles
18. Its reference solution passes every test
19. At least one common wrong implementation fails its tests
20. It runs within the mobile execution timeout
21. It appears correctly in search and filters
22. Progress is saved after an attempt
23. Learn Mode displays its explanation
24. Practice Mode opens its starter code
25. Interview Mode protects its solution and hidden tests

## Final product positioning

Java IDE Mobile should become: **a mobile Java IDE and structured interview trainer where
developers can learn, write, compile, run and validate Java solutions directly on Android.**

Its strongest difference should be:
- Real Java compilation on the device
- Structured data-structure and algorithm practice
- Java Collections, Streams, OOP and concurrency challenges
- SDET and Automation Engineer coding challenges
- Learn, Practice and timed Interview modes
- Hidden tests and detailed feedback
- Persistent progress and weak-pattern analysis
