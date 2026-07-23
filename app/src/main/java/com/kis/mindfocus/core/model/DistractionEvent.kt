package com.kis.mindfocus.core.model

import java.time.Instant

enum class DistractionType {
    NOISE,
    MOVEMENT,
}

/**
 * A single moment where a sensor reading crossed its distraction threshold.
 *
 * @param intensity how far past the threshold the reading was, normalised to `1.0` = exactly at
 * the threshold. Kept unit-less so the UI never has to know whether the source was decibels or m/s².
 */
data class DistractionEvent(
    val id: String,
    val type: DistractionType,
    val occurredAt: Instant,
    val intensity: Float,
)
