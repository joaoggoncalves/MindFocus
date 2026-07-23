package com.kis.mindfocus.data.remote.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.kis.mindfocus.data.remote.dto.DistractionEventDto
import com.kis.mindfocus.data.remote.dto.FocusSessionDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * Exercises the real Retrofit interface over a real HTTP connection. What is under test is the
 * contract nothing else checks: the endpoint paths, the `@SerialName` snake_case mapping, and the
 * lenient `Json` configuration from `networkModule` — all of which are strings and annotations that
 * compile fine when wrong.
 */
class FocusSessionApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: FocusSessionApi

    // Mirrors networkModule's Json so a parsing difference here would be a real one.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FocusSessionApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getSessions parses the list and hits the sessions path`() = runTest {
        server.enqueue(MockResponse().setBody(SESSIONS_JSON))

        val sessions = api.getSessions()

        assertEquals("/api/sessions", server.takeRequest().path)
        assertEquals(2, sessions.size)
        assertEquals("2026-07-22T09:00:00Z", sessions.first().startedAt)
        assertEquals(1, sessions.first().distractions.size)
        assertEquals("NOISE", sessions.first().distractions.single().type)
        assertEquals(0.42f, sessions.first().distractions.single().intensity, 0.001f)
    }

    /** A running session serialises `ended_at` as null; the DTO has to survive it. */
    @Test
    fun `getSessions accepts a session with a null ended_at`() = runTest {
        server.enqueue(MockResponse().setBody(SESSIONS_JSON))

        val active = api.getSessions()[1]

        assertNull(active.endedAt)
        assertEquals(emptyList<DistractionEventDto>(), active.distractions)
    }

    /**
     * The path is `session/{id}`, singular, while the list is `sessions` — easy to "fix" into
     * agreement and break the server contract, so it is pinned here.
     */
    @Test
    fun `getSession requests the singular path with the id`() = runTest {
        server.enqueue(MockResponse().setBody(SINGLE_SESSION_JSON))

        api.getSession("abc-123")

        assertEquals("/api/session/abc-123", server.takeRequest().path)
    }

    @Test
    fun `createSession posts the session as snake_case json`() = runTest {
        server.enqueue(MockResponse().setBody(SINGLE_SESSION_JSON))

        api.createSession(
            FocusSessionDto(
                id = "abc-123",
                startedAt = "2026-07-22T09:00:00Z",
                endedAt = "2026-07-22T09:25:00Z",
                distractions = emptyList(),
            ),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/sessions", request.path)

        val body = json.decodeFromString<FocusSessionDto>(request.body.readUtf8())
        assertEquals("abc-123", body.id)
        assertEquals("2026-07-22T09:25:00Z", body.endedAt)
    }

    /** A newer server adding fields must not break an older client. */
    @Test
    fun `unknown fields in the response are ignored`() = runTest {
        server.enqueue(MockResponse().setBody(SESSION_WITH_UNKNOWN_FIELD_JSON))

        assertEquals("abc-123", api.getSession("abc-123").id)
    }

    private companion object {
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
              },
              {
                "id": "def-456",
                "started_at": "2026-07-22T11:00:00Z",
                "ended_at": null
              }
            ]
        """

        const val SINGLE_SESSION_JSON = """
            {
              "id": "abc-123",
              "started_at": "2026-07-22T09:00:00Z",
              "ended_at": "2026-07-22T09:25:00Z",
              "distractions": []
            }
        """

        const val SESSION_WITH_UNKNOWN_FIELD_JSON = """
            {
              "id": "abc-123",
              "started_at": "2026-07-22T09:00:00Z",
              "ended_at": "2026-07-22T09:25:00Z",
              "productivity_score": 91,
              "distractions": []
            }
        """
    }
}
