# Coroutines and Architecture Hardening Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Preserve current refactor momentum while closing high-risk coroutine and layering gaps (cancellation safety, dispatcher/scope hygiene, and Clean Architecture dependency direction).

**Architecture:** Keep the existing MVVM + capability-repository direction, but enforce strict dependency flow (UI -> domain -> data) and structured concurrency rules from AGENTS.md. Execute quick, test-backed fixes first, then apply deeper boundary refactors for incoming watcher and wiki/glossary flows.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, Jetpack Compose, JUnit, MockK (or existing test doubles), Gradle.

---

> **Execution note:** This plan is intentionally designed for the current branch and workspace (no worktree setup).

### Task 1: Cancellation Safety Hotfixes

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/wallets/receive/ReceiveRoute.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/data/wallet/WalletAddressManager.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcher.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/wallets/receive/ReceiveViewModelTest.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcherTest.kt`
- Create test: `app/src/test/java/com/strhodler/utxopocket/data/wallet/WalletAddressManagerCancellationTest.kt`

**Step 1: Write the failing tests**

```kotlin
@Test
fun checkAddress_rethrowsCancellationException() = runTest {
    coEvery { incomingTxChecker.manualCheck(any(), any()) } throws CancellationException("cancel")
    assertFailsWith<CancellationException> { viewModel.checkAddress() }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReceiveViewModelTest" --tests "*IncomingTxWatcherTest" --tests "*WalletAddressManagerCancellationTest"`
Expected: FAIL with swallowed-cancellation assertions.

**Step 3: Write minimal implementation**

```kotlin
catch (cancel: CancellationException) {
    throw cancel
} catch (t: Throwable) {
    // existing fallback behavior
}
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReceiveViewModelTest" --tests "*IncomingTxWatcherTest" --tests "*WalletAddressManagerCancellationTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/wallets/receive/ReceiveRoute.kt app/src/main/java/com/strhodler/utxopocket/data/wallet/WalletAddressManager.kt app/src/main/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcher.kt app/src/test/java/com/strhodler/utxopocket/presentation/wallets/receive/ReceiveViewModelTest.kt app/src/test/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcherTest.kt app/src/test/java/com/strhodler/utxopocket/data/wallet/WalletAddressManagerCancellationTest.kt
git commit -m "fix(coroutines): preserve cancellation across receive and incoming flows"
```

### Task 2: Seal ViewModel Contracts and Remove Unused Presentation Dependency

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/node/NodeStatusViewModel.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/MainActivityViewModel.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/node/NodeStatusViewModelTest.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityStatusProjectionTest.kt`

**Step 1: Write failing tests**

```kotlin
@Test
fun events_exposedAsReadOnlySharedFlow() {
    assertTrue(viewModel.events !is MutableSharedFlow<*>)
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*NodeStatusViewModelTest" --tests "*MainActivityStatusProjectionTest"`
Expected: FAIL for contract exposure and constructor wiring.

**Step 3: Write minimal implementation**

```kotlin
private val _events = MutableSharedFlow<NodeStatusEvent>(...)
val events: SharedFlow<NodeStatusEvent> = _events.asSharedFlow()
```

Also remove unused `NetworkStatusMonitor` dependency/import from `MainActivityViewModel` constructor.

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*NodeStatusViewModelTest" --tests "*MainActivityStatusProjectionTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/node/NodeStatusViewModel.kt app/src/main/java/com/strhodler/utxopocket/presentation/MainActivityViewModel.kt app/src/test/java/com/strhodler/utxopocket/presentation/node/NodeStatusViewModelTest.kt app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityStatusProjectionTest.kt
git commit -m "refactor(presentation): tighten event contracts and prune unused dependencies"
```

### Task 3: Dispatcher Injection Compliance (Data Layer)

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/data/preferences/DefaultAppPreferencesRepository.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/data/node/NodeConnectionTester.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/di/CoroutinesModule.kt` (only if new qualifier bindings are required)
- Test: `app/src/test/java/com/strhodler/utxopocket/data/node/NodeConnectionTesterTest.kt` (create if missing)
- Test: `app/src/test/java/com/strhodler/utxopocket/data/preferences/DefaultAppPreferencesRepositoryTest.kt` (create if missing)

**Step 1: Write failing tests**

```kotlin
@Test
fun verifyPin_usesInjectedDispatcher() = runTest(testDispatcher) {
    repository.verifyPin("123456")
    advanceUntilIdle()
    // assert deterministic completion under test dispatcher
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*NodeConnectionTesterTest" --tests "*DefaultAppPreferencesRepositoryTest"`
Expected: FAIL due to constructor/dispatcher changes not yet applied.

**Step 3: Write minimal implementation**

```kotlin
class DefaultNodeConnectionTester @Inject constructor(
    ...,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
)
```

```kotlin
class DefaultAppPreferencesRepository @Inject constructor(
    ...,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
)
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*NodeConnectionTesterTest" --tests "*DefaultAppPreferencesRepositoryTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/data/preferences/DefaultAppPreferencesRepository.kt app/src/main/java/com/strhodler/utxopocket/data/node/NodeConnectionTester.kt app/src/main/java/com/strhodler/utxopocket/di/CoroutinesModule.kt app/src/test/java/com/strhodler/utxopocket/data/node/NodeConnectionTesterTest.kt app/src/test/java/com/strhodler/utxopocket/data/preferences/DefaultAppPreferencesRepositoryTest.kt
git commit -m "refactor(coroutines): inject dispatchers in data services"
```

### Task 4: Replace Fragile Aggregate Casting in MainActivityViewModel

**Files:**
- Modify: `app/src/main/java/com/strhodler/utxopocket/presentation/MainActivityViewModel.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityStatusProjectionTest.kt`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityViewModelDuressUnlockTest.kt`

**Step 1: Write failing tests**

```kotlin
@Test
fun uiState_projection_worksWithTypedAggregation() = runTest {
    // Arrange multi-flow inputs and assert stable mapping
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*MainActivityStatusProjectionTest" --tests "*MainActivityViewModelDuressUnlockTest"`
Expected: FAIL once typed aggregator scaffolding is introduced without full wiring.

**Step 3: Write minimal implementation**

```kotlin
private data class MainInputs(...)
private data class MainUiPrefs(...)
// compose with staged combine() calls, avoid values[index] casts
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*MainActivityStatusProjectionTest" --tests "*MainActivityViewModelDuressUnlockTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/presentation/MainActivityViewModel.kt app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityStatusProjectionTest.kt app/src/test/java/com/strhodler/utxopocket/presentation/MainActivityViewModelDuressUnlockTest.kt
git commit -m "refactor(presentation): type main activity state aggregation"
```

### Task 5: Move IncomingTxWatcher Implementation Out of Domain Package

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/data/incoming/DefaultIncomingTxWatcher.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcher.kt` (convert to interface or remove impl)
- Modify: `app/src/main/java/com/strhodler/utxopocket/di/RepositoryModule.kt` (or appropriate DI module)
- Modify: call sites currently injecting concrete watcher (e.g. `MainActivityViewModel`)
- Test: `app/src/test/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcherTest.kt` (adapt package/imports)

**Step 1: Write failing tests**

```kotlin
@Test
fun watcher_boundThroughDomainPort() {
    // test DI/constructor path uses IncomingTxChecker port
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*IncomingTxWatcherTest" --tests "*MainActivity*Test"`
Expected: FAIL while bindings and imports are in transition.

**Step 3: Write minimal implementation**

```kotlin
interface IncomingTxWatcher : IncomingTxChecker {
    fun setForeground(foreground: Boolean)
}

@Singleton
class DefaultIncomingTxWatcher @Inject constructor(...) : IncomingTxWatcher
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*IncomingTxWatcherTest" --tests "*MainActivity*Test"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/data/incoming/DefaultIncomingTxWatcher.kt app/src/main/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcher.kt app/src/main/java/com/strhodler/utxopocket/di/RepositoryModule.kt app/src/main/java/com/strhodler/utxopocket/presentation/MainActivityViewModel.kt app/src/test/java/com/strhodler/utxopocket/domain/service/IncomingTxWatcherTest.kt
git commit -m "refactor(incoming): bind watcher implementation from data layer"
```

### Task 6: Break Wiki/Glossary Layering Cycle

**Files:**
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/repository/WikiRepository.kt`
- Create: `app/src/main/java/com/strhodler/utxopocket/domain/repository/GlossaryRepository.kt`
- Create: domain models for wiki/glossary currently living in presentation packages
- Modify: `app/src/main/java/com/strhodler/utxopocket/data/wiki/DefaultWikiRepository.kt`
- Modify: `app/src/main/java/com/strhodler/utxopocket/data/glossary/DefaultGlossaryRepository.kt`
- Modify: presentation ViewModels/routes consuming data-layer repositories directly
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/wiki/*`
- Test: `app/src/test/java/com/strhodler/utxopocket/presentation/glossary/*`

**Step 1: Write failing tests**

```kotlin
@Test
fun wikiViewModel_dependsOnDomainRepositoryContract() {
    // compile-level contract + behavior assertion
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*Wiki*Test" --tests "*Glossary*Test"`
Expected: FAIL while models/contracts are migrated.

**Step 3: Write minimal implementation**

```kotlin
interface WikiRepository { fun getContent(...): Flow<WikiContentModel> }
interface GlossaryRepository { fun entries(...): Flow<List<GlossaryEntryModel>> }
```

**Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*Wiki*Test" --tests "*Glossary*Test"`
Expected: PASS.

**Step 5: Commit**

```bash
git add app/src/main/java/com/strhodler/utxopocket/domain/repository/WikiRepository.kt app/src/main/java/com/strhodler/utxopocket/domain/repository/GlossaryRepository.kt app/src/main/java/com/strhodler/utxopocket/data/wiki/DefaultWikiRepository.kt app/src/main/java/com/strhodler/utxopocket/data/glossary/DefaultGlossaryRepository.kt app/src/main/java/com/strhodler/utxopocket/presentation/wiki/WikiViewModel.kt app/src/main/java/com/strhodler/utxopocket/presentation/glossary/GlossaryViewModel.kt
git commit -m "refactor(architecture): restore wiki and glossary layer boundaries"
```

### Task 7: Full Verification Gate (Required Before Completion)

**Files:**
- No code changes expected unless fixes are required by verification output.

**Step 1: Run lint gate**

Run: `./gradlew lintDebug`
Expected: PASS.

**Step 2: Run unit-test gate**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS.

**Step 3: Run instrumentation gate (network/Tor-sensitive changes touched)**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS.

**Step 4: If any gate fails, apply minimal fix and rerun failed gate**

```bash
./gradlew <failed-task>
```

**Step 5: Final commit (if verification produced changes)**

```bash
git add <verification-fix-files>
git commit -m "fix(tests): address verification regressions after architecture hardening"
```
