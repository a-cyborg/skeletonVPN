package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.provider.Settings.System.getString
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.DatagramChannel
import java.nio.charset.Charset

/* Dev mode variables */
const val SERVER = "192.168.45.244"
const val PORT = 8000
const val SHARED_SECRET = "IXV=R"

// IpV4
const val VPN_ADDRESS = "10.0.0.2"
const val VPN_ROUTE = "0.0.0.0"
const val VPN_DNS = "8.8.8.8"
const val VPN_MTU = 1400
// IpV6
const val VPN_ADDRESS_V6 = "2001:db8::1"
const val VPN_ROUTE_V6 = "::" // Intercept all

class SkeletonVpnConnection(
    private val sVpnService: SkeletonVpnService,
    private val connectionId: Int,
    private val serverAddress: String,
    private val port: Int,
    private val sharedSecret: String,
) : Runnable {
    private val TAG = this@SkeletonVpnConnection::class.java.simpleName

    // The size of packet is constrained by the MTU.
    private val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()

    lateinit var connectionListener:  ConnectionListener

    interface ConnectionListener {
        fun onEstablish(tunInterface: ParcelFileDescriptor)
    }

    fun setOnConnectionListener(listener: (ParcelFileDescriptor) -> Unit) {
       this.connectionListener = object : ConnectionListener {
           override fun onEstablish(tunInterface: ParcelFileDescriptor) {
               listener(tunInterface)
           }
       }
    }

    override fun run() {
        Log.i(TAG, "run: Starting new connection. [ $connectionId ]")

        try {
            // Try 3 times to connect vpn server.
            for (i in 1..3) {
                Log.d(TAG, "run: MAX_Packet_size = $MAX_PACKET_SIZE")
                run(InetSocketAddress(SERVER, PORT))
            }
            // run(InetSocketAddress(serverAddress, port))
        } catch (e: Exception) {
            when (e) {
                is ClosedByInterruptException, is InterruptedException ->
                    Log.i(TAG, "run: Thread interrupted, closing [$connectionId].")
                else -> Log.e(TAG, "run: Connection failed, exiting.", e)
            }
        }
    }

    private fun run(server: InetSocketAddress) {
        var tunInterface: ParcelFileDescriptor

        // Create vpn tunnel.
        DatagramChannel.open().run {
            // Protect the tunnel before connecting.
            if (!sVpnService.protect(socket())) {
                throw IllegalStateException("Cannot protect the tunnel")
            }

            // Put the tunnel into non-blocking mode.
            configureBlocking(false)

            // Connect to the server.
            connect(server)

            tunInterface = handshake(this)

            // Packets to be sent are queued in this input stream.
            val inputStream  = FileInputStream(tunInterface.fileDescriptor).channel
            // Packets received need to be written to this output stream.
            val outputStream = FileOutputStream(tunInterface.fileDescriptor).channel

            // Allocate the buffer for a single packet.
            var packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

            // Forwarding packets
            while (true) {
                // Read the outgoing packet from the input stream.
                if (inputStream.read(packet) > 0) {
                    // Write the outgoing packet to the tunnel.
                    write(packet)
                    packet.clear()
                }
            }
        }
    }

    private fun handshake(tunnel: DatagramChannel) : ParcelFileDescriptor {
        // Allocate the buffer for handshaking.
        var packet = ByteBuffer.allocate(1024)

        // Put our secret to authenticate.
        packet.put(SHARED_SECRET.toByteArray()).flip()

        // Send the packet 3 times in case of packet loss.
        for (i in 1..3) {
            packet.position(0)
            tunnel.write(packet)
        }

        packet.clear()

        // Wait for the response.
        for (i in 1..8) {
            // As we use the tunnel in non-blocking mode.
            Thread.sleep(300)

            // Normally we should not receive random packets. but let's do this time.
            if (tunnel.read(packet) > 0) {
                return configure(String(packet.array(), 0, packet.array().indexOf(0)))
            }
        }

        throw IOException("Failed to handshake with server")
    }

    private fun configure(configParam: String): ParcelFileDescriptor {
        sVpnService.Builder()
            .addAddress(VPN_ADDRESS_V6, 64)
            .addRoute(VPN_ROUTE, 0)
            .setSession(TAG)
            .establish()?.let {
                synchronized(sVpnService) { connectionListener.onEstablish(it)}
                return it
            }
        throw IllegalStateException("Failed to create tun interface")
    }
}

