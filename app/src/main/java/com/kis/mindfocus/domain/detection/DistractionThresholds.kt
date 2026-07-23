package com.kis.mindfocus.domain.detection

import com.kis.mindfocus.core.model.DistractionType

/**
 * Where "background" stops and "distraction" starts.
 *
 * The defaults are hand-calibrated against a quiet room and a phone resting on a desk, not derived
 * from anything — they are a parameter of the class precisely so they can be tuned, or later
 * learned per user, without touching the detector.
 */
data class DistractionThresholds(
    /** Normalised peak amplitude. Quiet room ≈ 0.05, conversation ≈ 0.3, door slam ≈ 0.8. */
    val noiseAmplitude: Float = 0.3f,
    /** m/s² of linear acceleration. Desk taps ≈ 1, picking the phone up ≈ 3, walking ≈ 5. */
    val movementAcceleration: Float = 2.5f,
) {
    fun of(type: DistractionType): Float = when (type) {
        DistractionType.NOISE -> noiseAmplitude
        DistractionType.MOVEMENT -> movementAcceleration
    }
}
