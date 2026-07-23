package com.kis.mindfocus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FocusSessionDto(
    @SerialName("id") val id: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("distractions") val distractions: List<DistractionEventDto> = emptyList(),
)

@Serializable
data class DistractionEventDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("intensity") val intensity: Float,
)
