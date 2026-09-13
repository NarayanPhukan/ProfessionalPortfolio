package com.narayan.portfolioadmin.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.narayan.portfolioadmin.MainActivity
import com.narayan.portfolioadmin.R

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    const val CHANNEL_INQUIRIES = "portfolio_inquiries"
    const val CHANNEL_UPDATES = "portfolio_updates"
    const val CHANNEL_SYSTEM = "portfolio_system"

    const val NOTIFICATION_ID_TEST = 1001
    const val NOTIFICATION_ID_INQUIRY = 1002
    const val NOTIFICATION_ID_UPDATE = 1003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Client Inquiries Channel (High Priority)
            val inquiryChannel = NotificationChannel(
                CHANNEL_INQUIRIES,
                "Client Inquiries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications when visitors contact you on your web portfolio"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. App Updates Channel (High Priority)
            val updateChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when new Android app versions are ready"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. System & Diagnostics Channel
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "System & Diagnostics",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Test notifications and telemetry updates"
                enableVibration(true)
                setShowBadge(false)
            }

            manager.createNotificationChannel(inquiryChannel)
            manager.createNotificationChannel(updateChannel)
            manager.createNotificationChannel(systemChannel)
            Log.d(TAG, "Notification channels initialized")
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showTestPushNotification(context: Context) {
        val prefs = NotificationPreferences(context)
        if (!prefs.isPushEnabled) {
            Log.w(TAG, "Push notifications disabled in preferences")
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Push Notifications Active 🔔")
            .setContentText("Your Portfolio Admin is configured to receive real-time push alerts.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your Portfolio Admin push notification engine is active! You will receive instant notifications for new client inquiries, website visitor milestones, and new app updates."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(
                if (prefs.isSoundAndVibrateEnabled) {
                    NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
                } else 0
            )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        try {
            if (hasNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting notification", e)
        }
    }

    fun showInquiryNotification(context: Context, senderName: String, previewText: String) {
        val prefs = NotificationPreferences(context)
        if (!prefs.isPushEnabled || !prefs.isInquiryAlertsEnabled) return

        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_INQUIRIES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("New Client Inquiry from $senderName")
            .setContentText(previewText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("\"$previewText\"\n\nTap to open inquiries."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(
                if (prefs.isSoundAndVibrateEnabled) {
                    NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
                } else 0
            )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        try {
            if (hasNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_INQUIRY, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting inquiry notification", e)
        }
    }

    fun showUpdateNotification(context: Context, versionName: String, releaseNotes: String) {
        val prefs = NotificationPreferences(context)
        if (!prefs.isPushEnabled || !prefs.isAppUpdateAlertsEnabled) return

        val contentIntent = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Portfolio Admin v$versionName Available 🚀")
            .setContentText("A new app update is ready to install.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Version $versionName is available!\n\n$releaseNotes"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(
                if (prefs.isSoundAndVibrateEnabled) {
                    NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
                } else 0
            )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        try {
            if (hasNotificationPermission(context)) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting update notification", e)
        }
    }
}
