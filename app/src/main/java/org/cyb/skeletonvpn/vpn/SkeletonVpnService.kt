package org.cyb.skeletonvpn.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.util.Log
import org.cyb.skeletonvpn.R
import org.cyb.skeletonvpn.models.ConnectionState
import org.cyb.skeletonvpn.models.ConnectionState.*
import org.cyb.skeletonvpn.models.VpnState
import org.cyb.skeletonvpn.models.isValid
import org.cyb.skeletonvpn.utils.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SkeletonVpnService : VpnService() {

    private val connectionId = AtomicInteger(1)
    private val atomicThreadRef = AtomicReference<Thread>()
    private val atomicThreadAndTunRef = AtomicReference<Pair<Thread, ParcelFileDescriptor>>()

    private var messenger: Messenger? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (val action = intent?.action) {
            CONNECT_ACTION -> {
                setMessenger(intent)
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

    override fun onDestroy() {
        disconnect()
    }

    private fun setMessenger(intent: Intent) {
        messenger =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(MESSENGER_EXTRA_NAME, Messenger::class.java)
            } else {
                intent.getParcelableExtra(MESSENGER_EXTRA_NAME)
            }
    }

    private fun connect() {
        // Become a foreground service.
        startForeground()

        val connection = getConnection()
        if (connection != null) {
            val connectionThread = Thread(connection, "SkeletonVpnThread")
            setAtomicConnectionThread(connectionThread)

            connection.setConnectionEstablishListener { tun ->
                updateUI(CONNECTED)

                atomicThreadRef.compareAndSet(connectionThread, null)
                setAtomicConnectionPair(Pair(connectionThread, tun))
            }

            connectionThread.start()
        }
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this)
        }

        startForeground(NOTIFICATION_ID, getNotification(R.string.connecting, this))
    }

    private fun getConnection(): SkeletonVpnConnection? {
        val serverInfo = ServerInfoRepository(this).getServerInfo()

        try {
            serverInfo.isValid()
        } catch (e: InputMismatchException) {
            updateUI(ERROR, e.message.toString())
            return null
        }

        return SkeletonVpnConnection(
            this,
            connectionId.getAndIncrement(),
            serverInfo.serverAddress,
            serverInfo.serverPort.toInt(),
            serverInfo.sharedSecret,
        )
    }

    private fun setAtomicConnectionThread(thread: Thread?) {
        atomicThreadRef.getAndSet(thread)?.interrupt()
    }

    private fun setAtomicConnectionPair(newPair: Pair<Thread, ParcelFileDescriptor>?) {
        atomicThreadAndTunRef.getAndSet(newPair)?.let {
            it.first.interrupt()
            it.second.close()
        }
    }

    private fun disconnect() {
        Log.d(TAG, "disconnect: called")
        setAtomicConnectionThread(null)
        setAtomicConnectionPair(null)
        sendMsgToViewModel(DISCONNECTED)
        messenger = null
        stopForeground(true)
    }

    fun updateUI(state: ConnectionState, msg: String = "") {
        updateNotification(getBaseMessageOfState(state), this@SkeletonVpnService)
        sendMsgToViewModel(state, msg)
        vpnState = VpnState(state, msg)
    }

    private fun sendMsgToViewModel(state: ConnectionState, strMsg: String = "") {
        with(Message.obtain()) {
            what = state.code
            obj = strMsg

            messenger?.send(this)
        }
    }

    private fun receivedUnknownAction(action: String?) {
        Log.e(TAG, "receivedUnknownAction: [$action]")
    }

    companion object {
        const val CONNECT_ACTION: String = "SKELETON_CONNECT_ACTION"
        const val DISCONNECT_ACTION: String = "SKELETON_DISCONNECT_ACTION"
        const val MESSENGER_EXTRA_NAME: String = "org.cyb.skeletonvpn.message_handler"

        var vpnState: VpnState? = null
    }
}