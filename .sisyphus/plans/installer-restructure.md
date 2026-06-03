# Echo VR Installer — PCVR Guided Wizard Restructure

## TL;DR

> **Quick Summary**: Transform the PCVR installer from a flat button layout into a guided step-by-step wizard that asks the user's ownership status, guides through installation, and seamlessly transitions into Discord-based patching with an embedded JavaFX WebView.
>
> **Deliverables**:
> - Simplified FrameMain (only "Update Echo" + new wizard entry)
> - User-type question dialog (owner vs new player)
> - Modified FramePCDownload with "Next" button + path-sharing mechanism
> - Redesigned FramePCPatcher with embedded JavaFX Discord WebView
> - Hybrid Discord automation: auto-navigate + manual fallback
> - Optional post-install patch offerings for Echo owners
> - Full JUnit 5 TDD test suite for all new code
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES — 4 waves with up to 6 parallel tasks
> **Critical Path**: Wave 1 (Infrastructure) → Wave 2 (Types/Test infra) → Wave 3 (Core frames) → Wave 4 (Discord WebView) → Wave 5 (Integration)

---

## Context

### Original Request
Restructure the Echo VR Installer so that the PCVR "Install Echo" button opens a guided wizard instead of directly opening the install dialog. The wizard should ask whether the user owns Echo on their Meta account, guide through installation, and if the user is new, open a redesigned patcher with embedded Discord (JavaFX WebView) for semi-automated patch URL extraction.

### Interview Summary

**Key Discussions**:
- **Platform Scope**: PCVR only; Quest restructure follows later
- **Wizard Structure**: Multi-window sequence (user-type dialog → install → optional patch), passing data between windows
- **Discord Integration**: JavaFX WebView as separate popup; hybrid approach: auto-navigate to channel/message + highlight, user clicks reaction, auto-detect private thread, auto-extract URL. Manual fallback (existing copy-paste) as safety net.
- **Main Window**: Hide all buttons except "Update Echo (PC)"; add new wizard entry replacing "Step 1: Install Echo"
- **Post-Install (Owner)**: "Next" offers optional patches (No licence, Steam/Revive)
- **Testing**: TDD for all new code using JUnit 5 (configured but no tests exist)

**Research Findings**:
- `build.gradle` has JavaFX 17.0.2 dependencies commented out (lines 47-48)
- JUnit 5 is configured (`testImplementation`, `testRuntimeOnly`, `useJUnitPlatform()`) but zero test files exist
- Project uses Java modules (`module-info.java`) with `jlink`/`jpackage` for distribution
- `FramePCPatcher` already has a 6-step guided flow with section panels — base to redesign
- `FramePCDownload` downloads `ready-at-dawn-echo-arena.zip` then auto-updates from `https://files.echovr.de/updates/files`
- Discord invite: `https://discord.gg/KqjqdNUaHR`

### Metis Review

**Identified Gaps** (addressed):
- **JavaFX infrastructure missing**: Dependencies commented out, module system unaware, jlink/jpackage not configured for JavaFX → Added Wave 1 infrastructure tasks
- **Discord DOM fragility**: Selectors change over time, auto-extraction unreliable without fallback → Added externalized selector config + manual fallback in Wave 4 task design
- **JFXPanel lifecycle risks**: `Platform.setImplicitExit(false)` required, `ViewPainter.ROOT_PATHS` memory leak, EDT↔FX thread discipline → Added explicit lifecycle management tasks + test coverage
- **No test infrastructure despite JUnit config**: Tests configured but never used → Added test infrastructure setup as first Wave 2 task

---

## Work Objectives

### Core Objective
Convert the PCVR installer's flat button entry into a guided step-by-step wizard that determines user ownership status, walks through installation, and seamlessly transitions to Discord-integrated patching when needed — all with TDD test coverage.

### Concrete Deliverables
- `FrameMain.java` — simplified (only Update Echo + wizard entry visible; other buttons hidden)
- `UserTypeDialog.java` — new dialog asking "Own Echo on Meta?" vs "New player?"
- `FramePCDownload.java` — modified with "Next" button and path-sharing capability
- `FramePCPatcher.java` — redesigned with embedded JavaFX Discord WebView
- `DiscordWebView.java` — new JavaFX component for embedded Discord browser
- `DiscordNavigator.java` — auto-navigation logic (channel finder, reaction detection, thread extraction)
- `discord-selectors.properties` — externalized DOM selector configuration
- `WizardState.java` — state object passed between wizard windows
- Full JUnit 5 test suite: `UserTypeDialogTest`, `WizardStateTest`, `FramePCDownloadTest`, `FramePCPatcherTest`, `DiscordNavigatorTest`
- `module-info.java` — updated with JavaFX module requires
- `build.gradle` — JavaFX dependencies uncommented, jlink/jpackage JavaFX modules added

### Definition of Done
- [ ] `gradle build` — compiles successfully with JavaFX modules
- [ ] `gradle test` — all TDD tests pass (0 failures)
- [ ] Clicking wizard entry on FrameMain → UserTypeDialog appears
- [ ] Selecting "I own Echo" → install window → "Next" → optional patches offered
- [ ] Selecting "I'm a new player" → install window → "Next" → patcher with Discord WebView opens
- [ ] Discord WebView: embeds correctly, navigates to server, user can interact
- [ ] Manual fallback: copy-paste patch URL still works if auto-extract fails
- [ ] Update Echo (PC) button still functions independently

### Must Have
- Guided wizard flow: user-type question → install → (optional) patch
- Path from install window automatically passed to patcher
- Embedded Discord WebView in patcher with manual fallback
- TDD test suite for all new code
- JavaFX properly integrated with module system and jpackage

### Must NOT Have (Guardrails)
- **NO touching Quest-side code** (FrameQuestDownload, FrameQuestPatcher, InstallerQuest, GetLogFilesFromQuest) — scope boundary
- **NO changes to FrameSteamPatcher** (used as-is for optional post-install)
- **NO changes to FramePCEchoUpdate** (kept as-is)
- **NO breaking existing download/patching logic** — only UI flow changes
- **NO hardcoded Discord DOM selectors** — must be externalized in properties file
- **NO blocking the EDT** — all Discord WebView operations on FX thread, UI updates on EDT
- **NO skipping JFXPanel lifecycle cleanup** — `Platform.setImplicitExit(false)` and explicit `Platform.exit()` on app close

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES (JUnit 5 configured)
- **Automated tests**: TDD
- **Framework**: JUnit 5 (Jupiter)
- **TDD workflow**: Each task follows RED (failing test) → GREEN (minimal impl) → REFACTOR

### QA Policy
Every task MUST include agent-executed QA scenarios (see TODO template below).
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Frontend/UI**: Use Playwright (playwright skill) — Navigate, interact, assert DOM, screenshot
- **TUI/CLI**: Use interactive_bash (tmux) — Run command, send keystrokes, validate output
- **API/Backend**: Use Bash (curl) — Send requests, assert status + response fields
- **Library/Module**: Use Bash (bun/node REPL) — Import, call functions, compare output
- **Java/Discord WebView**: Use Bash (`gradle test`) for unit tests + manual verification via screenshots

---

## Execution Strategy

### Parallel Execution Waves

> Maximize throughput by grouping independent tasks into parallel waves.
> Each wave completes before the next begins.
> Target: 5-7 tasks per wave.

```
Wave 1 (Start Immediately — build infrastructure, zero code deps):
├── Task 1: Uncomment JavaFX dependencies + update jlink/jpackage [quick]
├── Task 2: Update module-info.java with JavaFX requires [quick]
├── Task 3: Create discord-selectors.properties + SelectorConfig class [quick]
├── Task 4: Set up test infrastructure (create test dirs, verify JUnit runs) [quick]

Wave 2 (After Wave 1 — core types + TDD foundation, MAX PARALLEL):
├── Task 5: Create WizardState class (user type, install path) + tests [quick]
├── Task 6: RED: UserTypeDialog tests → GREEN: UserTypeDialog implementation [quick]
├── Task 7: RED: SelectorConfig tests → GREEN: SelectorConfig implementation [quick]

Wave 3 (After Wave 2 — frame modifications, MAX PARALLEL):
├── Task 8: Simplify FrameMain (hide buttons, add wizard entry) + tests [quick]
├── Task 9: Modify FramePCDownload (add Next button, WizardState integration) + tests [deep]
├── Task 10: Build optional-patches post-install panel + tests [unspecified-high]

Wave 4 (After Wave 3 — Discord WebView, DEPENDENCY HEAVY):
├── Task 11: RED: DiscordWebView class (JavaFX Stage + WebView embed) → GREEN [deep]
├── Task 12: RED: DiscordNavigator (auto-navigate, reaction finder, thread detection) → GREEN [deep]
├── Task 13: Integrate DiscordWebView into FramePCPatcher redesign [deep]
├── Task 14: Implement JFXPanel lifecycle management (Platform exit, EDT crossing) [unspecified-high]
├── Task 15: Wire manual fallback (copy-paste link) as safety net [quick]

Wave 5 (After Wave 4 — Integration + Polish):
├── Task 16: Wire full wizard sequence (UserTypeDialog → FramePCDownload → FramePCPatcher) [deep]
├── Task 17: Path-sharing integration between FramePCDownload and FramePCPatcher [quick]
├── Task 18: Final integration tests — end-to-end TDD [deep]

Wave FINAL (After ALL tasks — 4 parallel reviews, then user okay):
├── Task F1: Plan Compliance Audit (oracle)
├── Task F2: Code Quality Review (unspecified-high)
├── Task F3: Real Manual QA (unspecified-high + playwright)
└── Task F4: Scope Fidelity Check (deep)
-> Present results -> Get explicit user okay
```

