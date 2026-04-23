# Codebase Refactoring & Optimization Plan

**Date:** 2026-04-23
**Strategy:** Incremental (Bottom-Up) Refactoring
**DI Framework:** Dagger/Hilt

## Objective
Transform the TUI-ConsoleLauncher from a monolithic architecture into a modular, robust, and extensible system. This will eliminate "God objects" (`Tuils`, `MainPack`), streamline the execution pipeline, and create a solid foundation for future AI and system integrations.

## Phase 1: AI Subsystem Modularization
*Focus: Isolate the AI tool execution logic from the monolithic `AndroidToolExecutor`.*

1. **Define `AITool` Interface:** Create a standard contract for all AI tools (e.g., `execute(Context, JSONObject args)`).
2. **Implement `ToolRegistry`:** Develop a dynamic registry capable of discovering and storing `AITool` implementations.
3. **Migrate Tools:** Extract the massive `if-else` block in `AndroidToolExecutor` into discrete, focused classes (e.g., `WebFetchTool`, `BrightnessTool`, `ContactManagerTool`).
4. **Externalize Prompts:** Move the hardcoded AI system prompt from `AISubsystem.java` to an external Markdown or XML resource file, enabling updates without recompilation.

## Phase 2: Utility Decomposition (`Tuils.java`)
*Focus: Break down the 1600+ line catch-all utility class into cohesive domain managers.*

1. **`FileSystemManager`:** Extract all file I/O operations (`download`, `write`, `delete`, `openFile`, `readerToString`).
2. **`SpannableManager` / `TextProcessor`:** Isolate UI text processing, Markdown parsing, color spanning, and generic string calculus.
3. **`DeviceStateManager`:** Encapsulate Android system utilities (battery status, notification checks, available storage, network connectivity).
4. **Integration:** Systematically replace `Tuils.*` calls across the codebase with their respective manager invocations.

## Phase 3: Dependency Injection (Dagger/Hilt Foundation)
*Focus: Establish the standard dependency injection framework to manage the new modules.*

1. **Gradle Setup:** Add required Dagger/Hilt dependencies and plugins to `app/build.gradle` and the project root `build.gradle`.
2. **Application Class:** Create/update the main application class and annotate it with `@HiltAndroidApp`.
3. **Hilt Modules:** Define modules to provide singletons and bindings for the newly created managers (`FileSystemManager`, `DeviceStateManager`, `ToolRegistry`, `AISubsystem`).

## Phase 4: Core Command Pipeline Decoupling (`MainPack`)
*Focus: Remove the `MainPack` "God object" dependency from the command execution pipeline.*

1. **Define `CommandContext`:** Create an interface representing the isolated execution environment for a command, exposing only necessary APIs (input, output, theme data).
2. **Refactor `ExecutePack`:** Transition `ExecutePack` to implement `CommandContext` and act as a proxy to injected services, rather than holding raw data fields.
3. **Inject Commands:** Update `CommandAbstraction` implementations to request specific managers via constructor or method injection (facilitated by Hilt) instead of pulling them from `MainPack`.
4. **Activity/Manager Annotations:** Annotate Android entry points (`LauncherActivity`) and core managers (`TerminalManager`, `MainManager`) with `@AndroidEntryPoint` to enable Hilt field injection.

## Phase 5: Streamlining Execution (BusyBox Deprecation)
*Focus: Optimize raw shell command execution by leveraging the recent Termux integration.*

1. **Audit Legacy Paths:** Identify all usages of `ShellHolder.java` and `BusyBoxInstaller.java`.
2. **Migrate to `TermuxManager`:** Refactor commands that rely on bundled Linux binaries (e.g., `ls`, `grep`, `awk`) to execute via the established `TermuxManager` bridge.
3. **Refactor `status.java`:** Move fragmented, low-level Android API calls used in status reporting into the `DeviceStateManager`.
4. **Cleanup:** Remove the obsolete BusyBox binaries and associated Java wrapper classes to significantly reduce the application's footprint.

## Success Criteria
- [ ] `AndroidToolExecutor` contains no tool-specific logic.
- [ ] `Tuils.java` is significantly reduced in size or eliminated entirely.
- [ ] Dagger/Hilt is successfully managing the lifecycle of core services.
- [ ] `MainPack` is no longer a mandatory parameter for command execution.
- [ ] Legacy BusyBox dependencies are removed.
