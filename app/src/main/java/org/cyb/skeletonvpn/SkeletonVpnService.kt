package org.cyb.skeletonvpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    companion object {
        const val ACTION_CONNECT = "SKELETON_VPN_CONNECT"
        const val ACTION_DISCONNECT = "SKELETON_VPN_DISCONNECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            START_NOT_STICKY
        } else {
            connect()
            START_STICKY
        }
    }

    override fun onDestroy() {
        disconnect()
    }

    private fun connect() {
        // Become a foreground service.
        updateForegroundNotification(R.string.connecting)
    }

    private fun disconnect() {
        stopForeground(true)
    }

    private fun updateForegroundNotification(msg: Int) {
        val NOTIFICATION_CHANNEL_ID = "Skeleton_Vpn"
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        val pendintIntent = PendingIntent.getActivity(this, 0,
            Intent(this,MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        // From Android 8.0 notification channel must be created.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        startForeground(133, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("\uD83D\uDC80")
            .setContentText(getString(msg))
            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
            .setContentIntent(pendintIntent)
            .build())
    }
}