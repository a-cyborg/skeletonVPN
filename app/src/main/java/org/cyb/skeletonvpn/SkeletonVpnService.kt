package org.cyb.skeletonvpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import org.cyb.skeletonvpn.util.NOTIFICATION_ID
import org.cyb.skeletonvpn.util.getNotification
import org.cyb.skeletonvpn.util.getValidServerInfoFromShardPrefs
import org.cyb.skeletonvpn.util.updateNotification
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {
    private val TAG = this@SkeletonVpnService::class.java.simpleName

    private val connectionId = AtomicInteger(1)
    private val atomicThreadRef = AtomicReference<Thread>()
    private val atomicThreadAndTunInterfaceRef =
        AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

    companion object {
        const val CONNECT_ACTION: String = "SKELETON_VPN_CONNECT"
        const val DISCONNECT_ACTION: String = "SKELETON_VPN_DISCONNECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (val action = intent?.action) {
            CONNECT_ACTION -> {
                connect()
                START_STICKY
            }
            DISCONNECT_ACTION -> {
                disconnect()
                START_NOT_STICKY
            }
            else -> {
                receivedUnknownAction(action)
                START_FLAG_RETRY
            }
        }
    }

    private fun receivedUnknownAction(action: String?) {
        Log.e(TAG, "Action = [$action].")
    }

    private fun connect() {
        startForeground(NOTIFICATION_ID, getNotification(R.string.connecting, this))

        try {
            val connection = initRunnableConnection()

            with(Thread(connection, "SkeletonVpnThread")) {
                makeSureOneThread(this)

                connection.setConnectionOnEstablishListener { tunInterface ->
                    updateNotification(R.string.connected, this@SkeletonVpnService)
                    removeThisThreadFromRef(this)
                    makeSureOnePair(Pair(this, tunInterface))
                }

                start()
            }
        } catch (e: InputMismatchException) {
            Log.d(TAG, "connect: Server Info is invalid ${e.message}")
        }
    }

    @Throws
    private fun initRunnableConnection(): SkeletonVpnConnection {
        // Throw exception if saved info is invalided.
        val serverInfo = getValidServerInfoFromShardPrefs(this)

        return SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            serverInfo.serverAddr,
            serverInfo.serverPort.toInt(),
            serverInfo.sharedSecret,
        )
    }

    private fun makeSureOneThread(newThread: Thread?) {
        atomicThreadRef.getAndSet(newThread)?.interrupt()
    }

    private fun removeThisThreadFromRef(currentThread: Thread) {
        atomicThreadRef.compareAndSet(currentThread, null)
    }

    private fun makeSureOnePair(newPair: Pair<Thread, ParcelFileDescriptor>?) {
        atomicThreadAndTunInterfaceRef.getAndSet(newPair)?.let {
            it.first.interrupt()
            it.second.close()
        }
    }

    override fun onDestroy() {
        disconnect()
    }

    private fun disconnect() {
        makeSureOneThread(null)
        makeSureOnePair(null)
        stopForeground(true)
        stopSelf()
    }
}