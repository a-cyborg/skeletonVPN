package org.cyb.skeletonvpn.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.cyb.skeletonvpn.MainActivity

const val NOTIFICATION_ID = 333 // In this app, we handle one and only notification.

private const val CHANNEL_ID = "Skeleton_Vpn"
private const val REQUEST_CODE = 0

fun updateNotification(msg: Int, context: Context) {
    NotificationManagerCompat.from(context)
        .notify(NOTIFICATION_ID, getNotification(msg, context))
}

fun getNotification(msg: Int, context: Context): Notification {
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setShowWhen(false)
        .setSmallIcon(androidx.appcompat.R.drawable.abc_star_half_black_48dp)
        .setContentIntent(getContentIntent(context))
        .setContentText(context.getString(msg))
        .build()
}

// Intent to be sent when the notification is clicked.
private fun getContentIntent(context: Context): PendingIntent {
    val clientActivity = MainActivity::class.java

    return PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, clientActivity),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}