**Critical Path**: T1 → T4 → T5 → T8 → T9 → T11 → T13 → T16 → T18 → FINAL
**Parallel Speedup**: ~55% faster than sequential (4 tasks in Wave 1, 3 in Wave 2, 3 in Wave 3, 5 in Wave 4)
**Max Concurrent**: 5 (Wave 4)

### Dependency Matrix

- **1**: — — 2, 3, 4 — None (can start immediately)
- **2**: 1 — 5, 6, 7 — None
- **3**: 1 — 7, 15 — None
- **4**: 1 — 5, 6, 7 — None
- **5**: 2 — 6, 8, 9, 16, 17 — T2 (module-info)
- **6**: 5 — 16 — T5 (WizardState)
- **7**: 2, 3 — 12 — T2 (module-info), T3 (selectors.properties)
- **8**: 5 — 9, 16 — T5 (WizardState)
- **9**: 5, 8 — 10, 11, 13, 16 — T5 (WizardState), T8 (FrameMain)
- **10**: 9 — 16 — T9 (FramePCDownload)
- **11**: 2, 5, 9 — 13 — T2 (module-info), T5 (WizardState), T9 (FramePCDownload)
- **12**: 7 — 13 — T7 (SelectorConfig)
- **13**: 9, 11, 12 — 16 — T9, T11, T12
- **14**: 11 — 13 — T11 (DiscordWebView)
- **15**: 3, 13 — 16 — T3 (selectors.properties), T13 (FramePCPatcher redesign)
- **16**: 5, 6, 8, 9, 13, 15 — 18 — Multiple
- **17**: 5, 9 — 16, 18 — T5, T9
- **18**: 16, 17 — FINAL — T16, T17

### Agent Dispatch Summary

- **Wave 1**: **4** — T1 → `quick`, T2 → `quick`, T3 → `quick`, T4 → `quick`
- **Wave 2**: **3** — T5 → `quick`, T6 → `quick`, T7 → `quick`
- **Wave 3**: **3** — T8 → `quick`, T9 → `deep`, T10 → `unspecified-high`
- **Wave 4**: **5** — T11 → `deep`, T12 → `deep`, T13 → `deep`, T14 → `unspecified-high`, T15 → `quick`
- **Wave 5**: **3** — T16 → `deep`, T17 → `quick`, T18 → `deep`
- **FINAL**: **4** — F1 → `oracle`, F2 → `unspecified-high`, F3 → `unspecified-high`, F4 → `deep`

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info + QA Scenarios.
> **A task WITHOUT QA Scenarios is INCOMPLETE. No exceptions.**

- [x] 1. **Uncomment JavaFX dependencies and update jlink/jpackage config**

  **What to do**:
  - Uncomment JavaFX dependencies in `build.gradle` (lines 47-48: `javafx-controls` and `javafx-fxml` — also add `javafx-web` for WebView support)
  - Add `javafx-web` dependency: `implementation "org.openjfx:javafx-web:${javafxVersion}"`
  - Add JavaFX platform classifier for the current OS if needed (e.g., `javafx-graphics` with `win`/`mac`/`linux` classifier)
  - Update `jlink` block to include JavaFX modules: `javafx.controls`, `javafx.fxml`, `javafx.web`, `javafx.graphics`, `javafx.base`
  - Update `applicationDefaultJvmArgs` if needed to include `--module-path` for JavaFX
  - Verify with `gradle build` that dependencies resolve and compile
  - Test cases: Verify `gradle dependencies` shows JavaFX modules; verify `gradle build` succeeds

  **Must NOT do**:
  - Do NOT change the Java version (stay on Java 17)
  - Do NOT change the module name (`bl00dy_c0d3_.echovr_installer`)
  - Do NOT remove existing dependencies or JVM args

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single-file build config change with well-known JavaFX dependency patterns
  - **Skills**: []
  - **Skills Evaluated but Omitted**: N/A

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3, 4)
  - **Blocks**: Tasks 2, 5, 11
  - **Blocked By**: None

  **References** (CRITICAL):
  - `build.gradle:1-113` — Current Gradle config with commented JavaFX deps at lines 47-48, jlink config at lines 70-105, JVM args at lines 33-39. Modify these sections.
  - `build.gradle:19` — `javafxVersion = '17.0.2'` already defined — use this variable for all JavaFX dependency versions
  - Official docs: JavaFX 17 runtime image with jlink requires `--add-modules javafx.controls,javafx.web,javafx.fxml`

  **Acceptance Criteria**:
  **TDD**:
  - [ ] `gradle build` → BUILD SUCCESSFUL (no dependency resolution errors)
  - [ ] `gradle dependencies --configuration runtimeClasspath | grep javafx` → shows javafx-controls, javafx-web, javafx-fxml

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Gradle build succeeds with JavaFX dependencies uncommented
    Tool: Bash
    Preconditions: Working directory is project root, Java 17 available
    Steps:
      1. Run: gradle clean build --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert stdout does NOT contain "Could not resolve" or "Could not find"
    Expected Result: Build completes without dependency resolution errors
    Failure Indicators: "FAILURE", "Could not resolve", "Could not find javafx"
    Evidence: .sisyphus/evidence/task-1-build-output.txt

  Scenario: JavaFX dependencies appear in dependency tree
    Tool: Bash
    Preconditions: Build succeeded
    Steps:
      1. Run: gradle dependencies --configuration runtimeClasspath 2>&1
      2. Assert stdout contains "org.openjfx:javafx-controls"
      3. Assert stdout contains "org.openjfx:javafx-web"
      4. Assert stdout contains "org.openjfx:javafx-fxml"
    Expected Result: All three JavaFX modules listed in runtime dependencies
    Failure Indicators: Missing any of the three JavaFX dependency lines
    Evidence: .sisyphus/evidence/task-1-deps-verify.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-1-build-output.txt` — full `gradle build` output
  - [ ] `.sisyphus/evidence/task-1-deps-verify.txt` — dependency tree grep output

  **Commit**: YES (with Wave 1 group)
  - Message: `build(javafx): uncomment JavaFX dependencies and update jlink/jpackage config`
  - Files: `build.gradle`
  - Pre-commit: `gradle build`

- [x] 2. **Update module-info.java with JavaFX module requires**

  **What to do**:
  - Read current `module-info.java` to understand existing module structure
  - Add `requires javafx.controls;`, `requires javafx.fxml;`, `requires javafx.web;`, `requires javafx.graphics;`
  - Add `requires javafx.base;` if not already transitively included
  - If any `opens` directives needed for JavaFX FXML, add them (not needed for WebView-based approach)
  - Verify with `gradle build` that module resolution succeeds
  - Test cases: Verify `gradle build` succeeds; verify module descriptor is valid

  **Must NOT do**:
  - Do NOT remove existing `requires` directives
  - Do NOT change the module name
  - Do NOT add `opens` directives unless explicitly needed

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single-file change adding well-defined module directives
  - **Skills**: []
  - **Skills Evaluated but Omitted**: N/A

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3, 4)
  - **Blocks**: Tasks 5, 7, 11
  - **Blocked By**: Task 1 (needs JavaFX deps resolved)

  **References** (CRITICAL):
  - `src/main/java/module-info.java` — Current module descriptor. Must read first to understand existing exports/requires structure.
  - `build.gradle:31-32` — `mainModule = 'bl00dy_c0d3_.echovr_installer'` — confirms module name

  **Acceptance Criteria**:
  **TDD**:
  - [ ] `gradle build` → BUILD SUCCESSFUL (module resolution passes)
  - [ ] `gradle build 2>&1 | grep -i "module not found"` → no output

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Module compilation succeeds with JavaFX requires
    Tool: Bash
    Preconditions: Task 1 completed (JavaFX deps available)
    Steps:
      1. Run: gradle clean compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert stdout does NOT contain "module not found" (case insensitive)
    Expected Result: Java module compilation passes with JavaFX modules resolved
    Failure Indicators: "error: module not found: javafx", any compilation error
    Evidence: .sisyphus/evidence/task-2-module-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-2-module-compile.txt` — full `gradle compileJava` output

  **Commit**: YES (with Wave 1 group)
  - Message: `build(javafx): add JavaFX module requires to module-info.java`
  - Files: `src/main/java/module-info.java`
  - Pre-commit: `gradle compileJava`

