package com.kis.mindfocus.domain.sensor

import kotlinx.coroutines.flow.Flow

interface NoiseSource {
    val readings: Flow<Float>
}
