# MindFocus

Context-aware focus mode MVP. Start a focus session, get interrupted by noise or movement, see it
counted, and have the session synced to a REST API as well as local storage.

Single `:app` module, Kotlin 2.1, Compose + Material 3, Koin, Room, Retrofit.

```
./gradlew :app:testDebugUnitTest   # 48 unit tests, all JVM, no device needed
./gradlew :app:lintDebug           # clean (0 errors)
./gradlew :app:assembleDebug
```

**No backend needed to run it.** `FocusSessionApi` is bound per variant: release builds get the
Retrofit client, debug builds get `FakeFocusSessionApi`, an in-memory stand-in for the three
endpoints. It keeps state across calls, so a session pushed by sync comes back on the next refresh;
it answers an unknown id with a real 404 so the server-error path is reachable by hand; and it takes
600ms to answer so loading states are visible rather than flashing past. It ships in no release
build — the debug and release halves of the binding live in their own source sets, so the fake is
not merely unused in release, it is not compiled into it.

The real base URL is a `buildConfigField` in `app/build.gradle.kts`.

---

## Architecture

The app follows standard MVVM with Clean Architecture patterns (layer separation, domain for business logic, data for all data handling including local and remote, all pure kotlin with no Android coupling and a separate presentation/UI layer that is coupled to Android) and uses Koin for dependency injection.

```
core/          model, DataError, Throttle, IdGenerator      — Pure Kotlin
domain/        repository + sensor + notifier interfaces,
               DistractionDetector, DistractionMonitor      — Pure Kotlin
data/          Room, Retrofit, DTO/entity mappers, repository impl — Room and Retrofit
sensor/        AudioRecord + SensorManager implementations       — Android
notification/  NotificationManager implementation                — Android
feature/       session (list + live timer) and sessiondetail screens, each with its ViewModel
navigation/    NavHost + type-safe routes
di/            Koin modules
```

**UI layer** is MVVM with MVI-style state: one immutable `FocusSessionUiState` exposed as a
`StateFlow`, one `Channel`-backed effects stream for fire-once messages, and a `@Stable`
`FocusSessionActions` interface so `FocusSessionContent` depends on callbacks rather than on the
ViewModel, which lets the previews work without Koin. `sessiondetail` follows the same
shape.

**Navigation** is Compose Navigation with `@Serializable` route objects rather than string paths, so
`SessionDetailRoute(sessionId)` is checked by the compiler and the argument arrives typed. The list
screen takes an `onNavigateToSession` lambda instead of a `NavController`. `SessionDetailViewModel`
gets its `sessionId` as a Koin runtime parameter (`parametersOf`), since it comes from the back
stack, not the graph.

Derived values (`isSessionActive`, `distractionCount`, `noiseCount`, `movementCount`) are getters on
the state class, not constructor parameters, so no `copy()` can produce a state that contradicts
itself.

**Three models, not one.** `FocusSessionDto` (wire, `@SerialName` snake_case), `FocusSessionEntity`
(Room, epoch millis) and `FocusSession` (domain, `Instant`, with `isActive` and the per-type counts)
are separate types with mappers in both directions. The cost is two mapper files; what it buys is
that a server rename or a schema migration stops at its own boundary instead of reaching the UI.

**Errors** are mapped once, at the repository boundary (`data/ErrorMapping.kt`). `IOException`,
`HttpException` and `SQLiteException` become a sealed `DataError`; the ViewModel maps that to a
`SessionErrorUi` enum; the composable maps *that* to a string resource. No layer handles a type it
should not know about, and the ViewModel never touches a `Context`.

**Offline first architecture.** Everything is written to Room first with the API requests only being done after. If they fail they can be retried and are still preserved offline so the app can be used entirely offline in practice with the only side effect being that the list of sessions will show an icon for the sessions that are saved offline/unsynced.

**One active session at a time**, enforced in the database rather than in the UI. The Start button
only becomes Stop once the write has round-tripped through Room, so on a slow device two quick taps
both see "Start" and both call `startSession`. The guard is a suspend `@Transaction` on the DAO that
checks for an active row and inserts only if there is none — a check-then-insert outside a
transaction would let both taps pass. `startSession` is therefore idempotent: the second call gets
the running session back instead of an error, since the user's intent is already satisfied and there
is nothing to report. Gating the button in the composable would have hidden the race rather than
closed it, and would not have covered any other caller.

