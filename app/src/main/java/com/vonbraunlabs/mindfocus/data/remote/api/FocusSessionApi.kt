package com.vonbraunlabs.mindfocus.data.remote.api

import com.vonbraunlabs.mindfocus.data.remote.dto.FocusSessionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FocusSessionApi {

    @POST("sessions")
    suspend fun createSession(@Body session: FocusSessionDto): FocusSessionDto

    @GET("sessions")
    suspend fun getSessions(): List<FocusSessionDto>

    @GET("session/{id}")
    suspend fun getSession(@Path("id") id: String): FocusSessionDto
}