- [x] 3. **Create discord-selectors.properties and SelectorConfig class skeleton**

  **What to do**:
  - Create `src/main/resources/discord-selectors.properties` with placeholder selectors:
    - `discord.server.invite=https://discord.gg/KqjqdNUaHR`
    - `discord.channel.name=quest-patch` (or correct channel name — use placeholder)
    - `discord.reaction.emoji=🎮` (or correct emoji — use placeholder)
    - `discord.message.selector=[data-list-item-id]` (CSS selector for reaction message — placeholder)
    - `discord.thread.selector=[class*="thread"]` (CSS selector for private thread — placeholder)
  - Create `src/main/java/bl00dy_c0d3_/echovr_installer/SelectorConfig.java` with:
    - `Properties` loading from classpath
    - Typed getter methods: `getServerInvite()`, `getChannelName()`, `getReactionEmoji()`, `getMessageSelector()`, `getThreadSelector()`
    - Fallback default values when properties file is missing
  - Test cases: Verify properties load from classpath (test will be in Task 7)

  **Must NOT do**:
  - Do NOT hardcode selectors in Java code — must be externalized
  - Do NOT implement Discord navigation logic here (that's Task 12)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple POJO + properties file creation with well-known patterns
  - **Skills**: []
  - **Skills Evaluated but Omitted**: N/A

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 4)
  - **Blocks**: Tasks 7, 12, 15
  - **Blocked By**: None

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/Helpers.java:24-29` — Existing static utility pattern with OS detection. SelectorConfig should follow similar utility style.
  - `src/main/resources/` — Directory where existing resources (images, fonts) live. Properties file goes here.

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Wait for Task 7 (RED tests) before implementing full logic
  - [ ] For now: file exists at `src/main/resources/discord-selectors.properties` with valid key=value format
  - [ ] For now: `SelectorConfig.java` compiles with stub methods

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Properties file exists and is readable
    Tool: Bash
    Preconditions: Working directory is project root
    Steps:
      1. Run: ls -la src/main/resources/discord-selectors.properties
      2. Assert file exists (exit code 0)
      3. Run: cat src/main/resources/discord-selectors.properties
      4. Assert output contains "discord.server.invite="
      5. Assert output contains "discord.channel.name="
    Expected Result: Properties file exists with all required keys
    Failure Indicators: File not found, missing keys
    Evidence: .sisyphus/evidence/task-3-selectors-exist.txt

  Scenario: SelectorConfig compiles without errors
    Tool: Bash
    Preconditions: Working directory is project root
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: SelectorConfig.java compiles cleanly
    Failure Indicators: Compilation errors mentioning SelectorConfig
    Evidence: .sisyphus/evidence/task-3-selectorconfig-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-3-selectors-exist.txt` — properties file contents
  - [ ] `.sisyphus/evidence/task-3-selectorconfig-compile.txt` — gradle compile output

  **Commit**: YES (with Wave 1 group)
  - Message: `feat(config): add externalized Discord DOM selectors config`
  - Files: `src/main/resources/discord-selectors.properties`, `src/main/java/bl00dy_c0d3_/echovr_installer/SelectorConfig.java`
  - Pre-commit: `gradle build`

- [x] 4. **Set up test infrastructure and verify JUnit 5 runs**

  **What to do**:
  - Create test directory structure: `src/test/java/bl00dy_c0d3_/echovr_installer/`
  - Create a smoke test: `SmokeTest.java` with one `@Test` that asserts `true`
  - Verify `gradle test` runs and reports 1 test passed
  - Add `src/test/resources/` directory for test resources
  - Verify JUnit 5 Platform is active (not JUnit 4 fallback)
  - Add `module-info.java` test module descriptor or ensure test classpath works with module system
  - Test cases: Smoke test passes

  **Must NOT do**:
  - Do NOT add new test dependencies — JUnit 5 already configured
  - Do NOT configure test coverage or CI — just verify basic test execution works
  - Do NOT remove the smoke test after verification (it stays as build health check)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Trivial file creation + build verification
  - **Skills**: []
  - **Skills Evaluated but Omitted**: N/A

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2, 3)
  - **Blocks**: Tasks 5, 6, 7
  - **Blocked By**: None

  **References** (CRITICAL):
  - `build.gradle:42-44` — JUnit 5 dependencies already configured: `testImplementation`, `testRuntimeOnly`
  - `build.gradle:52-54` — `test { useJUnitPlatform() }` — confirms JUnit 5 Platform
  - `src/main/java/module-info.java` — Module system may require test module descriptor or `--add-opens` flags

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/SmokeTest.java`
  - [ ] `gradle test` → PASS (1 test, 0 failures)
  - [ ] `gradle test --info 2>&1 | grep "TestEngine"` → shows JUnit Jupiter

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: JUnit 5 executes smoke test successfully
    Tool: Bash
    Preconditions: Working directory is project root, JUnit 5 deps resolved
    Steps:
      1. Run: gradle clean test --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert stdout contains "1 test" or test count indicator
      4. Assert stdout does NOT contain "FAILED" or "test failed"
    Expected Result: JUnit 5 Platform runs the smoke test and reports success
    Failure Indicators: "BUILD FAILED", "No tests found", "FAILED", "JUnit Vintage" (wrong engine)
    Evidence: .sisyphus/evidence/task-4-smoke-test.txt

  Scenario: Test report is generated
    Tool: Bash
    Preconditions: Smoke test ran successfully
    Steps:
      1. Run: ls build/reports/tests/test/
      2. Assert directory exists
      3. Run: find build/test-results/test/ -name "*.xml" | head -5
      4. Assert at least one XML test result file exists
    Expected Result: Gradle generates test reports after successful test run
    Failure Indicators: No report directory, no XML results
    Evidence: .sisyphus/evidence/task-4-test-report.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-4-smoke-test.txt` — full `gradle test` output
  - [ ] `.sisyphus/evidence/task-4-test-report.txt` — test report directory listing

  **Commit**: YES (with Wave 1 group)
  - Message: `test(infra): set up JUnit 5 test infrastructure with smoke test`
  - Files: `src/test/java/bl00dy_c0d3_/echovr_installer/SmokeTest.java`
  - Pre-commit: `gradle test`

