package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import org.cyb.skeletonvpn.util.NotificationHelper
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val connectionId = AtomicInteger(1)
    private val connectionRef = AtomicReference<Thread>()
    private val connectionAndTunRef = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

    private var notificationHelper: NotificationHelper = NotificationHelper(this)

    companion object {
        const val CONNECT_ACTION = "SKELETON_VPN_CONNECT"
        const val DISCONNECT_ACTION = "SKELETON_VPN_DISCONNECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == DISCONNECT_ACTION) {
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

        // TODO: Refactoring - DevMode hard coded parameters
        val connection = SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            "192.168.45.33",
            8000,
            "0justtwoofus", // shared secret.
        )

        Thread(connection, "SkeletonVpnThread").run {
            setConnectionReference(this) // Check if we have any dangled thread.

            connection.setOnConnectionListener { tunInterface ->
                updateForegroundNotification(R.string.connected)
                connectionRef.compareAndSet(this, null)
                setConnectionAndTunReference(Pair(this, tunInterface))
            }

            start()
        }
    }

    private fun setConnectionReference(newThread: Thread?) {
        connectionRef.getAndSet(newThread)?.interrupt()
    }

    private fun setConnectionAndTunReference(newPair: Pair<Thread, ParcelFileDescriptor>?) {
        connectionAndTunRef.getAndSet(newPair)?.let {
            it.first.interrupt()
            it.second.close()
        }
    }

    override fun onDestroy() {
        disconnect()
    }

    private fun disconnect() {
        setConnectionReference(null)
        setConnectionAndTunReference(null)
        stopForeground(true)
    }

    private fun updateForegroundNotification(msg: Int) {
        val notification = notificationHelper.getNotification(msg)
        startForeground(333, notification)
    }
}