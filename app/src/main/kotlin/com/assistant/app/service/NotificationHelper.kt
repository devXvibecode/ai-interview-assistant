package com.assistant.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.assistant.app.MainActivity
import com.assistant.app.R
import com.assistant.app.utils.Constants

class NotificationHelper(private val context: Context) {

    fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(Constants.ACTION_STOP_OVERLAY).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_OVERLAY)
            .setContentTitle("AI Assistant")
            .setContentText("Listening overlay active • Double-tap bubble to stop")
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .build()
    }
}