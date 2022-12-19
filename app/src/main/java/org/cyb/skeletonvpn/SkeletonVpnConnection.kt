package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.util.Log
import org.cyb.skeletonvpn.util.ServerConfig
import org.cyb.skeletonvpn.util.ToyVpnServerUtils
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeoutException

class SkeletonVpnConnection(
    private val sVpnService: SkeletonVpnService,
    private val connectionId: Int,
    private val serverAddress: String,
    private val serverPort: Int,
    private val sharedSecret: String,
) : Runnable {
    private val TAG = this@SkeletonVpnConnection::class.java.simpleName

    private lateinit var connectionEstablishListener: ConnectionEstablishListener

    interface ConnectionEstablishListener {
        fun onEstablish(tunInterface: ParcelFileDescriptor)
    }

    fun setConnectionEstablishListener(onEstablish: (ParcelFileDescriptor) -> Unit) {
        this.connectionEstablishListener = object : ConnectionEstablishListener {
            override fun onEstablish(tunInterface: ParcelFileDescriptor) {
                onEstablish(tunInterface)
            }
        }
    }

    override fun run() {
        Log.i(TAG, "run: Starting new connection [$connectionId]")
        try {
            runConnection()
        } catch (e: InterruptedException) {
            Log.i(TAG, "run: Thread is interrupted, closing [$connectionId]")
        } catch (e: TimeoutException) {
            Log.d(TAG, "run: handle handshake fail. [${e.message}]")
        }
    }

    private fun runConnection() {
        var tunInterface: ParcelFileDescriptor? = null
        var tunnel: DatagramChannel? = null

        try {
            tunnel = getTunnel()

            val serverConfig = ToyVpnServerUtils().handshake(tunnel, sharedSecret)
            tunInterface = getTunInterface(serverConfig)

            VpnProcessor(connectionId).run(tunnel, tunInterface)
        } finally {
            tunInterface?.close()
            tunnel?.disconnect()
        }
    }

    private fun getTunnel(): DatagramChannel {
        DatagramChannel.open().run {
            if (!sVpnService.protect(socket())) {
                throw IllegalStateException("Failed to protect the tunnel")
            }

            configureBlocking(false)  // non-blocking mode.

            Log.i(TAG, "getTunnel: Connect to server [$serverAddress : $serverPort]")
            return connect(InetSocketAddress(serverAddress, serverPort))
        }
    }

    private fun getTunInterface(serverConfig: ServerConfig): ParcelFileDescriptor {
        Log.d(TAG, "getTunInterface: serverConfig = [$serverConfig]")
        val tunBuilder = sVpnService.Builder().run {
            addAddress(serverConfig.Address.first, serverConfig.Address.second)
            addRoute(serverConfig.Route.first, serverConfig.Route.second)
            setMtu(serverConfig.MTU)
            setSession(TAG)
        }

        tunBuilder.establish()?.let {
            Log.i(TAG, "configure: Established new tun interface : $it")
            synchronized(sVpnService) { connectionEstablishListener.onEstablish(it) }

            return it
        }
        throw IllegalStateException("Failed to establish a tun interface.")
    }
}