package org.cyb.skeletonvpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val sConnectionThread = AtomicReference<Thread>()
    private val sConnection = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

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

    private fun connect() {
        // Become a foreground service.
        updateForegroundNotification(R.string.connecting)

        val connection = SkeletonVpnConnection(this)
        val thread = Thread(connection, "SkeletonVpnThread")
        setConnectingThread(thread)

        connection.setOnConnectionListener { tunInterface ->
            // Vpn tunnel is established.
            updateForegroundNotification(R.string.connected)

            sConnectionThread.compareAndSet(thread, null)
            setConnection(Pair(thread, tunInterface))
        }
        thread.start()
    }

    private fun setConnectingThread(thr: Thread?) {
        // Replace any existing connection thread with the new one.
        sConnectionThread.getAndSet(thr)?.interrupt()
    }

    private fun setConnection(connection: Pair<Thread, ParcelFileDescriptor>?) {
        val oldConnection = sConnection.getAndSet(connection)
        if (oldConnection != null) {
            try {
                Log.i(TAG, "setConnection: Closing vpn connection")
                oldConnection.first.interrupt()
                oldConnection.second.close()
            } catch (e: IOException) {
                Log.e(TAG, "setConnection: Closing tun interface", e)
            }
        }
    }

    override fun onDestroy() {
        disconnect()
    }

    private fun disconnect() {
        setConnectingThread(null)
        setConnection(null)
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

        startForeground(333, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("\uD83D\uDC80")
            .setContentText(getString(msg))
            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
            .setContentIntent(pendintIntent)
            .build())
    }
}