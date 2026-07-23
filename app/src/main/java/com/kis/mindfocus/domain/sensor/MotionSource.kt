package com.kis.mindfocus.domain.sensor

import kotlinx.coroutines.flow.Flow

interface MotionSource {
    val readings: Flow<Float>
}
