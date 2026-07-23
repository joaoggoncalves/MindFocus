package com.kis.mindfocus.feature.sessiondetail

import app.cash.turbine.test
import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.core.model.DistractionEvent
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.feature.session.SessionErrorUi
import com.kis.mindfocus.testing.FakeFocusSessionRepository
import com.kis.mindfocus.testing.MainDispatcherRule
import com.kis.mindfocus.testing.VirtualClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val clock = VirtualClock(dispatcher.scheduler)
    private val repository = FakeFocusSessionRepository(clock)

    private fun viewModel(sessionId: String) =
        SessionDetailViewModel(sessionId = sessionId, repository = repository)

    private suspend fun aRecordedSession(): String {
        val id = repository.startSession().getOrThrow().id
        repository.recordDistraction(
            id,
            DistractionEvent("e1", DistractionType.NOISE, clock.instant(), 2f),
        )
        repository.recordDistraction(
            id,
            DistractionEvent("e2", DistractionType.MOVEMENT, clock.instant(), 3f),
        )
        repository.endSession(id)
        return id
    }

    @Test
    fun `the session is read from the local store, not the network`() = runTest(dispatcher) {
        val id = aRecordedSession()
        // Every remote call fails; the screen must still render from Room.
        repository.failure = DataError.Network(IOException("offline"))

        val viewModel = viewModel(id)

        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(id, state.session?.id)
            assertEquals(2, state.distractions.size)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distractions are ordered newest first`() = runTest(dispatcher) {
        val id = repository.startSession().getOrThrow().id
        val start = clock.instant()
        repository.recordDistraction(
            id,
            DistractionEvent("old", DistractionType.NOISE, start, 1f),
        )
        repository.recordDistraction(
            id,
            DistractionEvent("new", DistractionType.MOVEMENT, start.plusSeconds(60), 1f),
        )

        val viewModel = viewModel(id)

        viewModel.uiState.test {
            advanceUntilIdle()

            assertEquals(
                listOf("new", "old"),
                expectMostRecentItem().distractions.map { it.id },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duration is measured from start to end`() = runTest(dispatcher) {
        val id = repository.startSession().getOrThrow().id
        advanceUntilIdle()
        dispatcher.scheduler.advanceTimeBy(Duration.ofMinutes(25).toMillis())
        repository.endSession(id)

        val viewModel = viewModel(id)

        viewModel.uiState.test {
            advanceUntilIdle()

            assertEquals(Duration.ofMinutes(25), expectMostRecentItem().duration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a background refresh failure is not surfaced`() = runTest(dispatcher) {
        val id = aRecordedSession()
        repository.failure = DataError.Network(IOException("offline"))

        val viewModel = viewModel(id)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a user-initiated refresh reports offline as a passing message`() = runTest(dispatcher) {
        val id = aRecordedSession()
        val viewModel = viewModel(id)
        advanceUntilIdle()

        viewModel.effects.test {
            repository.failure = DataError.Network(IOException("offline"))
            viewModel.onRefresh()
            advanceUntilIdle()

            val effect = awaitItem() as SessionDetailEffect.ShowError
            assertEquals(SessionErrorUi.NoConnection, effect.error)
            assertNull(viewModel.uiState.value.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an unknown session id reports as missing rather than loading forever`() =
        runTest(dispatcher) {
            val viewModel = viewModel("does-not-exist")

            viewModel.uiState.test {
                advanceUntilIdle()
                val state = expectMostRecentItem()

                assertTrue(state.isMissing)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
