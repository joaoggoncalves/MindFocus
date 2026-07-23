package com.kis.mindfocus.domain.notification

import com.kis.mindfocus.domain.detection.DistractionSignal

/**
 * Domain-side view of "tell the user". Keeps `NotificationManager`, channels and permission checks
 * out of the monitoring logic, so that logic stays JVM-testable.
 */
interface DistractionNotifier {
    fun notifyDistraction(signal: DistractionSignal)
}
