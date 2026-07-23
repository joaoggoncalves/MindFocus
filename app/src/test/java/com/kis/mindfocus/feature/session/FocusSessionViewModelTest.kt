package com.kis.mindfocus.feature.session

import app.cash.turbine.test
import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.domain.detection.DistractionDetector
import com.kis.mindfocus.domain.detection.DistractionMonitor
import com.kis.mindfocus.testing.FakeFocusSessionRepository
import com.kis.mindfocus.testing.FakeMotionSource
import com.kis.mindfocus.testing.FakeNoiseSource
import com.kis.mindfocus.testing.RecordingNotifier
import com.kis.mindfocus.testing.MainDispatcherRule
import com.kis.mindfocus.testing.VirtualClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class FocusSessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val clock = VirtualClock(dispatcher.scheduler)
    private val repository = FakeFocusSessionRepository(clock)

    private fun TestScope.createViewModel() = FocusSessionViewModel(
        repository = repository,
        // Silent sensors: this class tests session and state behaviour, not detection.
        monitor = DistractionMonitor(
            noiseSource = FakeNoiseSource(),
            motionSource = FakeMotionSource(),
            detector = DistractionDetector(),
            repository = repository,
            notifier = RecordingNotifier(),
            idGenerator = { "event" },
            clock = clock,
        ),
        clock = clock,
        applicationScope = backgroundScope,
    )

    /**
     * The elapsed-time ticker is an infinite loop, so `advanceUntilIdle` never returns while a
     * session is running and `uiState` is being collected. Tests in that state advance a bounded
     * amount of virtual time instead.
     */
    @Test
    fun `starting a session marks the state active`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceTimeBy(SETTLE_MILLIS)
            assertFalse(expectMostRecentItem().isSessionActive)

            viewModel.onStartSession()
            advanceTimeBy(SETTLE_MILLIS)

            assertTrue(expectMostRecentItem().isSessionActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `elapsed time advances once per second while a session runs`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            viewModel.onStartSession()
            // One millisecond past the third tick: advanceTimeBy stops short of the boundary
            // itself, so an exact 3000 would only run the ticks at 0, 1000 and 2000.
            advanceTimeBy(Duration.ofSeconds(3).toMillis() + 1)

            assertEquals(Duration.ofSeconds(3), expectMostRecentItem().elapsed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `elapsed time stays at zero when no session is running`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            advanceTimeBy(Duration.ofSeconds(10).toMillis())

            assertEquals(Duration.ZERO, expectMostRecentItem().elapsed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a storage failure becomes a typed UI error, not an exception`() = runTest(dispatcher) {
        repository.failure = DataError.Local(IOException("disk"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            viewModel.onStartSession()
            advanceUntilIdle()

            assertEquals(SessionErrorUi.StorageFailure, expectMostRecentItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the startup refresh stays silent when the API is unreachable`() = runTest(dispatcher) {
        repository.failure = DataError.Network(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Nothing is lost when a background refresh fails, so nothing is reported.
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a user-initiated refresh reports offline as a passing message, not a banner`() =
        runTest(dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.effects.test {
                repository.failure = DataError.Network(IOException("offline"))
                viewModel.onRefresh()
                advanceUntilIdle()

                val effect = awaitItem() as FocusSessionEffect.ShowError
                assertEquals(SessionErrorUi.NoConnection, effect.error)
                assertNull(viewModel.uiState.value.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dismissing an error clears it`() = runTest(dispatcher) {
        repository.failure = DataError.Local(IOException("disk"))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            viewModel.onStartSession()
            advanceUntilIdle()
            assertEquals(SessionErrorUi.StorageFailure, expectMostRecentItem().error)

            viewModel.onErrorDismissed()
            advanceUntilIdle()

            assertNull(expectMostRecentItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stopping a session moves it into history and syncs it`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            viewModel.onStartSession()
            advanceTimeBy(Duration.ofSeconds(5).toMillis())
            viewModel.onStopSession()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isSessionActive)
            assertEquals(1, state.history.size)
            assertEquals(Duration.ofSeconds(5), state.history.single().duration)
            assertTrue(state.history.single().isSynced)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed sync reports a message and leaves the session unsynced`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onStartSession()
        advanceUntilIdle()

        viewModel.effects.test {
            repository.failure = DataError.Network(IOException("offline"))
            viewModel.onRetrySync("session-0")
            advanceUntilIdle()

            val effect = awaitItem() as FocusSessionEffect.ShowMessage
            assertEquals(SessionMessageUi.SyncFailed, effect.message)
            assertFalse(viewModel.uiState.value.history.any { it.isSynced })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        /** Long enough for pending dispatches to run, short of the ticker's one-second period. */
        const val SETTLE_MILLIS = 100L
    }
}
