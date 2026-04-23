# Graph Report - .  (2026-04-23)

## Corpus Check
- Large corpus: 240 files · ~145,633 words. Semantic extraction will be expensive (many Claude tokens). Consider running on a subfolder, or use --no-semantic to run AST-only.

## Summary
- 2215 nodes · 5283 edges · 41 communities detected
- Extraction: 60% EXTRACTED · 40% INFERRED · 0% AMBIGUOUS · INFERRED: 2108 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_AI Core Subsystem|AI Core Subsystem]]
- [[_COMMUNITY_Alias & Command Management|Alias & Command Management]]
- [[_COMMUNITY_AI Request & Callback Flow|AI Request & Callback Flow]]
- [[_COMMUNITY_Application Search & UI Utilities|Application Search & UI Utilities]]
- [[_COMMUNITY_TUI Command Execution (System)|TUI Command Execution (System)]]
- [[_COMMUNITY_TUI Command Execution (User)|TUI Command Execution (User)]]
- [[_COMMUNITY_UI Layout & Command Output|UI Layout & Command Output]]
- [[_COMMUNITY_App Management Commands|App Management Commands]]
- [[_COMMUNITY_Preferences & Configuration|Preferences & Configuration]]
- [[_COMMUNITY_Android System Activity Helpers|Android System Activity Helpers]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]

## God Nodes (most connected - your core abstractions)
1. `get()` - 104 edges
2. `Tuils` - 85 edges
3. `append()` - 47 edges
4. `SuggestionsManager` - 38 edges
5. `AppsManager` - 34 edges
6. `RssManager` - 33 edges
7. `UIManager` - 32 edges
8. `XMLPrefsManager` - 31 edges
9. `AISubsystem` - 31 edges
10. `exec()` - 31 edges

## Surprising Connections (you probably didn't know these)
- `cleanup()` --calls--> `clear`  [INFERRED]
  /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/MainManager.java → /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/clear.java
- `onCommandResult()` --calls--> `get()`  [INFERRED]
  /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/MainManager.java → /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/music.java
- `appendInfo()` --calls--> `append()`  [INFERRED]
  /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/volume.java → /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/shortcut.java
- `exec()` --calls--> `append()`  [INFERRED]
  /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/music.java → /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/shortcut.java
- `run()` --calls--> `get()`  [INFERRED]
  /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/UIManager.java → /Users/bhupendrasharma/Documents/AI Launcher/TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/music.java

## Communities

### Community 0 - "AI Core Subsystem"
Cohesion: 0.01
Nodes (41): ai, airplane, APICommand, beep, bluetooth, call, changelog, cleararchive (+33 more)

### Community 1 - "Alias & Command Management"
Cohesion: 0.03
Nodes (27): Alias, AliasManager, AppCapabilityScanner, Capability, PendingIntegration, exec(), CronManager, ScheduledTaskReceiver (+19 more)

### Community 2 - "AI Request & Callback Flow"
Cohesion: 0.02
Nodes (26): AICallback, AIProvider, AIRequest, Builder, AIResponse, AIListener, AISubsystem, AISubsystemToolExecutionTest (+18 more)

### Community 3 - "Application Search & UI Utilities"
Cohesion: 0.03
Nodes (25): Command, CommandGroup, CommandTuils, CompactionEngineTest, ContactManager, exec(), DirInfo, FileManager (+17 more)

### Community 4 - "TUI Command Execution (System)"
Cohesion: 0.03
Nodes (36): exec(), brightness, ExecutePack, exec(), exec(), NotificationManager, exec(), open (+28 more)

### Community 5 - "TUI Command Execution (User)"
Cohesion: 0.02
Nodes (54): alias, get(), label(), labels(), onNotArgEnough(), ArgInfo, config, get() (+46 more)

### Community 6 - "UI Layout & Command Output"
Cohesion: 0.03
Nodes (22): clear, CommandsPreferences, Html4EscapeSymbolsInitializer, Html5EscapeSymbolsInitializer, HtmlEscapeSymbols, Reference, References, Inputable (+14 more)

### Community 7 - "App Management Commands"
Cohesion: 0.04
Nodes (20): apps, exec(), get(), getLowercaseString(), getString(), label(), labels(), onArgNotFound() (+12 more)

### Community 8 - "Preferences & Configuration"
Cohesion: 0.02
Nodes (28): getLowercaseString(), getString(), label(), AISubsystemTest, defaultValue(), getLowercaseString(), getString(), label() (+20 more)

### Community 9 - "Android System Activity Helpers"
Cohesion: 0.02
Nodes (36): Activity, compare(), Assist, BoundApp, BroadcastReceiver, ClickableSpan, Contact, FakeLauncherActivity (+28 more)