- [x] 5. **RED → GREEN: Create WizardState class with tests**

  **What to do**:
  - **RED**: Write `WizardStateTest.java` first with test cases for:
    - Default state: user type is null, install path is empty
    - Setting user type to OWNER returns correct enum value
    - Setting user type to NEW_PLAYER returns correct enum value
    - Setting install path and retrieving it
    - Path normalization (trailing slashes, backslashes on Windows)
    - Immutability of getters (defensive copies if needed)
  - **GREEN**: Create `WizardState.java` with:
    - `enum UserType { OWNER, NEW_PLAYER }`
    - Private fields: `UserType userType`, `String installPath`
    - Getter/setter for each field
    - Path normalization in setter (replace `\` with `/`, trim trailing `/`)
    - `toString()` for debugging
  - Run `gradle test` → all WizardState tests pass

  **Must NOT do**:
  - Do NOT add UI logic or Swing components to WizardState (pure data class)
  - Do NOT make WizardState a Singleton — each wizard run gets its own instance
  - Do NOT persist state to disk

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple data class with straightforward TDD — well-defined inputs/outputs
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 7)
  - **Blocks**: Tasks 6, 8, 9, 16, 17
  - **Blocked By**: Wave 1 completion (test infra + module-info)

  **References** (CRITICAL):
  - `src/test/java/bl00dy_c0d3_/echovr_installer/SmokeTest.java` — Existing test to use as template for JUnit 5 annotations and assertions
  - `build.gradle:42-44` — JUnit 5 API available: `org.junit.jupiter.api.Assertions.*`, `@Test`, `@BeforeEach`

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/WizardStateTest.java` (≥5 test methods)
  - [ ] Source file created: `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java`
  - [ ] `gradle test --tests "*WizardStateTest"` → PASS (all tests green)

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: WizardState holds and returns correct user type and path
    Tool: Bash (gradle test)
    Preconditions: JUnit 5 infrastructure working
    Steps:
      1. Run: gradle test --tests "*WizardStateTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert all test methods pass (0 failures, 0 errors)
    Expected Result: All 5+ WizardState tests pass
    Failure Indicators: Any test failure or error
    Evidence: .sisyphus/evidence/task-5-wizardstate-tests.txt

  Scenario: Path normalization handles Windows backslashes
    Tool: Bash (gradle test)
    Preconditions: WizardStateTest includes path normalization test
    Steps:
      1. Run: gradle test --tests "*WizardStateTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Path normalization test passes
    Failure Indicators: Test failure on backslash handling
    Evidence: .sisyphus/evidence/task-5-path-normalize.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-5-wizardstate-tests.txt` — full test output
  - [ ] `.sisyphus/evidence/task-5-path-normalize.txt` — path normalization test output

  **Commit**: YES
  - Message: `feat(wizard): add WizardState with user type and path sharing`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/WizardStateTest.java`
  - Pre-commit: `gradle test --tests "*WizardStateTest"`

- [x] 6. **RED → GREEN: Create UserTypeDialog with tests**

  **What to do**:
  - **RED**: Write `UserTypeDialogTest.java` with test cases for:
    - Dialog displays two option buttons: "I own Echo on Meta" and "I'm a new player"
    - Selecting "I own Echo on Meta" sets `WizardState.userType = OWNER`
    - Selecting "I'm a new player" sets `WizardState.userType = NEW_PLAYER`
    - Dialog returns the WizardState with correct user type after selection
    - Dialog has a title/header text asking the question
    - Dialog is modal (blocks parent frame)
  - **GREEN**: Create `UserTypeDialog.java` extending `JDialog` with:
    - Constructor takes `FrameMain` parent and returns `WizardState`
    - Two `SpecialButton` instances with appropriate labels
    - Click handler that sets WizardState and disposes dialog
    - Matches existing UI patterns (Background, SpecialButton, TipBox)
    - Uses `conthrax-sb.otf` font for consistent look
  - Run `gradle test` → all UserTypeDialog tests pass

  **Must NOT do**:
  - Do NOT open FramePCDownload directly from this dialog (wiring is Task 16)
  - Do NOT modify FrameMain's existing layout — this is a new popup
  - Do NOT use hardcoded strings — extract to constants or resource bundle

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple modal dialog with two buttons — follows existing UI patterns
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 7)
  - **Blocks**: Task 16
  - **Blocked By**: Task 5 (needs WizardState)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:16-35` — Pattern to follow: extends JDialog, takes FrameMain as constructor param, calls initComponents() + setVisible(true)
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:46-55` — Dialog initialization pattern: DISPOSE_ON_CLOSE, setResizable(false), setIconImage, setTitle, setModal, Background panel
  - `src/main/java/bl00dy_c0d3_/echovr_installer/SpecialButton.java` — Custom button class: `new SpecialButton(text, "button_up.png", "button_down.png", "button_highlighted.png", fontSize)`
  - `src/main/java/bl00dy_c0d3_/echovr_installer/Helpers.java:56-62` — `centerFrame()` utility for centering dialogs
  - `src/main/java/bl00dy_c0d3_/echovr_installer/Background.java` — Custom background panel class

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/UserTypeDialogTest.java` (≥4 test methods)
  - [ ] Source file created: `src/main/java/bl00dy_c0d3_/echovr_installer/UserTypeDialog.java`
  - [ ] `gradle test --tests "*UserTypeDialogTest"` → PASS

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: UserTypeDialog unit tests pass (TDD)
    Tool: Bash (gradle test)
    Preconditions: WizardState available, test infra working
    Steps:
      1. Run: gradle test --tests "*UserTypeDialogTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: OWNER selection, NEW_PLAYER selection, dialog title, modality
    Expected Result: All UserTypeDialog tests pass
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-6-usertype-tests.txt

  Scenario: Dialog compiles and integrates with existing UI components
    Tool: Bash (gradle build)
    Preconditions: All Wave 1 tasks complete
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: UserTypeDialog compiles using existing SpecialButton/Background/Helpers
    Failure Indicators: Compilation errors (missing imports)
    Evidence: .sisyphus/evidence/task-6-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-6-usertype-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-6-compile.txt` — compilation output

  **Commit**: YES
  - Message: `feat(wizard): add UserTypeDialog asking owner vs new player status`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/UserTypeDialog.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/UserTypeDialogTest.java`
  - Pre-commit: `gradle test --tests "*UserTypeDialogTest"`

- [x] 7. **RED → GREEN: Complete SelectorConfig with tests**

  **What to do**:
  - **RED**: Write `SelectorConfigTest.java` with test cases for:
    - Loading properties from `discord-selectors.properties` on classpath
    - `getServerInvite()` returns `https://discord.gg/KqjqdNUaHR`
    - `getChannelName()` returns configured channel name
    - `getReactionEmoji()` returns configured emoji
    - Missing properties file → all getters return fallback defaults (not null)
    - Missing individual key → returns fallback for that key only
  - **GREEN**: Complete `SelectorConfig.java` (skeleton from Task 3):
    - Load properties via `ClassLoader.getSystemResourceAsStream()`
    - Typed getter methods with `properties.getProperty(key, fallback)`
    - Document each property's purpose in Javadoc
  - Run `gradle test` → all SelectorConfig tests pass

  **Must NOT do**:
  - Do NOT hardcode selectors in Java — all values must come from properties file
  - Do NOT throw exceptions on missing properties — always return fallback

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Standard Properties loading with well-defined getters
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 6)
  - **Blocks**: Tasks 12, 15
  - **Blocked By**: Tasks 2 (module-info), 3 (properties file skeleton)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/SelectorConfig.java` — Skeleton created in Task 3. Complete with Properties loading.
  - `src/main/resources/discord-selectors.properties` — Properties file created in Task 3. Read keys from here.

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/SelectorConfigTest.java` (≥6 test methods)
  - [ ] `gradle test --tests "*SelectorConfigTest"` → PASS

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: SelectorConfig loads properties and returns correct values
    Tool: Bash (gradle test)
    Preconditions: Properties file exists from Task 3
    Steps:
      1. Run: gradle test --tests "*SelectorConfigTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: All 6+ SelectorConfig tests pass
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-7-selectorconfig-tests.txt

  Scenario: Fallback values returned when properties file is missing
    Tool: Bash (gradle test)
    Preconditions: Test uses a test-specific properties file or mock
    Steps:
      1. Run: gradle test --tests "*SelectorConfigTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Fallback behavior tests pass
    Failure Indicators: NullPointerException, test failure on missing file
    Evidence: .sisyphus/evidence/task-7-fallback-tests.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-7-selectorconfig-tests.txt` — full test output
  - [ ] `.sisyphus/evidence/task-7-fallback-tests.txt` — fallback test output

  **Commit**: YES
  - Message: `feat(config): implement SelectorConfig with properties loading and fallbacks`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/SelectorConfig.java` (complete), `src/test/java/bl00dy_c0d3_/echovr_installer/SelectorConfigTest.java`
  - Pre-commit: `gradle test --tests "*SelectorConfigTest"`

- [x] 8. **RED → GREEN: Simplify FrameMain — hide buttons, add wizard entry**

  **What to do**:
  - **RED**: Write `FrameMainTest.java` with test cases for:
    - Wizard entry button is visible and labeled correctly (e.g., "Install Echo VR")
    - Non-wizard PC buttons (No licence patch, Steam Patch) are hidden (setVisible(false))
    - "Update Echo (PC)" button remains visible
    - Quest-side buttons (Quest Install Echo, Quest No licence patch) are hidden
    - "Delete cache" and "Get Quest Logs" buttons are hidden
    - Clicking wizard entry button triggers flow (mock/verify interaction)
  - **GREEN**: Modify `FrameMain.java`:
    - Hide `btn_PCnonLicence`, `btn_PCnoOVRHeadset`, `btn_QuestInstallEcho`, `btn_QuestNoLicence`, `btn_deleteCache`, `btn_addGetLog`
    - Keep `btn_PCUpdateEcho` visible and functional
    - Replace `btn_PCInstallEcho` (currently "Step 1: Install Echo") with wizard entry button that opens `UserTypeDialog`
    - Wizard entry flow: new `UserTypeDialog(this)` → get `WizardState` → if OK, proceed to `FramePCDownload`
    - Remove or comment out the panel backgrounds (`rahmen1`, `rahmen2`) since they contain hidden buttons
    - Add `addWizardEntry()` method to keep code clean
  - Run `gradle test` → all FrameMain tests pass

  **Must NOT do**:
  - Do NOT remove any button code — use `setVisible(false)` to hide (can be re-enabled later for Quest restructure)
  - Do NOT modify FramePCEchoUpdate behavior
  - Do NOT change frame dimensions or background

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Well-scoped UI modification — hiding buttons, adding one new button with known wiring pattern
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 9, 10)
  - **Blocks**: Tasks 9, 16
  - **Blocked By**: Task 5 (WizardState), Task 6 (UserTypeDialog)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java:113-128` — Current `addPCButtons()` method with `btn_PCInstallEcho` — modify this to wire to wizard
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java:169-241` — `addBackgroundFrames()` with `rahmen1` and `rahmen2` — hide these panels
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java:150-167` — `addQuestButtons()` — hide quest buttons
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java:132-148` — `btn_PCUpdateEcho` — keep this button visible
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:31` — Constructor signature: `FramePCDownload(FrameMain frameMain)` — needs to accept WizardState too

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/FrameMainTest.java` (≥5 test methods)
  - [ ] `gradle test --tests "*FrameMainTest"` → PASS
  - [ ] `gradle build` → BUILD SUCCESSFUL

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: FrameMain unit tests pass verifying button visibility
    Tool: Bash (gradle test)
    Preconditions: Wave 2 complete
    Steps:
      1. Run: gradle test --tests "*FrameMainTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: All FrameMain visibility tests pass
    Failure Indicators: Test failures on button state assertions
    Evidence: .sisyphus/evidence/task-8-framemain-tests.txt

  Scenario: FrameMain compiles and renders (visual verification)
    Tool: Bash (gradle build)
    Preconditions: All source files created
    Steps:
      1. Run: gradle build --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert no compilation errors mentioning FrameMain
    Expected Result: FrameMain compiles with hidden buttons
    Failure Indicators: Compilation errors
    Evidence: .sisyphus/evidence/task-8-framemain-build.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-8-framemain-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-8-framemain-build.txt` — build output

  **Commit**: YES
  - Message: `feat(wizard): simplify FrameMain to show only wizard entry and Update Echo`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/FrameMainTest.java`
  - Pre-commit: `gradle test --tests "*FrameMainTest"`

