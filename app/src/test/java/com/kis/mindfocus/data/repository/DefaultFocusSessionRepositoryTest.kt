package com.kis.mindfocus.data.repository

import com.kis.mindfocus.core.error.DataError
import com.kis.mindfocus.core.util.IdGenerator
import com.kis.mindfocus.data.local.dao.FocusSessionDao
import com.kis.mindfocus.data.local.entity.FocusSessionEntity
import com.kis.mindfocus.data.local.entity.SessionWithDistractions
import com.kis.mindfocus.data.remote.api.FocusSessionApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The repository is the error boundary — everything above it sees `DataError` and nothing else. A
 * mocked API could only ever return the exceptions the test already decided to throw, so this runs
 * the real Retrofit client against a real server and lets OkHttp produce the failures.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFocusSessionRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: DefaultFocusSessionRepository

    private val dao = mockk<FocusSessionDao>(relaxed = true)
    private val clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FocusSessionApi::class.java)

        repository = DefaultFocusSessionRepository(
            dao = dao,
            api = api,
            clock = clock,
            idGenerator = IdGenerator { NEW_ID },
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `refreshSessions stores what the API returned and marks it synced`() = runTest {
        server.enqueue(MockResponse().setBody(SESSIONS_JSON))
        val stored = slot<List<SessionWithDistractions>>()
        coEvery { dao.upsertSessionsWithDistractions(capture(stored)) } returns Unit

        val result = repository.refreshSessions()

        assertTrue(result.isSuccess)
        // Anything that came from the server is by definition already on the server.
        assertTrue(stored.captured.single().session.isSynced)
        assertEquals("abc-123", stored.captured.single().session.id)
        assertEquals(1, stored.captured.single().distractions.size)
    }

    @Test
    fun `a server error becomes DataError_Server carrying the code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"error":"down"}"""))

        val error = repository.refreshSessions().exceptionOrNull()

        assertEquals(503, (error as DataError.Server).code)
    }

    @Test
    fun `a dropped connection becomes DataError_Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        val error = repository.refreshSessions().exceptionOrNull()

        assertTrue(error is DataError.Network)
    }

    @Test
    fun `syncSession pushes the local session and marks it synced`() = runTest {
        coEvery { dao.getSession(SESSION_ID) } returns storedSession(SESSION_ID)
        server.enqueue(MockResponse().setBody(SINGLE_SESSION_JSON))

        val result = repository.syncSession(SESSION_ID)

        assertTrue(result.isSuccess)
        assertEquals("/api/sessions", server.takeRequest().path)
        coVerify { dao.markSynced(SESSION_ID, isSynced = true) }
    }

    /** A failed push leaves the row unsynced so the retry action still has something to retry. */
    @Test
    fun `a rejected push leaves the session unsynced`() = runTest {
        coEvery { dao.getSession(SESSION_ID) } returns storedSession(SESSION_ID)
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.syncSession(SESSION_ID)

        assertTrue(result.exceptionOrNull() is DataError.Server)
        coVerify(exactly = 0) { dao.markSynced(any(), any()) }
    }

    @Test
    fun `syncing an unknown session fails locally without calling the API`() = runTest {
        coEvery { dao.getSession(SESSION_ID) } returns null

        val error = repository.syncSession(SESSION_ID).exceptionOrNull()

        assertEquals(SESSION_ID, (error as DataError.NotFound).id)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `startSession inserts when nothing is active`() = runTest {
        coEvery { dao.insertSessionIfNoneActive(any()) } answers {
            firstArg<FocusSessionEntity>().id
        }

        val session = repository.startSession().getOrThrow()

        assertEquals(NEW_ID, session.id)
        assertEquals(clock.instant(), session.startedAt)
        assertTrue(session.isActive)
    }

    /** The double-tap case: the guard reports an existing session, so that one comes back instead. */
    @Test
    fun `startSession returns the running session instead of a second one`() = runTest {
        coEvery { dao.insertSessionIfNoneActive(any()) } returns SESSION_ID
        coEvery { dao.getSession(SESSION_ID) } returns storedSession(SESSION_ID, endedAt = null)

        val session = repository.startSession().getOrThrow()

        assertEquals(SESSION_ID, session.id)
        assertTrue(session.isActive)
    }

    private fun storedSession(id: String, endedAt: Long? = 1_700_000_100_000): SessionWithDistractions =
        SessionWithDistractions(
            session = FocusSessionEntity(
                id = id,
                startedAtEpochMillis = 1_700_000_000_000,
                endedAtEpochMillis = endedAt,
                isSynced = false,
            ),
            distractions = emptyList(),
        )

    private companion object {
        const val SESSION_ID = "session-1"
        const val NEW_ID = "generated-id"

        const val SESSIONS_JSON = """
            [
              {
                "id": "abc-123",
                "started_at": "2026-07-22T09:00:00Z",
                "ended_at": "2026-07-22T09:25:00Z",
                "distractions": [
                  {
                    "id": "evt-1",
                    "type": "NOISE",
                    "occurred_at": "2026-07-22T09:04:00Z",
                    "intensity": 0.42
                  }
                ]
              }
            ]
        """

        const val SINGLE_SESSION_JSON = """
            {
              "id": "session-1",
              "started_at": "2026-07-22T09:00:00Z",
              "ended_at": "2026-07-22T09:25:00Z",
              "distractions": []
            }
        """
    }
}