### Community 10 - "Community 10"
Cohesion: 0.03
Nodes (14): Binder, KeeperService, MediaController, exec(), music, MusicController, MusicManager2, onServiceConnected() (+6 more)

### Community 11 - "Community 11"
Cohesion: 0.03
Nodes (12): BusyBoxInstaller, InstallationCallback, PkgInfo, CustomExceptionHandler, LongTermMemory, MemoryEntry, MimeTypes, StoppableThread (+4 more)

### Community 12 - "Community 12"
Cohesion: 0.04
Nodes (22): CompactionEngine, ConversationManager, ConversationManagerTest, HtmlEscape, InternalStringReader, getEscapeLevel(), getUseHexa(), getUseHtml5() (+14 more)

### Community 13 - "Community 13"
Cohesion: 0.04
Nodes (19): AndroidToolExecutor, AppCompatActivity, changeHint(), in(), LauncherActivity, AliasTrigger, AppTrigger, cleanup() (+11 more)

### Community 14 - "Community 14"
Cohesion: 0.05
Nodes (10): AllowEqualsSequence, Entry, onOutput(), OnTextChanged, Suggestion, SuggestionTextWatcher, run(), TerminalManager (+2 more)

### Community 15 - "Community 15"
Cohesion: 0.09
Nodes (8): OnLineListener, Interactive, OnCommandLineListener, OnCommandResultListener, OnResult, SH, Shell, SU

### Community 16 - "Community 16"
Cohesion: 0.07
Nodes (6): calc, CommandAbstraction, PermanentSuggestionCommand, Redirectator, RedirectCommand, sms

### Community 17 - "Community 17"
Cohesion: 0.08
Nodes (6): ChangelogManager, Device, Device, DeviceListener, OutputDevice, TorchManager

### Community 18 - "Community 18"
Cohesion: 0.12
Nodes (8): get(), getLowercaseString(), getString(), label(), labels(), onArgNotFound(), onNotArgEnough(), reply

### Community 19 - "Community 19"
Cohesion: 0.12
Nodes (8): get(), getLowercaseString(), getString(), label(), labels(), notifications, onArgNotFound(), onNotArgEnough()

### Community 20 - "Community 20"
Cohesion: 0.13
Nodes (2): Builder, ShellHolder

### Community 21 - "Community 21"
Cohesion: 0.25
Nodes (2): DeviceAdminReceiver, PolicyReceiver

### Community 22 - "Community 22"
Cohesion: 0.25
Nodes (3): ShortcutBridge, ShortcutInfoCompat, ShortcutBridgeTest

### Community 23 - "Community 23"
Cohesion: 0.33
Nodes (1): XMLPrefsElement

### Community 24 - "Community 24"
Cohesion: 0.33
Nodes (1): AIProvider

### Community 25 - "Community 25"
Cohesion: 0.4
Nodes (1): OnBatteryUpdate

### Community 26 - "Community 26"
Cohesion: 0.5
Nodes (1): OutlineEditText

### Community 27 - "Community 27"
Cohesion: 0.5
Nodes (1): OutlineTextView

### Community 28 - "Community 28"
Cohesion: 0.5
Nodes (2): OutputDevice, Torch

### Community 29 - "Community 29"
Cohesion: 0.5
Nodes (2): Flashlight, Torch

### Community 30 - "Community 30"
Cohesion: 0.67
Nodes (1): PlaceholderTest

### Community 31 - "Community 31"
Cohesion: 0.67
Nodes (1): CommandExecuter

### Community 32 - "Community 32"
Cohesion: 0.67
Nodes (1): ToolExecutor

### Community 33 - "Community 33"
Cohesion: 0.67
Nodes (1): ToolCall

### Community 34 - "Community 34"
Cohesion: 0.67
Nodes (1): JourneyManager

### Community 35 - "Community 35"
Cohesion: 0.67
Nodes (1): APICommand

### Community 36 - "Community 36"
Cohesion: 1.0
Nodes (1): Constants

### Community 40 - "Community 40"
Cohesion: 1.0
Nodes (1): Termux RUN_COMMAND Bridge

### Community 41 - "Community 41"
Cohesion: 1.0
Nodes (1): Cron/AlarmManager Scheduler

### Community 42 - "Community 42"
Cohesion: 1.0
Nodes (1): Thread-Level Cancellation Flow

### Community 43 - "Community 43"
Cohesion: 1.0
Nodes (1): ADB Logcat Dev Loop

