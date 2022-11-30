package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import org.cyb.skeletonvpn.util.getNotification
import org.cyb.skeletonvpn.util.updateNotification
import org.cyb.skeletonvpn.util.NOTIFICATION_ID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val connectionId = AtomicInteger(1)
    private val connectionRef = AtomicReference<Thread>()
    private val connectionAndTunRef = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

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
        startForeground(NOTIFICATION_ID, getNotification(R.string.connecting, this))

        // TODO: Refactoring - DevMode hard coded parameters
        val connection = SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            "192.168.45.33",
            8000,
            "justtwoofus", // shared secret.
        )

        Thread(connection, "SkeletonVpnThread").run {
            setConnectionReference(this) // Check if we have any dangled thread.

            connection.setOnConnectionListener { tunInterface ->
                updateNotification(R.string.connected, this@SkeletonVpnService)
                connectionRef.compareAndSet(this, null)
                setAtomicConnectionAndTunPair(Pair(this, tunInterface))
            }

            start()
        }
    }

    private fun setConnectionReference(newThread: Thread?) {
        connectionRef.getAndSet(newThread)?.interrupt()
    }

    private fun setAtomicConnectionAndTunPair(newPair: Pair<Thread, ParcelFileDescriptor>?) {
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
        setAtomicConnectionAndTunPair(null)
        stopForeground(true)
    }
}