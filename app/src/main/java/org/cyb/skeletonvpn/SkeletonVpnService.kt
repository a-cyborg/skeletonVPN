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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val connectionId = AtomicInteger(1)
    private val connectionThread = AtomicReference<Thread>()
    private val connection = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

    companion object {
        const val ACTION_CONNECT = "SKELETON_VPN_CONNECT"
        const val ACTION_DISCONNECT = "SKELETON_VPN_DISCONNECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == ACTION_DISCONNECT) {
            Log.d(TAG, "onStartCommand: Disconnect")
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

        val connection = SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            "", // server address.
            0, // Port.
            "Secret", // shared secret.
        )

        Thread(connection, "SkeletonVpnThread").run {
            setConnectingThread(this) // Check if we have any dangled thread.

            connection.setOnConnectionListener { tunInterface ->
                // Vpn tunnel is established.
                updateForegroundNotification(R.string.connected)
                connectionThread.compareAndSet(this, null)
                setConnection(Pair(this, tunInterface))
            }
            start()}
    }

    private fun setConnectingThread(thr: Thread?) {
        // Replace any existing connection thread with the new one.
        connectionThread.getAndSet(thr)?.interrupt()
    }

    private fun setConnection(connection: Pair<Thread, ParcelFileDescriptor>?) {
        this.connection.getAndSet(connection)?.run {
            try {
                Log.i(TAG, "setConnection: Closing tun interface [ " + second + "]")
                first.interrupt()
                second.close()
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

        // From Android 8.0 notification channel must be created.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_ID,
                        NotificationManager.IMPORTANCE_DEFAULT))
        }

        startForeground(333, NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("\uD83D\uDC80")
            .setContentText(getString(msg))
            .setSmallIcon(androidx.loader.R.drawable.notification_bg)
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                Intent(this,MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT))
            .build())
    }
}