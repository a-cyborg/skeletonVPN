package org.cyb.skeletonvpn.util

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.cyb.skeletonvpn.MainActivity

val NOTIFICATION_ID = 333 // In this app we handle one and only notification.

private val CHANNEL_ID = "Skeleton_Vpn"
private val REQUEST_CODE = 0
private val CLIENT_ACTIVITY = MainActivity::class.java

fun updateNotification(msg: Int, context: Context) {
    NotificationManagerCompat.from(context)
        .notify(NOTIFICATION_ID, getNotification(msg, context))
}

fun getNotification(msg: Int, context: Context) : Notification {
    // Intent to be sent when the notification is clicked.
    val contentIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, CLIENT_ACTIVITY),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setShowWhen(false)
        .setSmallIcon(androidx.appcompat.R.drawable.abc_star_half_black_48dp)
        .setContentIntent(contentIntent)
        .setContentText(context.getString(msg))
        .build()
}