- [x] 9. **RED → GREEN: Modify FramePCDownload — add Next button and WizardState integration**

  **What to do**:
  - **RED**: Write `FramePCDownloadTest.java` with test cases for:
    - Constructor accepts `WizardState` parameter
    - "Next" button is visible after download completes
    - "Next" button is hidden/disabled before download completes
    - "Next" button stores the selected path into WizardState before proceeding
    - If WizardState.userType is OWNER: "Next" opens optional patches panel (Task 10)
    - If WizardState.userType is NEW_PLAYER: "Next" opens FramePCPatcher with shared path
    - Download path label updates WizardState.installPath in real-time
  - **GREEN**: Modify `FramePCDownload.java`:
    - Add `WizardState wizardState` field, accept in constructor alongside `FrameMain`
    - Add "Next" button (initially hidden, shown after download completes)
    - Wire "Next" button: call `wizardState.setInstallPath(labelPcDownloadPath.getText())` then branch based on userType
    - When userType is OWNER: show optional patches dialog (delegates to Task 10 component)
    - When userType is NEW_PLAYER: dispose self, open `new FramePCPatcher(wizardState)`
    - Keep all existing download logic untouched
  - Run `gradle test` → all FramePCDownload tests pass

  **Must NOT do**:
  - Do NOT change the download logic, URL, or zip extraction
  - Do NOT break existing download progress tracking
  - Do NOT remove the "Start Download" button or path chooser

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Modifies existing complex dialog — must preserve download behavior while adding state management and conditional branching
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 8, 10)
  - **Blocks**: Tasks 10, 13, 16
  - **Blocked By**: Tasks 5 (WizardState), 8 (FrameMain wiring)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:1-256` — Full current implementation. Key areas: constructor (line 31-35), download button (line 133-169), path label (line 63-68), downloader callback (line 146-162)
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:133-169` — `pcStartDownload` button and its `mouseReleased` handler — add "Next" button similar pattern after this
  - `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java` — Created in Task 5, used to pass userType and installPath
  - `src/main/java/bl00dy_c0d3_/echovr_installer/SpecialButton.java` — Button class for "Next" button

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/FramePCDownloadTest.java` (≥6 test methods)
  - [ ] `gradle test --tests "*FramePCDownloadTest"` → PASS
  - [ ] `gradle build` → BUILD SUCCESSFUL

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: FramePCDownload unit tests verify Next button behavior
    Tool: Bash (gradle test)
    Preconditions: WizardState available, test infra working
    Steps:
      1. Run: gradle test --tests "*FramePCDownloadTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: Next visibility, path storage, OWNER vs NEW_PLAYER branching
    Expected Result: All FramePCDownload tests pass
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-9-pcdownload-tests.txt

  Scenario: FramePCDownload compiles with WizardState integration
    Tool: Bash (gradle build)
    Preconditions: WizardState.java exists
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Compilation succeeds with new constructor signature
    Failure Indicators: Compilation errors, method signature mismatches
    Evidence: .sisyphus/evidence/task-9-pcdownload-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-9-pcdownload-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-9-pcdownload-compile.txt` — compilation output

  **Commit**: YES
  - Message: `feat(wizard): add Next button and WizardState integration to FramePCDownload`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/FramePCDownloadTest.java`
  - Pre-commit: `gradle test --tests "*FramePCDownloadTest"`

- [x] 10. **RED → GREEN: Build optional patches post-install panel for Echo owners**

  **What to do**:
  - **RED**: Write `OptionalPatchesPanelTest.java` with test cases for:
    - Panel displays two options: "No Licence Patch" and "Steam Patch (Revive)"
    - "No Licence Patch" button opens `FramePCPatcher` with install path from WizardState
    - "Steam Patch (Revive)" button opens `FrameSteamPatcher`
    - Panel has a "Skip / I'm done" button that closes the dialog
    - Panel receives WizardState to pass path to patcher
  - **GREEN**: Create `OptionalPatchesPanel.java` extending `JDialog`:
    - Two `SpecialButton` instances for the patch options
    - One "Skip" button to close
    - Matches existing UI patterns (Background, SpecialButton, TipBox)
    - Opens correct frame on button click, passing WizardState for path continuity
    - Uses `conthrax-sb.otf` font
  - Run `gradle test` → all OptionalPatchesPanel tests pass

  **Must NOT do**:
  - Do NOT modify FramePCPatcher or FrameSteamPatcher internals — this panel just opens them
  - Do NOT implement the patch logic — just open existing frames

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: New UI component with multiple buttons and frame-launching logic — moderate complexity
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 8, 9)
  - **Blocks**: Task 16
  - **Blocked By**: Task 9 (called from FramePCDownload "Next" button)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java` — Existing constructor: `new FramePCPatcher()` — will need new constructor `FramePCPatcher(WizardState wizardState)` in Task 13
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameSteamPatcher.java:32` — Existing constructor: `FrameSteamPatcher(FrameMain frameMain)` — open as-is
  - `src/main/java/bl00dy_c0d3_/echovr_installer/UserTypeDialog.java` — Follow same JDialog pattern created in Task 6

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/OptionalPatchesPanelTest.java` (≥5 test methods)
  - [ ] `gradle test --tests "*OptionalPatchesPanelTest"` → PASS
  - [ ] `gradle build` → BUILD SUCCESSFUL

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: OptionalPatchesPanel unit tests pass
    Tool: Bash (gradle test)
    Preconditions: WizardState available, test infra working
    Steps:
      1. Run: gradle test --tests "*OptionalPatchesPanelTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: All tests pass including button presence and click behaviors
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-10-optionalpatches-tests.txt

  Scenario: Panel compiles and integrates with existing frames
    Tool: Bash (gradle build)
    Preconditions: FramePCPatcher and FrameSteamPatcher exist
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Compilation succeeds
    Failure Indicators: Compilation errors
    Evidence: .sisyphus/evidence/task-10-optionalpatches-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-10-optionalpatches-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-10-optionalpatches-compile.txt` — compilation output

  **Commit**: YES
  - Message: `feat(wizard): add optional patches panel for Echo owners post-install`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/OptionalPatchesPanel.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/OptionalPatchesPanelTest.java`
  - Pre-commit: `gradle test --tests "*OptionalPatchesPanelTest"`

