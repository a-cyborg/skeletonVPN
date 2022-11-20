package org.cyb.skeletonvpn.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.cyb.skeletonvpn.MainActivity

class NotificationHelper (
    val context: Context
) {
    val NOTIFICATION_CHANNEL_ID = "Skeleton_Vpn"

    fun getNotification(msg: Int) : Notification {
        return getNotificationBuilder()
            .setContentText(context.getString(msg))
            .build()
    }

    private fun getNotificationBuilder() : NotificationCompat.Builder {
        return NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("\uD83D\uDC80")
            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
            .setContentIntent(getPendingIntent())
    }

    private fun getPendingIntent() : PendingIntent {
        return PendingIntent
            .getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
    }
}




