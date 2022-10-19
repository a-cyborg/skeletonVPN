package org.cyb.skeletonvpn

import android.os.ParcelFileDescriptor
import android.util.Log

class SkeletonVpnConnection(
    val sVpnService: SkeletonVpnService,
) : Runnable{
    private val TAG = this@SkeletonVpnConnection::class.java.simpleName

    lateinit var connectionListener:  ConnectionListener

    interface ConnectionListener {
        fun onEstablish(tunInterface: ParcelFileDescriptor)
    }

    fun setOnConnectionListener(listener: (ParcelFileDescriptor) -> Unit) {
       this.connectionListener = object : ConnectionListener {
           override fun onEstablish(tunInterface: ParcelFileDescriptor) {
               Log.i(TAG, "onEstablish: Called tunInterface = " + tunInterface)
               listener(tunInterface)
           }
       }
    }

    override fun run() {
        Log.d(TAG, "run: thread = " + Thread.currentThread())
        configureTunInterface()

        while (true) {
            try {
                Thread.sleep(3000)
                Log.d(TAG, "run: I'm running...")
            } catch (e: InterruptedException) {
                return
            }
        }
    }

    private fun configureTunInterface() {
        val vpnInterface = sVpnService.Builder()
            .addAddress(VPN_ADDRESS_V6, 64)
            .addRoute(VPN_ROUTE, 0)
            .setSession(TAG)
            .establish()

        synchronized(sVpnService) {
            if (connectionListener != null && vpnInterface != null) {
                connectionListener.onEstablish(vpnInterface)
            }
        }
    }
}
/* Local tun interface address*/
// IpV4
const val VPN_ADDRESS = "10.0.0.2"
const val VPN_ROUTE = "0.0.0.0"
const val VPN_DNS = "8.8.8.8"
// IpV6
const val VPN_ADDRESS_V6 = "2001:db8::1"
const val VPN_ROUTE_V6 = "::" // Intercept all