- [x] 11. **RED → GREEN: Create DiscordWebView — JavaFX Stage with WebView embed**

  **What to do**:
  - **RED**: Write `DiscordWebViewTest.java` with test cases for:
    - `DiscordWebView` creates a JavaFX Stage (not Swing JFrame)
    - WebView loads a URL via `webEngine.load(url)`
    - `Platform.setImplicitExit(false)` is called to prevent JavaFX from shutting down when Stage closes
    - `navigateTo(url)` method changes the WebView URL
    - `getWebEngine()` returns the WebEngine for JavaScript injection
    - `close()` disposes the Stage without calling `Platform.exit()`
  - **GREEN**: Create `DiscordWebView.java`:
    - Extends `javafx.application.Application` or creates Stage manually
    - Initialize JavaFX runtime: `new JFXPanel()` (forces JavaFX toolkit init within Swing app)
    - Create `Stage` with `WebView` and `WebEngine`
    - `navigateTo(String url)` method
    - `getWebEngine()` for JavaScript bridge
    - `close()` to hide/dispose Stage
    - Handle `Platform.setImplicitExit(false)` for Swing integration
    - Set user agent if needed for Discord compatibility
  - Run `gradle test` → all DiscordWebView tests pass

  **Must NOT do**:
  - Do NOT call `Platform.exit()` in close() — must keep JavaFX alive for Swing app
  - Do NOT extend `javafx.application.Application` — use manual Stage creation via `JFXPanel` init
  - Do NOT block the EDT when initializing JavaFX

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Swing-JavaFX interop requires careful lifecycle management — `JFXPanel` initialization, `Platform.setImplicitExit(false)`, thread discipline
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (sequential with Tasks 12-15, but Task 12 can run parallel to 11 since it uses WebView interface)
  - **Blocks**: Tasks 13, 14
  - **Blocked By**: Task 1 (JavaFX deps), Task 2 (module-info), Task 9 (FramePCDownload new constructor — needed for path)

  **References** (CRITICAL):
  - `build.gradle:47-48` — JavaFX dependencies (uncommented in Task 1): `javafx-controls`, `javafx-web`, `javafx-fxml`
  - `src/main/java/module-info.java` — Updated in Task 2 with `requires javafx.web`, `requires javafx.graphics`, etc.
  - Official JavaFX docs: `JFXPanel` is the bridge — call `new JFXPanel()` on EDT to init JavaFX runtime in Swing apps
  - Official JavaFX docs: `Platform.setImplicitExit(false)` prevents JavaFX from exiting when last Stage closes
  - Official JavaFX docs: `WebView` and `WebEngine` provide embedded Chromium browser in JavaFX
  - Metis warning: `ViewPainter.ROOT_PATHS` can cause memory leaks if Stages aren't properly cleaned up

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/DiscordWebViewTest.java` (≥5 test methods)
  - [ ] Source file created: `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java`
  - [ ] `gradle test --tests "*DiscordWebViewTest"` → PASS

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: DiscordWebView unit tests pass
    Tool: Bash (gradle test)
    Preconditions: JavaFX dependencies resolved (Wave 1)
    Steps:
      1. Run: gradle test --tests "*DiscordWebViewTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: All DiscordWebView tests pass
    Failure Indicators: Test failures, JavaFX init errors
    Evidence: .sisyphus/evidence/task-11-discordwebview-tests.txt

  Scenario: DiscordWebView compiles with JavaFX modules
    Tool: Bash (gradle compileJava)
    Preconditions: module-info.java updated with JavaFX requires
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert no "module not found: javafx.web" errors
    Expected Result: Compilation succeeds with JavaFX imports
    Failure Indicators: Module resolution errors
    Evidence: .sisyphus/evidence/task-11-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-11-discordwebview-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-11-compile.txt` — compilation output

  **Commit**: YES
  - Message: `feat(discord): create JavaFX WebView wrapper for embedded Discord browser`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/DiscordWebViewTest.java`
  - Pre-commit: `gradle test --tests "*DiscordWebViewTest"`

- [x] 12. **RED → GREEN: Create DiscordNavigator — auto-navigate, reaction finder, thread detection**

  **What to do**:
  - **RED**: Write `DiscordNavigatorTest.java` with test cases for:
    - `navigateToServer(String inviteUrl)` loads Discord server in WebView and waits for load
    - `findChannel(String channelName)` uses CSS selector from SelectorConfig to locate channel element
    - `findReactionMessage()` locates the message with the target reaction using configured selector
    - `detectPrivateThread()` polls for appearance of private thread element using configured selector
    - `extractPatchUrl()` extracts the CDN URL from the private thread message
    - Timeout behavior: methods return Optional.empty() or throw TimeoutException if element not found within timeout
    - Manual fallback method: `waitForManualPaste(timeout)` — returns empty if user doesn't paste
  - **GREEN**: Create `DiscordNavigator.java`:
    - Constructor takes `DiscordWebView` and `SelectorConfig`
    - `navigateToServer()`: load invite URL, wait for page load complete
    - `findChannel()`: execute JavaScript to find and click channel by name/text
    - `findReactionMessage()`: scroll to and highlight message with reaction button
    - `highlightReactionButton()`: add visual highlight to reaction button (border, glow) — user clicks manually
    - `detectPrivateThread()`: poll DOM every 2 seconds for new thread element, return URL when found
    - `extractPatchUrl()`: regex match for `https://cdn.discordapp.com/attachments/...` or similar CDN pattern
    - `waitForManualPaste(timeoutSeconds)`: fallback that shows a "Paste from clipboard" prompt
    - All DOM operations via `webEngine.executeScript()`
    - Timeout handling (configurable, default 30s per operation)
    - Thread safety: all WebView operations on FX Application Thread via `Platform.runLater()`
  - Run `gradle test` → all DiscordNavigator tests pass

  **Must NOT do**:
  - Do NOT click the reaction button automatically — only highlight it. User performs the click.
  - Do NOT hardcode Discord CSS selectors — use SelectorConfig exclusively
  - Do NOT run WebView operations on EDT — use `Platform.runLater()` for FX thread

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Complex WebView DOM manipulation with JavaScript injection, polling, timeouts — requires careful async handling and thread discipline
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (can run parallel to Task 11 — uses DiscordWebView interface)
  - **Blocks**: Task 13
  - **Blocked By**: Task 7 (SelectorConfig with loaded selectors)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java` — Created in Task 11, provides `getWebEngine()` for JavaScript execution
  - `src/main/java/bl00dy_c0d3_/echovr_installer/SelectorConfig.java` — Completed in Task 7, provides DOM selectors
  - `src/main/resources/discord-selectors.properties` — Created in Task 3, contains selectors like `discord.channel.name`, `discord.message.selector`, `discord.thread.selector`
  - Metis warning: Discord DOM changes frequently — selectors in properties file MUST be the single source of truth
  - Official Discord invite URL: `https://discord.gg/KqjqdNUaHR`

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/DiscordNavigatorTest.java` (≥8 test methods)
  - [ ] Source file created: `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordNavigator.java`
  - [ ] `gradle test --tests "*DiscordNavigatorTest"` → PASS

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: DiscordNavigator unit tests pass (all DOM operations)
    Tool: Bash (gradle test)
    Preconditions: DiscordWebView and SelectorConfig available
    Steps:
      1. Run: gradle test --tests "*DiscordNavigatorTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: navigate, findChannel, findReaction, detectThread, extractUrl, timeout, fallback
    Expected Result: All 8+ DiscordNavigator tests pass
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-12-navigator-tests.txt

  Scenario: DiscordNavigator compiles with correct thread discipline
    Tool: Bash (gradle compileJava)
    Preconditions: JavaFX modules available
    Steps:
      1. Run: gradle compileJava --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Compilation succeeds using Platform.runLater patterns
    Failure Indicators: Compilation errors
    Evidence: .sisyphus/evidence/task-12-navigator-compile.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-12-navigator-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-12-navigator-compile.txt` — compilation output

  **Commit**: YES
  - Message: `feat(discord): add DiscordNavigator for semi-auto channel/reaction/thread handling`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordNavigator.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/DiscordNavigatorTest.java`
  - Pre-commit: `gradle test --tests "*DiscordNavigatorTest"`

- [x] 13. **RED → GREEN: Redesign FramePCPatcher with Discord WebView integration**

  **What to do**:
  - **RED**: Write `FramePCPatcherTest.java` (new tests, not modifying existing if any) with test cases for:
    - Constructor accepts `WizardState` parameter and uses `wizardState.getInstallPath()` as default path
    - Patcher window contains an embedded Discord instruction area (replacing external browser steps 1-3)
    - "Open Discord" button opens JavaFX DiscordWebView popup via DiscordNavigator
    - Step 4 (paste link field) is pre-filled if DiscordNavigator successfully extracts URL
    - Step 5 (path chooser) defaults to WizardState install path
    - Step 6 (Start Patching) works with either auto-extracted or manually-pasted URL
    - Manual fallback: if Discord extraction fails, user can still paste URL manually
  - **GREEN**: Redesign `FramePCPatcher.java`:
    - Add `WizardState wizardState` field, new constructor `FramePCPatcher(WizardState wizardState)`
    - Keep existing constructor `FramePCPatcher()` for backward compatibility (optional patches panel)
    - Replace Steps 1-3 (external Discord instructions) with:
      - Step 1: "Open Discord in embedded browser" button → opens DiscordWebView
      - Step 2: "The installer will guide you — click the highlighted reaction" with progress indicator
      - Step 3: Auto-extract tries to fill URL field; shows status (success/waiting)
    - Keep Steps 4-6 with enhancements:
      - Paste field auto-populated on success, shows "URL detected!" indicator
      - Path defaults from WizardState
      - "Start Patching" button functions same as current
    - Add manual fallback toggle: "Discord didn't work? Paste link manually" → shows current Step 4 text field
    - Add progress/status indicator for Discord operations ("Navigating...", "Looking for reaction...", "Waiting for thread...")
    - Preserve existing background image, fonts, and section panel styling
  - Run `gradle test` → all FramePCPatcher tests pass

  **Must NOT do**:
  - Do NOT remove the existing patching logic (download, path validation, error handling)
  - Do NOT break the existing 6-step section panel layout — enhance it
  - Do NOT remove the existing constructor — add new one, keep old for backward compat

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Major redesign of existing complex dialog — must integrate 3 new components (DiscordWebView, DiscordNavigator, WizardState) while preserving patching logic
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (sequential — depends on Tasks 11 and 12)
  - **Blocks**: Tasks 15, 16
  - **Blocked By**: Tasks 9 (FramePCDownload path), 11 (DiscordWebView), 12 (DiscordNavigator)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:1-374` — Full current implementation. Key areas: constructor (line 30-33), step headers (lines 53-62), hyperlink (line 57), reaction image (lines 62-78), copy link image (lines 81-99), text field (lines 103-105), path chooser (lines 108-157), start patch button (lines 170-212)
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:30-33` — Current constructor signature: `public FramePCPatcher()` — add new: `public FramePCPatcher(WizardState wizardState)`
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:103-105` — `textfieldPCPatchLink` — this field should be auto-populated by DiscordNavigator on success
  - `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java` — Created in Task 11, used to open Discord popup
  - `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordNavigator.java` — Created in Task 12, provides auto-navigation and URL extraction
  - `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java` — Created in Task 5, provides installPath for default path

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/FramePCPatcherTest.java` (≥7 test methods)
  - [ ] `gradle test --tests "*FramePCPatcherTest"` → PASS
  - [ ] `gradle build` → BUILD SUCCESSFUL

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: FramePCPatcher unit tests pass with Discord integration
    Tool: Bash (gradle test)
    Preconditions: DiscordWebView and DiscordNavigator available
    Steps:
      1. Run: gradle test --tests "*FramePCPatcherTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: WizardState path default, Discord button, auto-fill, manual fallback
    Expected Result: All FramePCPatcher tests pass
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-13-patcher-tests.txt

  Scenario: FramePCPatcher compiles with all new dependencies
    Tool: Bash (gradle build)
    Preconditions: All Wave 1-4 source files exist
    Steps:
      1. Run: gradle build --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Full build succeeds with redesigned patcher
    Failure Indicators: Compilation errors, missing imports
    Evidence: .sisyphus/evidence/task-13-patcher-build.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-13-patcher-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-13-patcher-build.txt` — build output

  **Commit**: YES
  - Message: `feat(discord): redesign FramePCPatcher with embedded Discord WebView and auto-extract`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/FramePCPatcherTest.java`
  - Pre-commit: `gradle test --tests "*FramePCPatcherTest"`

- [x] 14. **Implement JFXPanel lifecycle management and EDT↔FX thread safety**

  **What to do**:
  - Ensure `Platform.setImplicitExit(false)` is called before any JavaFX Stage creation
  - Implement `Platform.exit()` call on application shutdown (add shutdown hook)
  - Verify all WebView operations use `Platform.runLater()` for FX Application Thread
  - Verify all Swing UI updates use `SwingUtilities.invokeLater()` for EDT
  - Add `ViewPainter.ROOT_PATHS` cleanup to prevent memory leaks when Stages are closed
  - Add a `Runtime.getRuntime().addShutdownHook()` in `EchoVRInstaller.main()` to call `Platform.exit()`
  - Test thread correctness: verify no "Not on FX application thread" exceptions
  - Test memory: verify no Stage references retained after close

  **Must NOT do**:
  - Do NOT call `Platform.exit()` when closing a single Stage — only on app shutdown
  - Do NOT create JavaFX stages from background threads — always from FX thread

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Thread safety and lifecycle management — requires careful verification of concurrency patterns
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 4 (must run after Task 11 to verify its lifecycle)
  - **Blocks**: Task 16
  - **Blocked By**: Task 11 (DiscordWebView created)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java` — Created in Task 11, verify Platform.setImplicitExit and lifecycle
  - `src/main/java/bl00dy_c0d3_/echovr_installer/EchoVRInstaller.java:14-53` — Main entry point, add shutdown hook here
  - Metis warning: `Platform.setImplicitExit(false)` is MANDATORY for Swing apps using JavaFX; `ViewPainter.ROOT_PATHS` leak if Stages not properly disposed

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Verify `gradle test --tests "*DiscordWebViewTest"` still passes after lifecycle changes
  - [ ] Manual verification: app closes cleanly without JavaFX processes lingering

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: All tests pass after lifecycle management implementation
    Tool: Bash (gradle test)
    Preconditions: DiscordWebView tests exist
    Steps:
      1. Run: gradle test --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert no "Not on FX application thread" in output
    Expected Result: All tests pass with thread-safe lifecycle
    Failure Indicators: Thread exceptions, test failures after lifecycle changes
    Evidence: .sisyphus/evidence/task-14-lifecycle-tests.txt

  Scenario: Shutdown hook registered in EchoVRInstaller
    Tool: Bash (grep)
    Preconditions: Lifecycle management implemented
    Steps:
      1. Run: grep -n "Platform.exit\|addShutdownHook\|setImplicitExit" src/main/java/bl00dy_c0d3_/echovr_installer/EchoVRInstaller.java
      2. Assert output contains "Platform.exit" or "addShutdownHook"
      3. Run: grep -n "setImplicitExit" src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java
      4. Assert output contains "Platform.setImplicitExit(false)"
    Expected Result: Platform lifecycle calls present in correct locations
    Failure Indicators: Missing Platform.exit or setImplicitExit(false)
    Evidence: .sisyphus/evidence/task-14-lifecycle-grep.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-14-lifecycle-tests.txt` — full test output
  - [ ] `.sisyphus/evidence/task-14-lifecycle-grep.txt` — grep results

  **Commit**: YES (can group with Task 13 commit)
  - Message: `fix(javafx): implement proper JFXPanel lifecycle and EDT/FX thread safety`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/EchoVRInstaller.java`, `src/main/java/bl00dy_c0d3_/echovr_installer/DiscordWebView.java`
  - Pre-commit: `gradle test`