---

## Native Resources

Both sensors sit behind one-property domain interfaces:

```kotlin
interface NoiseSource  { val readings: Flow<Float> }   // normalised 0f..1f amplitude
interface MotionSource { val readings: Flow<Float> }   // m/s², gravity removed
```

The contract is that both are **cold**: hardware is acquired when the flow is collected and released
when collection stops. `AndroidNoiseSource` builds and releases its `AudioRecord` inside the flow
body's `try/finally`; `AndroidMotionSource` is a `callbackFlow` that unregisters its listener in
`awaitClose`. There is no `start()`/`stop()` pair to leak, and cancelling the coroutine is the only
teardown path.

`DistractionMonitor` merges both, runs each reading through `DistractionDetector`, writes accepted
events to the repository, and notifies through a `DistractionNotifier` interface. It knows nothing
about `AudioRecord`, `SensorManager` or `NotificationManager`.

**Lifecycle.** Monitoring runs only when a session is active **and** the screen is resumed:

```kotlin
combine(activeSession, screenVisible) { session, visible -> session?.id?.takeIf { visible } }
    .distinctUntilChanged()
    .collectLatest { id -> id?.let { monitor.monitor(it) } }
```

`screenVisible` is driven by a `LifecycleResumeEffect` in the composable. The `distinctUntilChanged`
on the id is load-bearing: `activeSession` re-emits a new object every time a distraction is
recorded, and without it every event would tear down and restart the microphone.

**Permissions.** Permission state is Android framework state, so it stays in the UI
(`rememberFocusPermissionsState`) and not ViewModel (keeping VM free of Context). `RECORD_AUDIO` and (on 33+)
`POST_NOTIFICATIONS` are requested together; the screen shows a rationale and gates the Start button
on the microphone grant. Both implementations re-check their permission at the point of use rather than trusting
that the UI got it right.

---

## Battery and performance

The microphone is the most expensive thing the app does, so the primary control is
*when it runs at all*: only while a session is active **and** the screen is resumed.

Android's `AudioRecord` was used for the microphone. It reads a buffer, takes the peak, then sleeps 500ms. Two problems
with that, both worth knowing before anyone tunes thresholds against it: `read()` already blocks
until the buffer fills, so the sleep doesn't save any power (the mic stays open and keeps buffering
either way), and it means each cycle only inspects the audio it managed to read, anything loud that
lands in the sleep is never seen.

The accelerometer is registered at `SENSOR_DELAY_NORMAL` (~200ms) since it doesn't need much more than that given that
the user is not supposed to be moving the cellphone constantly and it would cost more battery, risk multiple distractions being logged, etc.

The elapsed-time ticker sits *inside* `flatMapLatest` on the active session, so an idle screen
schedules no work at all. `WhileSubscribed(5s)` on both `StateFlow`s means a backgrounded app stops
ticking and stops observing the database.

The notification channel is `IMPORTANCE_LOW`. An alert with a sound wouldn't make a lot of sense given the app's context (focus) and premise.

---

## How the code was kept testable

48 unit tests, all plain JVM (no emulator or Robolectric)

- **`DistractionDetector`** is the core rule engine and has no Android or coroutine dependency at
  all. It takes a reading and an `Instant` and returns a verdict, so thresholds, refractory windows
  and per-type independence are tested as pure functions.
- **`Clock` and `IdGenerator` are injected**, never called statically. `VirtualClock` drives
  `java.time` off the coroutine test scheduler, so `advanceTimeBy` moves the wall clock too otherwise every elapsed duration in a test reads zero.
- **`DistractionMonitor`** is tested against `FakeNoiseSource`/`FakeMotionSource` emitting fixed
  float sequences, proving the record-and-throttle wiring without any hardware.
- **`FocusSessionViewModel`** is tested against a fake repository, covering state transitions,
  the live timer, typed error mapping and sync-failure effects. `SessionDetailViewModel` gets the
  same treatment for its narrower surface: load, missing session, refresh, error routing.