## Knowledge Gaps
- **5 isolated node(s):** `Constants`, `Termux RUN_COMMAND Bridge`, `Cron/AlarmManager Scheduler`, `Thread-Level Cancellation Flow`, `ADB Logcat Dev Loop`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 20`** (19 nodes): `.getBusyboxPath()`, `.isInstalled()`, `ShellHolder.java`, `Builder`, `.addEnvironment()`, `.open()`, `.setAutoHandler()`, `.setHandler()`, `.setOnSTDERRLineListener()`, `.setOnSTDOUTLineListener()`, `.setShell()`, `.setWantSTDERR()`, `.setWatchdogTimeout()`, `.useSH()`, `.useSU()`, `ShellHolder`, `.build()`, `.setupBusyBox()`, `.ShellHolder()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (9 nodes): `DeviceAdminReceiver`, `PolicyReceiver.java`, `PolicyReceiver`, `.onDisabled()`, `.onDisableRequested()`, `.onEnabled()`, `.onPasswordChanged()`, `.onPasswordFailed()`, `.onPasswordSucceeded()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 23`** (6 nodes): `XMLPrefsElement.java`, `XMLPrefsElement`, `.delete()`, `.getValues()`, `.path()`, `.write()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 24`** (6 nodes): `AIProvider`, `.complete()`, `.providerId()`, `.supportsStreaming()`, `.supportsToolUse()`, `AIProvider.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 25`** (5 nodes): `OnBatteryUpdate.java`, `OnBatteryUpdate`, `.onCharging()`, `.onNotCharging()`, `.update()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 26`** (4 nodes): `OutlineEditText.java`, `OutlineEditText`, `.draw()`, `.OutlineEditText()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 27`** (4 nodes): `OutlineTextView.java`, `OutlineTextView`, `.draw()`, `.OutlineTextView()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 28`** (4 nodes): `Torch.java`, `OutputDevice`, `Torch`, `.Torch()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 29`** (4 nodes): `Flashlight`, `.Flashlight()`, `Flashlight.java`, `Torch`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 30`** (3 nodes): `PlaceholderTest`, `.infraWorks()`, `PlaceholderTest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 31`** (3 nodes): `CommandExecuter`, `.execute()`, `CommandExecuter.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 32`** (3 nodes): `ToolExecutor.java`, `ToolExecutor`, `.execute()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 33`** (3 nodes): `ToolCall.java`, `ToolCall`, `.ToolCall()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 34`** (3 nodes): `JourneyManager`, `.isJourneyNotification()`, `JourneyManager.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 35`** (3 nodes): `APICommand`, `.willWorkOn()`, `APICommand.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 36`** (2 nodes): `Constants`, `Constants.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 40`** (1 nodes): `Termux RUN_COMMAND Bridge`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 41`** (1 nodes): `Cron/AlarmManager Scheduler`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 42`** (1 nodes): `Thread-Level Cancellation Flow`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 43`** (1 nodes): `ADB Logcat Dev Loop`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `get()` connect `Alias & Command Management` to `AI Core Subsystem`, `AI Request & Callback Flow`, `Application Search & UI Utilities`, `TUI Command Execution (System)`, `UI Layout & Command Output`, `Preferences & Configuration`, `Android System Activity Helpers`, `Community 10`, `Community 11`, `Community 12`, `Community 13`, `Community 14`, `Community 15`, `Community 16`, `Community 20`, `Community 22`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Why does `Tuils` connect `Community 11` to `Alias & Command Management`, `Application Search & UI Utilities`, `TUI Command Execution (System)`, `TUI Command Execution (User)`, `UI Layout & Command Output`, `App Management Commands`, `Preferences & Configuration`, `Android System Activity Helpers`, `Community 10`, `Community 13`, `Community 14`, `Community 15`, `Community 16`?**
  _High betweenness centrality (0.054) - this node is a cross-community bridge._
- **Why does `MusicService` connect `Community 10` to `Application Search & UI Utilities`, `UI Layout & Command Output`, `App Management Commands`?**
  _High betweenness centrality (0.041) - this node is a cross-community bridge._
- **Are the 100 inferred relationships involving `get()` (e.g. with `.parseResponse_returnsToolCalls()` and `.parseResponse_extractsFencedToolCallAsToolCall()`) actually correct?**
  _`get()` has 100 INFERRED edges - model-reasoned connections that need verification._
- **Are the 44 inferred relationships involving `append()` (e.g. with `.applyCompaction_replacesHistory()` and `.stateless_historyAlwaysEmpty()`) actually correct?**
  _`append()` has 44 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Constants`, `Termux RUN_COMMAND Bridge`, `Cron/AlarmManager Scheduler` to the rest of the system?**
  _5 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AI Core Subsystem` be split into smaller, more focused modules?**
  _Cohesion score 0.01 - nodes in this community are weakly interconnected._