- [x] 15. **Wire manual fallback: clipboard paste as safety net in FramePCPatcher**

  **What to do**:
  - Add a toggle/link in FramePCPatcher: "Discord didn't work? Click here to paste the URL manually"
  - When toggled: show the existing `textfieldPCPatchLink` text field (from original implementation)
  - Add "Paste from Clipboard" button that reads system clipboard using `Toolkit.getDefaultToolkit().getSystemClipboard()`
  - Validate pasted URL matches Discord CDN pattern (`cdn.discordapp.com/attachments/...`)
  - Show validation feedback: green checkmark if valid, red X if invalid
  - Ensure fallback path is always accessible (not hidden behind automation failure)
  - Test cases: invalid URL rejected, valid URL accepted, clipboard paste works
  - Ensure Manual fallback is accessible at ALL times, even when auto-extraction is working (don't hide behind failure)

  **Must NOT do**:
  - Do NOT make manual fallback the only option — auto-extract is primary
  - Do NOT hide the manual fallback — it must be discoverable

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Adding a fallback UI path to existing FramePCPatcher — straightforward UI additions
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (can run parallel to Task 14 since it modifies FramePCPatcher UI, not lifecycle)
  - **Blocks**: Task 16
  - **Blocked By**: Task 3 (discord-selectors.properties — used for URL validation pattern), Task 13 (FramePCPatcher redesign with Discord steps)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:103-105` — `textfieldPCPatchLink` — existing text field for paste, reused as fallback
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:173-202` — `pcStartPatch` mouseReleased handler with URL validation — reuse validation logic
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:176` — URL validation regex: `link.matches("https://cdn.discordapp.com/attachments/.*/pnsovr.dll.*")`
  - Java AWT: `Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor)` — clipboard access

  **Acceptance Criteria**:
  **TDD**:
  - [ ] URL validation: correct CDN pattern accepted, wrong pattern rejected
  - [ ] Clipboard paste: retrieves text successfully
  - [ ] `gradle test --tests "*FramePCPatcherTest"` — all tests (including new fallback tests) pass

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Manual fallback visible and functional
    Tool: Bash (gradle test)
    Preconditions: FramePCPatcher with Discord WebView exists
    Steps:
      1. Run: gradle test --tests "*FramePCPatcherTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: fallback toggle visible, URL validation, clipboard paste
    Expected Result: Fallback tests pass
    Failure Indicators: Test failures on fallback functionality
    Evidence: .sisyphus/evidence/task-15-fallback-tests.txt

  Scenario: Invalid URL rejected by fallback validation
    Tool: Bash (gradle test)
    Preconditions: FramePCPatcherTest includes invalid URL test
    Steps:
      1. Run: gradle test --tests "*FramePCPatcherTest.*invalidUrl*" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Invalid URL test passes (rejected)
    Failure Indicators: Test failure — invalid URL accepted
    Evidence: .sisyphus/evidence/task-15-url-validation.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-15-fallback-tests.txt` — test output
  - [ ] `.sisyphus/evidence/task-15-url-validation.txt` — validation test output

  **Commit**: YES
  - Message: `feat(discord): add manual clipboard paste fallback to FramePCPatcher`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java`, updated `src/test/java/bl00dy_c0d3_/echovr_installer/FramePCPatcherTest.java`
  - Pre-commit: `gradle test --tests "*FramePCPatcherTest"`

- [x] 16. **Wire full wizard sequence end-to-end (UserTypeDialog → FramePCDownload → FramePCPatcher)**

  **What to do**:
  - **RED**: Write `WizardFlowIntegrationTest.java` with integration test cases:
    - Full OWNER flow: UserTypeDialog(OWNER) → FramePCDownload → "Next" → OptionalPatchesPanel
    - Full NEW_PLAYER flow: UserTypeDialog(NEW_PLAYER) → FramePCDownload → "Next" → FramePCPatcher(WizardState)
    - WizardState installPath correctly propagates from FramePCDownload to FramePCPatcher
    - FramePCPatcher default path matches what was set in FramePCDownload
    - FrameMain wizard entry button triggers UserTypeDialog → rest of flow
    - Flow handles cancellation at any step (user closes dialog) — no crash, no state leak
  - **GREEN**: Wire the complete sequence in FrameMain and FramePCDownload:
    - FrameMain wizard entry → `UserTypeDialog(this)` → if result OK → `new FramePCDownload(this, wizardState)`
    - FramePCDownload "Next" button: check `wizardState.userType`
      - OWNER → `new OptionalPatchesPanel(this, wizardState)`
      - NEW_PLAYER → dispose → `new FramePCPatcher(wizardState)`
    - Ensure all dialogs are modal (blocking) so flow is sequential
    - Add error handling: if any step fails, show error dialog, don't proceed
  - Run `gradle test` → all integration tests pass

  **Must NOT do**:
  - Do NOT change the behavior of individual dialogs — only the wiring between them
  - Do NOT make the flow non-modal — must be sequential

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: Cross-component integration with state propagation — requires understanding of all wizard components
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 5 (must run after all component tasks)
  - **Blocks**: Task 18
  - **Blocked By**: Tasks 5, 6, 8, 9, 13, 15

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java` — Wizard entry point wiring
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java` — "Next" button branching logic
  - `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java` — Shared state object with userType and installPath
  - `src/main/java/bl00dy_c0d3_/echovr_installer/UserTypeDialog.java` — Returns WizardState with userType set

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/WizardFlowIntegrationTest.java` (≥5 test methods)
  - [ ] `gradle test --tests "*WizardFlowIntegrationTest"` → PASS
  - [ ] `gradle build` → BUILD SUCCESSFUL

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Integration tests pass for both OWNER and NEW_PLAYER paths
    Tool: Bash (gradle test)
    Preconditions: All Wave 1-4 tasks complete
    Steps:
      1. Run: gradle test --tests "*WizardFlowIntegrationTest" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert tests cover: OWNER flow, NEW_PLAYER flow, path propagation
    Expected Result: All integration tests pass
    Failure Indicators: Any test failure, state propagation error
    Evidence: .sisyphus/evidence/task-16-integration-tests.txt

  Scenario: Full build succeeds with all wired components
    Tool: Bash (gradle build)
    Preconditions: All source files complete
    Steps:
      1. Run: gradle build --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert no "cannot find symbol" or "unresolved reference" errors
    Expected Result: Complete build passes
    Failure Indicators: Compilation errors in wiring code
    Evidence: .sisyphus/evidence/task-16-full-build.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-16-integration-tests.txt` — integration test output
  - [ ] `.sisyphus/evidence/task-16-full-build.txt` — full build output

  **Commit**: YES
  - Message: `feat(wizard): wire full wizard sequence with state propagation`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java` (wiring), `src/test/java/bl00dy_c0d3_/echovr_installer/WizardFlowIntegrationTest.java`
  - Pre-commit: `gradle test`

