package org.cyb.skeletonvpn.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.cyb.skeletonvpn.ui.MainActivity

const val NOTIFICATION_ID: Int = 333 // In this app, we handle one and only notification.

private const val CHANNEL_ID = "Skeleton_VPN"
private const val CHANNEL_NAME = "Skeleton VPN Channel"
private const val REQUEST_CODE = 0

fun updateNotification(msg: Int, context: Context) {
    NotificationManagerCompat
        .from(context)
        .notify(NOTIFICATION_ID, getNotification(msg, context))
}

fun getNotification(msg: Int, context: Context): Notification {
    // Intent to be sent when the notification is clicked.
    val clientActivity = MainActivity::class.java

    val pendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, clientActivity),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setShowWhen(false)
        .setSmallIcon(androidx.appcompat.R.drawable.abc_star_black_48dp)
        .setContentIntent(pendingIntent)
        .setContentText(context.getString(msg))
        .build()
}

@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.lightColor = Color.DKGRAY
    channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

    // Create notification channel
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(channel)
}