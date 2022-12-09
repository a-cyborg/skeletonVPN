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
        when (val action = intent?.action) {
            CONNECT_ACTION -> {
                connect()
                return START_STICKY
            }
            DISCONNECT_ACTION -> {
                disconnect()
                return START_NOT_STICKY
            }
            else -> {
                receivedUnknownAction(action)
                return START_NOT_STICKY
            }
        }
    }

    private fun receivedUnknownAction(action: String?) {
        Log.e(TAG, "receivedUnknownAction: received action = [$action]")
    }

    private fun connect() {
        // Become a foreground service.
        startForeground(NOTIFICATION_ID, getNotification(R.string.connecting, this))

        try {
            val connection =initConnectionRunnable()

            with (Thread(connection, "SkeletonVpnThread")) {
                makeSureOneThread(this)

                connection.setOnEstablishListener { tunInterface ->
                    updateNotification(R.string.connected, this@SkeletonVpnService)
                    removeRunningThreadFromAtomicRef(this)
                    makeSureOnePairOfThreadAndTun(Pair(this, tunInterface))
                }

                start()
            }
        } catch (exception: Exception) {
            throw exception
        } finally {
            disconnect()
        }
    }

    @Throws
    private fun initConnectionRunnable() : SkeletonVpnConnection {
        val serverInfo = getServerInfoFromSharedPreferences(this)
        serverInfo.isValidNetworkAddress() // Throw exception if it is invalid.

        return SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            serverInfo.serverAddr,
            serverInfo.serverPort.toInt(),
            serverInfo.sharedSecret,
        )
    }

    private fun makeSureOneThread(newThread: Thread) {
       atomicThreadRef.getAndSet(newThread)?.interrupt()
    }

    private fun removeRunningThreadFromAtomicRef(runningThread: Thread) {
        atomicThreadRef.compareAndSet(runningThread, null)
    }

    private fun makeSureOnePairOfThreadAndTun (newPair: Pair<Thread, ParcelFileDescriptor>?) {
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
        makeSureOnePairOfThreadAndTun(null)
        stopForeground(true)
        stopSelf()
    }
}