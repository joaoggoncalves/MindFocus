package com.kis.mindfocus.domain.sensor

import com.kis.mindfocus.core.model.DistractionType

data class SensorReading(
    val type: DistractionType,
    val value: Float,
)
