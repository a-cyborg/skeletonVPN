package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

class VpnProcessor(
    val tunnel: DatagramChannel,
    val tun: ParcelFileDescriptor) {

    private val TAG = this::class.java.simpleName

    private val MAX_PACKET_SIZE = Short.MAX_VALUE.toInt()
    private val TEMP_INTERVAL_TIME_MS = TimeUnit.SECONDS.toMillis(15)

    // Allocate the buffer for a single packet.
    private val packet = ByteBuffer.allocate(MAX_PACKET_SIZE)

    fun run() {
        // Packets to be sent are queued in this input stream.
        val inputStream  = FileInputStream(tun.fileDescriptor).channel
        // Packets received need to be written to this output stream.
        val outputStream = FileOutputStream(tun.fileDescriptor)

        var lastReceiveTime = System.currentTimeMillis()
        var lastSendTime = System.currentTimeMillis()

        while (true) {
            var idle = true

            // Read the outgoing packet from the tun interface.
            inputStream.read(packet).let { numOfBytesRead ->
                if (numOfBytesRead > 0) {
                    writePacketToTunnel(packet, tunnel)

                    idle = false
                    lastReceiveTime = System.currentTimeMillis()
                }
            }

            // Read the incoming packet from the tunnel.
            tunnel.read(packet).let { numOfBytesRead ->
                if (numOfBytesRead > 0) {
                    writePacketToTun(packet, outputStream, numOfBytesRead)

                    idle = false
                    lastSendTime = System.currentTimeMillis()
                }
            }

            // TODO: Separate read/write thread.
            if (idle) {
                Log.d(TAG, "run: We aer idle")
                // If we are idle, sleep for a fraction of a time
                // to avoid busy looping.
                Thread.sleep(100)
                val timeNow = System.currentTimeMillis()

                // We are receiving for a long time but not sending.
                if (lastReceiveTime + TEMP_INTERVAL_TIME_MS >= timeNow) {
                    packet.put(0).limit(1)
                    tunnel.write(packet)
                    packet.clear()

                    lastSendTime = System.currentTimeMillis()
                } else if (lastSendTime + TEMP_INTERVAL_TIME_MS >= timeNow) {
                    // We are sending for a long time but not receiving.
                    throw IllegalStateException("Time out")
                }
            }
        }
    }

    private fun writePacketToTunnel(packet: ByteBuffer, tunnel: DatagramChannel) {
        with (packet) {
            flip()
            tunnel.write(this)
            clear()
        }
    }

    private fun writePacketToTun(packet: ByteBuffer, outputStream: FileOutputStream, length: Int) {
        outputStream.write(packet.array(), 0, length)
        packet.clear()
    }
}