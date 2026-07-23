package com.kis.mindfocus.sensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.kis.mindfocus.domain.sensor.NoiseSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.abs

/**
 * Ambient noise via [AudioRecord]: read a buffer, report how loud its loudest sample was, repeat.
 *
 * Peak amplitude rather than RMS — the question is only "was that loud", and a threshold answers it
 * without averaging. Audio never leaves this loop: no buffer is retained, written to disk or sent
 * anywhere, which is also why this uses [AudioRecord] rather than `MediaRecorder.getMaxAmplitude`,
 * whose simpler API requires encoding to an output file.
 *
 * **Battery:** the microphone is open for as long as this flow is collected, which is only while a
 * session is running and the screen is on. If profiling ever shows that matters, duty-cycling
 * (stopping the recorder between samples) is the next step — it was not worth the complexity here.
 */
class AndroidNoiseSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : NoiseSource {

    override val readings: Flow<Float> = flow {
        if (!hasRecordPermission()) return@flow

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL, ENCODING)
        if (bufferSize <= 0) return@flow

        val recorder = createRecorder(bufferSize) ?: return@flow
        val buffer = ShortArray(bufferSize)

        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val samples = recorder.read(buffer, 0, buffer.size)
                if (samples > 0) emit(buffer.peakAmplitude(samples))
                delay(SAMPLE_INTERVAL_MILLIS)
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(ioDispatcher)

    /** Loudest sample in the buffer, as a fraction of full scale for 16-bit PCM. */
    private fun ShortArray.peakAmplitude(samples: Int): Float {
        var peak = 0
        for (index in 0 until samples) {
            peak = maxOf(peak, abs(this[index].toInt()))
        }
        return peak / Short.MAX_VALUE.toFloat()
    }

    /** Guarded by [hasRecordPermission] at the single call site. */
    @SuppressLint("MissingPermission")
    private fun createRecorder(bufferSize: Int): AudioRecord? {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_HZ,
            CHANNEL,
            ENCODING,
            bufferSize,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }
        return recorder
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val SAMPLE_INTERVAL_MILLIS = 500L
    }
}