- **The API and the error boundary are tested over real HTTP**, against `MockWebServer` rather than
  a mocked `FocusSessionApi`. A mocked API could only ever return the exceptions the test itself
  decided to throw, which proves nothing about the mapping; serving canned JSON to the real Retrofit
  client instead pins the endpoint paths, the snake_case `@SerialName` mapping and the tolerance of
  unknown fields, and letting OkHttp produce the failures pins the other half — 503 becomes
  `DataError.Server(503)`, a dropped socket becomes `DataError.Network`, and a rejected push leaves
  the row unsynced so the retry action still has something to retry.
- **`KoinModuleVerificationTest`** instantiates the whole graph with the four Android-backed
  bindings faked. Koin resolves at runtime, so this replaces Hilt's compile-time check.


---

## Trade-offs

**Single Gradle module.** Feature-vertical packages, module-shaped boundaries, but one `:app`.
Splitting into `:core:model` / `:data` / `:feature:session` would be the way but it's still unnecessary for an app this size, and
`core`/`domain` could be lifted out entirely.

**No Use Cases.** Having UseCase files is very common in Clean Architecture and right now the ViewModels call the repositories directly instead of using them. This makes sense right now because most of them would just be pass through, no real business logic are being done with the repo calls but of course if the app grew with new features and functions UseCases would probably be a better alternative.

**Koin over Hilt.** Faster builds and no KSP round for DI (KSP is still there for Room). The cost is
losing compile-time graph validation, but since it's a small app and the setup is relatively simple Koin feels more suitable.

**Sensor thresholds are fixed.** They are constructor parameters on
`DistractionThresholds` precisely so they can be tuned or made adaptive without touching the detector. But they could definitely be learned (ML) so the app would work better in different environments.

---

## Intentionally deprioritised

- **Background mode/Foreground service**: It would be the obvious next step for the app. Currently the focus sessions and distraction detection only work when the app is in foreground and for privacy reasons Android makes it more difficult to record the microphone while the app is the background.
- **Compose UI tests and instrumentation tests.** Currently the app's UI is very simple but if more things were added or if there was more time UI tests should be written. Also instrumentation/device tests could improve testing on notifications, Room, etc.
- **Automatic sync retry.** Failed pushes are retryable, but only manually, per row. Automatic sync would require a WorkManager.
- **Conflict resolution.** `refreshSessions` upserts whatever the server returns and deletes nothing. No tombstones, no clock-skew handling, mostly fine while one device writes; the first thing to design properly if a second one ever does.

---

## Scaling this to production

**Sync** manual per-row retry becomes `WorkManager` with a unique
periodic job, network constraints, exponential backoff, and a real conflict policy.

**Detection** would move behind a strategy interface. Right now fixed thresholds are being used but that would definitely be a problem for a real production app since users might be in different environments with different noise levels, movement level, etc.
Most likely ML could be used to determine the thresholds and several other strategies to improve detection.

**Modularisation** at three or more features: `:core:model`, `:core:data`, `:feature:*`, with convention plugins for the shared Android config.

**Observability** structured logging around sensor lifecycle and sync outcomes, crashlytics, monitoring, etc.

**Security** audio is already never persisted or transmitted, which is the property to defend.
Session data would move behind authenticated requests with certificate pinning, and Room would get
SQLCipher if distraction history is considered sensitive.

**Auth** for a production app it would definitely be benefitial to have proper authentication and user profiles.

---

## Accessibility

- Session status and sync state are conveyed by **icon shape and text**, never colour alone.
- The timer shows `12:34` but announces "Elapsed: 12 minutes, 34 seconds" — TalkBack reads the raw
  digits as a time of day. Plural-aware, via `pluralStringResource`.
- Composite items (stat tiles) are merged with `semantics(mergeDescendants = true)` so they announce
  as one node; section titles are marked `heading()` for jump navigation.
- Touch targets are floored at 48dp, including on M3 buttons that default to 40dp.
- All colours come from `MaterialTheme.colorScheme` (dynamic colour on 12+, static fallback), all
  text from `MaterialTheme.typography`, so contrast and font scaling both follow system settings.
  There is a `fontScale = 1.8f` preview alongside the light/dark ones.
- Body content is width-capped at 640dp so text does not stretch across a tablet.