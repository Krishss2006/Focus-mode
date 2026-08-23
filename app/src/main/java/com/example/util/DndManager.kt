package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object DndManager {

    const val CHANNEL_ID = "auto_dnd_status_channel"
    private const val NOTIFICATION_ID = 1001

    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    /**
     * Sets Do Not Disturb interruption filter.
     * @param enableDnd true for Priority DND (Enter geofence), false for All / Normal (Exit geofence)
     * @return Result message or null on failure
     */
    fun setDndMode(context: Context, enableDnd: Boolean): Pair<Boolean, String> {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return Pair(false, "NotificationManager service unavailable")

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return Pair(false, "DND access not granted. Please allow in System Settings.")
        }

        return try {
            if (enableDnd) {
                // Set to Priority Only DND when entering college
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                Pair(true, "Priority DND Enabled (College Zone)")
            } else {
                // Set to Normal (All interruptions allowed) when exiting college
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                Pair(true, "Normal Mode Restored (Exited College)")
            }
        } catch (e: SecurityException) {
            Pair(false, "Security Exception: ${e.localizedMessage}")
        } catch (e: Exception) {
            Pair(false, "Error: ${e.localizedMessage}")
        }
    }

    fun getCurrentInterruptionFilter(context: Context): Int {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    fun getFilterName(filter: Int): String {
        return when (filter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "All Notifications (Normal)"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only (DND)"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "Total Silence (DND)"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only (DND)"
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> "Unknown"
            else -> "Filter #$filter"
        }
    }

    fun isCurrentlyDnd(context: Context): Boolean {
        val filter = getCurrentInterruptionFilter(context)
        return filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    fun showStatusNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Auto DND Geofence Alerts"
            val descriptionText = "Notifications for automatic DND transitions based on college geofence"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
