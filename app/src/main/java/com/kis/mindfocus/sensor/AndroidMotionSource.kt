package com.kis.mindfocus.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.kis.mindfocus.domain.sensor.MotionSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Movement via the accelerometer.
 *
 * **Battery:** registered at `SENSOR_DELAY_NORMAL` (~200ms) rather than a game/UI rate — spotting
 * that a phone was picked up needs nothing faster — and unregistered as soon as collection stops.
 * The flow is conflated so a slow collector drops stale samples instead of building a backlog.
 */
class AndroidMotionSource(private val context: Context) : MotionSource {

    override val readings: Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService<SensorManager>()
        if (sensorManager == null) {
            close()
            return@callbackFlow
        }

        // TYPE_LINEAR_ACCELERATION already removes gravity, but it is a composite sensor that not
        // every device provides; the raw accelerometer is the fallback.
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val gravityIncluded = sensor.type == Sensor.TYPE_ACCELEROMETER

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val magnitude = sqrt(x * x + y * y + z * z)
                trySendBlocking(
                    if (gravityIncluded) abs(magnitude - SensorManager.GRAVITY_EARTH) else magnitude,
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()
}

private operator fun FloatArray.component1(): Float = this[0]
private operator fun FloatArray.component2(): Float = this[1]
private operator fun FloatArray.component3(): Float = this[2]
