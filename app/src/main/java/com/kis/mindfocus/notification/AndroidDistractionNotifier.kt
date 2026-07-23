package com.kis.mindfocus.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kis.mindfocus.MainActivity
import com.kis.mindfocus.R
import com.kis.mindfocus.core.model.DistractionType
import com.kis.mindfocus.domain.detection.DistractionSignal
import com.kis.mindfocus.domain.notification.DistractionNotifier

class AndroidDistractionNotifier(private val context: Context) : DistractionNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannel()
    }

    override fun notifyDistraction(signal: DistractionSignal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!notificationManager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_distraction)
            .setContentTitle(context.getString(R.string.notification_distraction_title))
            .setContentText(context.getString(signal.type.messageRes))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

        notificationManager.notify(signal.type.notificationId, notification)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val DistractionType.messageRes: Int
        get() = when (this) {
            DistractionType.NOISE -> R.string.notification_distraction_noise
            DistractionType.MOVEMENT -> R.string.notification_distraction_movement
        }

    private val DistractionType.notificationId: Int
        get() = when (this) {
            DistractionType.NOISE -> NOISE_NOTIFICATION_ID
            DistractionType.MOVEMENT -> MOVEMENT_NOTIFICATION_ID
        }

    private companion object {
        const val CHANNEL_ID = "distractions"
        const val NOISE_NOTIFICATION_ID = 1001
        const val MOVEMENT_NOTIFICATION_ID = 1002
    }
}
