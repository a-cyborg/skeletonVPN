package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.util.Log
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class SkeletonVpnConnection(
    private val sVpnService: SkeletonVpnService,
    private val connectionId: Int,
    private val serverAddress: String,
    private val serverPort: Int,
    private val sharedSecret: String,
) : Runnable {
    private val TAG = this@SkeletonVpnConnection::class.java.simpleName

    private lateinit var connectionListener:  ConnectionListener

    interface ConnectionListener {
        fun onEstablish(tunInterface: ParcelFileDescriptor)
    }

    fun setConnectionOnEstablishListener(onEstablish: (ParcelFileDescriptor) -> Unit) {
       this.connectionListener = object : ConnectionListener {
           override fun onEstablish(tunInterface: ParcelFileDescriptor) {
               onEstablish(tunInterface)
           }
       }
    }

    override fun run() {
        Log.i(TAG, "run: Starting new connection [ $connectionId ]")
        try {
            Log.i(TAG, "run: Connect to server [$serverAddress : $serverPort]")
            runConnection()
        } catch (exception: InterruptedException) {
            Log.i(TAG, "run: Thread is interrupted, closing [$connectionId]")
        }
    }

    private fun runConnection() {
        var tunInterface: ParcelFileDescriptor? = null
        var tunnel : DatagramChannel? = null
        var processor : VpnProcessor? = null

        try {
            tunnel = getTunnel()
            tunInterface = handshake(tunnel)

            processor = VpnProcessor(tunnel, tunInterface)
            processor.run()
        } finally {
            processor?.tun?.close()
            processor?.tunnel?.disconnect()
            processor = null
            tunInterface?.close()
            tunnel?.disconnect()
        }
    }

    private fun getTunnel() : DatagramChannel {
        val tunnel = DatagramChannel.open()

        if (!sVpnService.protect(tunnel.socket())) {
            throw IllegalStateException("Cannot protect the tunnel")
        }

        tunnel.configureBlocking(false) // non-blocking mode
        tunnel.connect(InetSocketAddress(serverAddress, serverPort))

        return tunnel
    }

    private fun handshake(tunnel: DatagramChannel) : ParcelFileDescriptor {
        // To keep things simple in this demo, We just send the shared secret in plaintext
        // and wait for the server to send the parameters.

        val packet = ByteBuffer.allocate(1024)
        packet.put(sharedSecret.toByteArray()).flip()

        // Send the packet 3 times in case of packet loss.
        //for (i in 1..3) {
        packet.position(0)
        tunnel.write(packet)
        // }

        packet.clear()

        return configure("-")
    }

    private fun configure(parameters: String): ParcelFileDescriptor {
        // Tun interface builder.
        /*
        val builder = sVpnService.Builder()

        for(parameter in parameters.split(",")) {
            val field = parameter.split(":")

            when (field[0].first().toString()) {
                "a" -> builder.addAddress(field[1], field[2].toInt())
                "m" -> builder.setMtu(field[1].toInt())
                "r" -> builder.addRoute(field[1], field[2].toInt())
                "d" -> builder.addDnsServer(field[1])
            }
        }

        builder.setSession(serverAddress)
        builder.setConfigureIntent(sVpnService.configurePendingIntent)

         */

        val tunBuilder = sVpnService.Builder().run {
            addAddress("10.0.0.2", 32)
            addRoute("0.0.0.0", 0)
            // addRoute(VPN_ROUTE_V6, 0)
            setMtu(1400)
            setSession(TAG)
            // setConfigureIntent(sVpnService.configurePendingIntent)
        }

        tunBuilder.establish()?.let {
            Log.i(TAG, "configure: Established new tun interface : $it")
            synchronized(sVpnService) { connectionListener.onEstablish(it)}

            return it
        }
        throw IllegalStateException("Failed to establish a tun interface.")
    }
}