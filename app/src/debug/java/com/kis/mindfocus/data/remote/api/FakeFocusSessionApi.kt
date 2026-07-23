package com.kis.mindfocus.data.remote.api

import com.kis.mindfocus.data.remote.dto.DistractionEventDto
import com.kis.mindfocus.data.remote.dto.FocusSessionDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Stands in for the API in debug builds, where [com.kis.mindfocus.BuildConfig.BASE_URL] points at a
 * host that does not resolve — without this, every refresh and every sync fails with
 * `DataError.Network` and the sync, retry and unsynced-badge paths cannot be exercised by hand.
 *
 * Deliberately not a "always succeeds instantly" stub: it holds state across calls so a pushed
 * session comes back on the next refresh, it answers an unknown id with a real 404 so the
 * `DataError.Server` path is reachable, and it takes [LATENCY] to answer so loading states are
 * visible rather than flashing past.
 */
class FakeFocusSessionApi(clock: Clock) : FocusSessionApi {

    private val mutex = Mutex()
    private val stored = linkedMapOf<String, FocusSessionDto>()

    init {
        seed(clock.instant())
    }

    override suspend fun createSession(session: FocusSessionDto): FocusSessionDto = respond {
        stored[session.id] = session
        session
    }

    override suspend fun getSessions(): List<FocusSessionDto> = respond {
        stored.values.sortedByDescending { it.startedAt }
    }

    override suspend fun getSession(id: String): FocusSessionDto = respond {
        stored[id] ?: throw notFound(id)
    }

    /**
     * Every seeded session is finished. An `ended_at` of null here would be indistinguishable from
     * a session running on another device, and `refreshSessions` upserts it straight into the local
     * store — which would hand the app a second active session and defeat the one-active-session
     * guard in `DefaultFocusSessionRepository.startSession`.
     */
    private fun seed(now: Instant) {
        val yesterday = now.minus(Duration.ofDays(1))
        stored += session(
            id = "seed-focused",
            startedAt = yesterday,
            duration = Duration.ofMinutes(25),
            distractions = listOf(
                distraction("seed-focused-1", "NOISE", yesterday.plusSeconds(240), 0.42f),
            ),
        )
        stored += session(
            id = "seed-noisy",
            startedAt = yesterday.plus(Duration.ofHours(3)),
            duration = Duration.ofMinutes(12),
            distractions = listOf(
                distraction("seed-noisy-1", "NOISE", yesterday.plus(Duration.ofHours(3)).plusSeconds(30), 0.81f),
                distraction("seed-noisy-2", "MOVEMENT", yesterday.plus(Duration.ofHours(3)).plusSeconds(95), 0.64f),
                distraction("seed-noisy-3", "NOISE", yesterday.plus(Duration.ofHours(3)).plusSeconds(410), 0.73f),
            ),
        )
    }

    private fun session(
        id: String,
        startedAt: Instant,
        duration: Duration,
        distractions: List<DistractionEventDto>,
    ): Pair<String, FocusSessionDto> = id to FocusSessionDto(
        id = id,
        startedAt = startedAt.toString(),
        endedAt = startedAt.plus(duration).toString(),
        distractions = distractions,
    )

    private fun distraction(id: String, type: String, at: Instant, intensity: Float) =
        DistractionEventDto(id = id, type = type, occurredAt = at.toString(), intensity = intensity)

    private suspend fun <T> respond(block: () -> T): T {
        delay(LATENCY.toMillis())
        return mutex.withLock { block() }
    }

    private fun notFound(id: String): HttpException = HttpException(
        Response.error<FocusSessionDto>(
            404,
            """{"error":"session $id not found"}"""
                .toResponseBody("application/json".toMediaType()),
        ),
    )

    private companion object {
        val LATENCY: Duration = Duration.ofMillis(600)
    }
}