- [x] 17. **Path-sharing integration between FramePCDownload and FramePCPatcher**

  **What to do**:
  - Verify that when `wizardState.setInstallPath()` is called in FramePCDownload, the value is available to FramePCPatcher
  - Ensure path normalization is consistent: FramePCDownload uses `C:/EchoVR/ready-at-dawn-echo-arena`, FramePCPatcher expects `C:/EchoVR/ready-at-dawn-echo-arena/bin/win10`
  - Add `getBinPath()` helper to WizardState that appends `/bin/win10` to install path
  - FramePCPatcher's `labelPcPatchDownloadPath` should default to `wizardState.getBinPath()` when constructed with WizardState
  - Test: path from FramePCDownload's label → WizardState → FramePCPatcher's label matches
  - Add a method `WizardState.getBinPath()` that returns `installPath + "/ready-at-dawn-echo-arena/bin/win10"` (or appropriate path)
  - Test cases: verify path chain, verify bin path computation, verify default fallback if path not set

  **Must NOT do**:
  - Do NOT change the download target path — only ensure it's passed correctly to patcher

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Data flow verification and a helper method addition — well-scoped
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 5 (runs alongside Task 16)
  - **Blocks**: Task 18
  - **Blocked By**: Tasks 5 (WizardState), 9 (FramePCDownload path setting)

  **References** (CRITICAL):
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCDownload.java:26` — Default path: `C:/EchoVR`
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:23` — Default path: `C:/EchoVR/ready-at-dawn-echo-arena`
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FramePCPatcher.java:177` — Expected path usage: `echoPath + "/bin/win10"`
  - `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java` — Created in Task 5, add `getBinPath()` method here

  **Acceptance Criteria**:
  **TDD**:
  - [ ] `WizardState.getBinPath()` returns `installPath + "/ready-at-dawn-echo-arena/bin/win10"`
  - [ ] Test verifies path chain: FramePCDownload path → WizardState → FramePCPatcher label
  - [ ] `gradle test --tests "*WizardStateTest"` — updated tests pass

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Path propagates correctly through wizard flow
    Tool: Bash (gradle test)
    Preconditions: WizardState with getBinPath() implemented
    Steps:
      1. Run: gradle test --tests "*WizardFlowIntegrationTest*path*" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Path propagation tests pass
    Failure Indicators: Path mismatch between components
    Evidence: .sisyphus/evidence/task-17-path-sharing.txt

  Scenario: getBinPath() computes correct path
    Tool: Bash (gradle test)
    Preconditions: WizardState has getBinPath()
    Steps:
      1. Run: gradle test --tests "*WizardStateTest*binPath*" --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: getBinPath() returns installPath/bin/win10 correctly
    Failure Indicators: Test failure on path computation
    Evidence: .sisyphus/evidence/task-17-binpath.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-17-path-sharing.txt` — path propagation test output
  - [ ] `.sisyphus/evidence/task-17-binpath.txt` — bin path test output

  **Commit**: YES (can group with Task 18)
  - Message: `feat(wizard): implement path sharing and bin path computation in WizardState`
  - Files: `src/main/java/bl00dy_c0d3_/echovr_installer/WizardState.java`, `src/test/java/bl00dy_c0d3_/echovr_installer/WizardStateTest.java` (updated)
  - Pre-commit: `gradle test --tests "*WizardStateTest"`

- [x] 18. **Final integration tests — end-to-end TDD for complete wizard flows**

  **What to do**:
  - **RED**: Write comprehensive end-to-end tests in `WizardE2ETest.java`:
    - Test 1: OWNER flow end-to-end — UserTypeDialog → FramePCDownload → (mock download) → "Next" → OptionalPatchesPanel → "No Licence Patch" → FramePCPatcher opens with correct path
    - Test 2: NEW_PLAYER flow end-to-end — UserTypeDialog → FramePCDownload → (mock download) → "Next" → FramePCPatcher opens with Discord WebView integration and correct path
    - Test 3: Cancellation at UserTypeDialog — no frames opened, no state leaks
    - Test 4: Cancellation at FramePCDownload — FramePCDownload disposed, no patcher opened
    - Test 5: FramePCPatcher manual fallback works when auto-extract fails
    - Test 6: Download path reflects in WizardState after user changes path in FramePCDownload
    - Test 7: Update Echo button still works independently of wizard
  - **GREEN**: Fix any issues found, ensure all E2E tests pass
  - Run `gradle test` → ALL tests pass (entire test suite)

  **Must NOT do**:
  - Do NOT mock the actual Discord WebView in E2E tests — use the real components where possible, mock only external network
  - Do NOT skip any test case even if implementation is challenging

  **Recommended Agent Profile**:
  - **Category**: `deep`
    - Reason: End-to-end integration testing requires understanding of all components and their interactions
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 5 (final task — must run after all others)
  - **Blocks**: FINAL verification
  - **Blocked By**: Tasks 16, 17

  **References** (CRITICAL):
  - `src/test/java/bl00dy_c0d3_/echovr_installer/WizardFlowIntegrationTest.java` — Created in Task 16, extend with E2E scenarios
  - All component test files from previous tasks — ensure no regressions
  - `src/main/java/bl00dy_c0d3_/echovr_installer/FrameMain.java` — Verify Update Echo button still works

  **Acceptance Criteria**:
  **TDD**:
  - [ ] Test file created: `src/test/java/bl00dy_c0d3_/echovr_installer/WizardE2ETest.java` (≥7 test methods)
  - [ ] `gradle test` → ALL tests pass (full suite, 0 failures)

  **QA Scenarios (MANDATORY)**:

  ```
  Scenario: Full test suite passes including all E2E scenarios
    Tool: Bash (gradle test)
    Preconditions: ALL implementation tasks complete
    Steps:
      1. Run: gradle clean test --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
      3. Assert test count matches expected total (SmokeTest + WizardStateTest + UserTypeDialogTest + SelectorConfigTest + FrameMainTest + FramePCDownloadTest + OptionalPatchesPanelTest + DiscordWebViewTest + DiscordNavigatorTest + FramePCPatcherTest + WizardFlowIntegrationTest + WizardE2ETest)
      4. Assert no test failures
    Expected Result: Complete test suite passes with all TDD tests green
    Failure Indicators: Any test failure or unexpected skip
    Evidence: .sisyphus/evidence/task-18-full-suite.txt

  Scenario: Build succeeds with all tests passing
    Tool: Bash (gradle build)
    Preconditions: Full test suite passes
    Steps:
      1. Run: gradle build --no-daemon 2>&1
      2. Assert stdout contains "BUILD SUCCESSFUL"
    Expected Result: Full build including tests passes
    Failure Indicators: Build failure or test failures
    Evidence: .sisyphus/evidence/task-18-full-build.txt
  ```

  **Evidence to Capture**:
  - [ ] `.sisyphus/evidence/task-18-full-suite.txt` — complete test suite output
  - [ ] `.sisyphus/evidence/task-18-full-build.txt` — full build output

  **Commit**: YES (final implementation commit)
  - Message: `test(wizard): add end-to-end integration tests for complete wizard flows`
  - Files: `src/test/java/bl00dy_c0d3_/echovr_installer/WizardE2ETest.java`
  - Pre-commit: `gradle test`

---

## Final Verification Wave (MANDATORY — after ALL implementation tasks)

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in `.sisyphus/evidence/`. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `gradle build` + `gradle test`. Review all changed files for: raw type casts, empty catches, `System.out.println` in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp). Verify JavaFX lifecycle (Platform.exit called, no memory leaks).
  Output: `Build [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill)
  Start from clean state. Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration (wizard flow end-to-end). Test edge cases: empty state, invalid input, rapid actions. Verify Discord WebView opens and is interactive. Save to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination: Task N touching Task M's files. Flag unaccounted changes. Verify Quest-side files untouched.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

- **Wave 1**: `build(javafx): uncomment JavaFX dependencies and update module system` — build.gradle, module-info.java
- **Wave 2**: `feat(wizard): add WizardState, UserTypeDialog, and SelectorConfig with tests` — new *.java files
- **Wave 3**: `feat(wizard): simplify FrameMain and add Next button to install flow` — FrameMain.java, FramePCDownload.java
- **Wave 4**: `feat(discord): integrate JavaFX WebView with hybrid auto-navigation` — new Discord*.java files, FramePCPatcher.java
- **Wave 5**: `feat(wizard): wire full wizard sequence and integration tests` — integration test files
- **Post-verification**: `${type}(${scope}): ${desc}` — conventional commits

---

## Success Criteria

### Verification Commands
```bash
gradle build                          # Expected: BUILD SUCCESSFUL, no compile errors
gradle test                           # Expected: all tests pass, 0 failures
gradle jlink                          # Expected: creates runnable image with JavaFX modules
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests pass
- [ ] JavaFX WebView opens and renders Discord
- [ ] Wizard flow works end-to-end (both owner and new player paths)
- [ ] Manual fallback works when auto-extract fails
