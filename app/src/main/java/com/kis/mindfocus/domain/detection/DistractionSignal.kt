package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionType

/** A reading the detector accepted as a distraction. [intensity] is `1.0` at exactly the threshold. */
data class DistractionSignal(
    val type: DistractionType,
    val intensity: Float,
)
