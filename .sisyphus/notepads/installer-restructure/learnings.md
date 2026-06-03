# Learnings

## 2026-05-29: SelectorConfig & Properties

- Created `discord-selectors.properties` with 8 keys for Discord server/channel/reaction/URL configuration
- Created `SelectorConfig.java` — instance-based config holder using `java.util.Properties`
- Uses `getClass().getClassLoader().getResourceAsStream(...)` for classpath resource loading (consistent with the existing `Helpers.class.getClassLoader().getResource()` pattern)
- Graceful fallback: constructor catches `IOException` and silently continues with empty properties; each getter provides a hardcoded default via `properties.getProperty(key, default)`
- `getTimeoutSeconds()` returns `int` with `NumberFormatException` safety net (defaults to 30)
- No compile errors in SelectorConfig.java
- Build failure is pre-existing (JavaFX module resolution in module-info.java)

## 2026-05-29: module-info.java JavaFX requires

- Added `requires javafx.controls`, `requires javafx.fxml`, `requires javafx.web`, `requires javafx.graphics` to module-info.java
- The `org.openjfx.javafxplugin` Gradle plugin (v0.0.13) is needed to put JavaFX jars on the module path during compilation
- Without the plugin, `implementation` deps go to classpath, but `requires` in module-info.java resolves from module path only
- Plugin config: `version = '17.0.2'`, `modules = ['javafx.controls', 'javafx.fxml', 'javafx.web']`
- `javafx.base` is not explicitly required — transitively included via controls/graphics
- Build verified: `gradle compileJava --no-daemon` → BUILD SUCCESSFUL

## 2026-05-29: Task 1 — Uncomment JavaFX deps & update jlink

- Uncommented `javafx-controls` and `javafx-fxml` deps (lines 47-48), added `javafx-web`
- Updated jlink options: `jdk.zipfs,javafx.controls,javafx.fxml,javafx.web,javafx.graphics,javafx.base`
- **Critical**: JavaFX JARs from Maven Central are automatic modules (no `module-info.class`). Gradle puts them on classpath, not module path. Our `module-info.java` `requires` them from module path → compilation fails.
- **Fix**: Added `--module-path` to compileJava via `doFirst { options.compilerArgs += ['--module-path', configurations.runtimeClasspath.asPath] }`. Must use `doFirst` (execution phase) not configuration phase to avoid "Cannot change dependencies after resolution" error.
- **Gradle wrapper**: Had to upgrade from 7.2 → 8.5 because JDK 21 (class file major 65) is incompatible with Gradle 7.x Groovy compiler. Updated `gradle/wrapper/gradle-wrapper.properties`.
- **jpackage** on Linux may fail intermittently due to caching issues; works on second run.
- `gradle dependencies --configuration runtimeClasspath | grep javafx` confirms all modules resolved.
- Full `gradle clean build --no-daemon` → BUILD SUCCESSFUL (17 tasks, 2m5s).

## 2026-05-29: Task 5 — WizardState class with tests

- Created `WizardState.java` — pure data class with `UserType` enum (OWNER, NEW_PLAYER), `userType` (default null), `installPath` (default "")
- Path normalization in `setInstallPath()`: replaces `\` with `/`, trims trailing `/`
- `getBinPath()` returns `installPath + "/bin/win10"` for patcher DLL location
- `toString()` format: `WizardState{userType=OWNER, path=C:/EchoVR/...}`
- No UI logic, no Singleton, no persistence — pure state holder
- All 6 tests pass: default state, setUserType (both enum values), set/get installPath, path normalization (backslash→forward slash), toString coverage

## 2026-05-29: Task 7 — SelectorConfig tests (RED → GREEN)

- Created `SelectorConfigTest.java` with 10 test methods covering all getters
- Tests verify against actual `discord-selectors.properties` values (server invite, channel name, timeout=30)
- Non-null/non-empty checks for CSS selector strings, emoji, regex pattern
- Includes safety net: `testAllGettersReturnNonNull` (calls every getter) and `testDefaultConstructorDoesNotThrow`
- Pattern: `@BeforeEach setUp() { config = new SelectorConfig(); }` for clean state per test
- JUnit 5 annotations/style matches existing `SmokeTest.java`
- All 10 tests pass: `tests="10" skipped="0" failures="0" errors="0"`
