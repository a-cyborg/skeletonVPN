package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import org.cyb.skeletonvpn.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val connectionId = AtomicInteger(1)
    private val atomicThreadRef = AtomicReference<Thread>()
    private val atomicThreadAndTunInterfaceRef = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

    companion object {
        const val CONNECT_ACTION = "SKELETON_VPN_CONNECT"
        const val DISCONNECT_ACTION = "SKELETON_VPN_DISCONNECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (val action = intent?.action) {
            CONNECT_ACTION -> { connect(); START_STICKY }
            DISCONNECT_ACTION -> { disconnect(); START_NOT_STICKY }
            else -> { receivedUnknownAction(action) }
        }
    }

    private fun receivedUnknownAction(action: String?): Int {
        // TODO: implement solution for the unknown action.
        Log.e(TAG, "receivedUnknownAction: received action = [$action]")
        return START_FLAG_RETRY
    }

    private fun connect() {
        // Become a foreground service.
        startForeground(NOTIFICATION_ID, getNotification(R.string.connecting, this))

        val connection =initConnectionRunnable()

        val connectionThread = Thread(connection, "SkeletonVpnConnectionThread")
        atomicThreadRef.getAndSet(connectionThread)?.interrupt()

        connection.setOnEstablishListener { tunInterface ->
            updateNotification(R.string.connected, this)
            atomicThreadRef.compareAndSet(connectionThread, null)
            setAtomicThreadAndTunPairReference(Pair(connectionThread, tunInterface))
        }

        connectionThread.start()
    }

    private fun initConnectionRunnable() : SkeletonVpnConnection {
        // TODO: implements validation check if wrong value is returned then throw critical Exception.
        val serverInfo = getServerInfoFromSharedPreferences(this)

        return SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            serverInfo.serverAddr,
            serverInfo.serverPort.toInt(),
            serverInfo.sharedSecret,
        )
    }

    private fun setAtomicThreadAndTunPairReference (newPair: Pair<Thread, ParcelFileDescriptor>?) {
        atomicThreadAndTunInterfaceRef.getAndSet(newPair)?.let {
            it.first.interrupt()
            it.second.close()
        }
    }

    override fun onDestroy() {
        disconnect()
    }

    private fun disconnect() {
        atomicThreadRef.set(null)
        setAtomicThreadAndTunPairReference(null)
        stopForeground(true)
        stopSelf()
    }
}

/*
Thread(connection, "SkeletonVpnConnectionThread").run {
    setAtomicThreadReference(this)

    connection.setOnConnectionListener { tunInterface ->
        updateNotification(R.string.connected, this@SkeletonVpnService)
        atomicThreadRef.compareAndSet(this, null)
        setAtomicThreadAndTunPairReference(Pair(this, tunInterface))
    }
    start()
}
 */
