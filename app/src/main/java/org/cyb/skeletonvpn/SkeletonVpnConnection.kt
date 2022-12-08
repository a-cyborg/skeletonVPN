package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

// IpV6
const val VPN_ADDRESS_V6 = "2001:db8::1"
const val VPN_ROUTE_V4 = "0.0.0.0"
const val VPN_ROUTE_V6 = "::"

class SkeletonVpnConnection(
    private val sVpnService: SkeletonVpnService,
    private val connectionId: Int,
    private val serverAddress: String,
    private val serverPort: Int,
    private val sharedSecret: String,
) : Runnable {
    private val TAG = this@SkeletonVpnConnection::class.java.simpleName

    // The size of packet is constrained by the MTU.
    private val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()
    private val MAX_HANDSHAKE_ATTEMPS = 30

    private lateinit var connectionListener:  ConnectionListener

    interface ConnectionListener {
        fun onEstablish(tunInterface: ParcelFileDescriptor)
    }

    fun setOnEstablishListener(listener: (ParcelFileDescriptor) -> Unit) {
       this.connectionListener = object : ConnectionListener {
           override fun onEstablish(tunInterface: ParcelFileDescriptor) {
               listener(tunInterface)
           }
       }
    }

    override fun run() {
        Log.i(TAG, "run: Starting new connection. [ $connectionId ]")

        try {
            // If anything needs to be obtained using the network, get it now.
            // In this demo, all we need to know is the server address.
            Log.d(TAG, "run: Try to connect server addr = $serverAddress port = $serverPort")
            run(InetSocketAddress(serverAddress, serverPort))
        } catch (e: Exception) {
            when (e) {
                is ClosedByInterruptException, is InterruptedException -> {
                    Log.i(TAG, "run: Thread interrupted, closing [$connectionId].")
                } else -> {
                    Log.e(TAG, "run: Connection failed, exiting.", e)
                }
            }
        }
    }

    private fun run(server: InetSocketAddress) {
        var tunInterface: ParcelFileDescriptor? = null
        var tunnel : DatagramChannel? = null

        try {
            tunnel = DatagramChannel.open()
            // Protect the tunnel before connecting.
            if (!sVpnService.protect(tunnel.socket())) {
                throw IllegalStateException("Cannot protect the tunnel")
            }

            tunnel.configureBlocking(false)
            tunnel.connect(server)

            tunInterface = handshake(tunnel)

            // Packets to be sent are queued in this input stream.
            val inputStream  = FileInputStream(tunInterface.fileDescriptor).channel
            // Packets received need to be written to this output stream.
            val outputStream = FileOutputStream(tunInterface.fileDescriptor)

            // Allocate the buffer for a single packet.
            val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

            var lastReceiveTime = System.currentTimeMillis()
            var lastSendTime = System.currentTimeMillis()

            // Forwarding packets
            while (true) {
                var idle = true

                // Read the outgoing packet from the input stream.
                inputStream.read(packet).let {
                    if (it > 0) {
                        Log.d(TAG, "run: [OUT] android --- Tunnel ---> Vpn = $packet")
                        Log.d(TAG, "run: [OUT] length = $it")
                        packet.flip()
                        tunnel.write(packet)
                        packet.clear()

                        // There might be more incoming packets.
                        idle = false
                        lastReceiveTime = System.currentTimeMillis()
                    }
                }

                // Read the incoming packet from the tunnel.
                tunnel.read(packet).let {
                    if (it > 0) {
                        if (it == 1) {
                            // Ignore control messages from SkelServer.
                            packet.clear()
                        } else {
                            Log.d(TAG, "run: [IN] Vpn ----Tunnel ---> Android = $packet.")
                            Log.d(TAG, "run: [IN] length = $it")

                            outputStream.write(packet.array(), 0, it)

                            packet.clear()
                        }
                    }
                    idle = false
                    lastSendTime = System.currentTimeMillis()
                }

                // If we are idle, sleep for a fraction of a time
                // to avoid busy looping.
                val TEMP_INTERVAL_TIME_MS = TimeUnit.SECONDS.toMillis(15)
                // TODO: Blocking read on another thread.
                if (idle) {
                    Thread.sleep(100)
                    val timeNow = System.currentTimeMillis()

                    // We are receiving for a long time but not sending.
                    if (lastReceiveTime + TEMP_INTERVAL_TIME_MS >= timeNow) {
                        packet.put(0).limit(1)
                        tunnel.write(packet)
                        packet.clear()
                        lastSendTime = System.currentTimeMillis()
                    } else if (lastSendTime + TEMP_INTERVAL_TIME_MS >= timeNow) {
                        // We are seding for a long time but not receiving.
                        throw IllegalStateException("Time out")
                    }
                }
            }
        } finally {
            Log.d(TAG, "run: Finally block called ð")
            tunInterface?.run { close() }
            tunnel?.run { close() }
        }
    }

    private fun handshake(tunnel: DatagramChannel) : ParcelFileDescriptor {
        // To keep things simple in this demo, We just send the shared secret in plaintext
        // and wait for the server to send the parameters.

        // Allocate the buffer for handshaking.
        val packet = ByteBuffer.allocate(1024)

        // Put our secret to authenticate.
        packet.put(sharedSecret.toByteArray()).flip()

        // Send the packet 3 times in case of packet loss.
        //for (i in 1..3) {
            Log.d(TAG, "handshake: Send packet to ${serverAddress} and ${tunnel.isConnected}")
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
            addRoute(VPN_ROUTE_V4, 0)
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