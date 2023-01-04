package org.cyb.skeletonvpn.vpn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Messenger

class VpnServiceStarter(private val context: Context) {

    fun prepareVpnService(): Intent? {
        return VpnService.prepare(context)
    }

    fun start(messenger: Messenger) {
        val intent = getServiceIntent()
        intent.action = SkeletonVpnService.CONNECT_ACTION
        intent.putExtra(SkeletonVpnService.MESSENGER_EXTRA_NAME, messenger)

        context.startService(intent)
    }

    fun stop() {
        val intent = getServiceIntent()
        intent.action = SkeletonVpnService.DISCONNECT_ACTION

        context.startService(intent)
    }

    private fun getServiceIntent() =
        Intent(context.applicationContext, SkeletonVpnService::class.java)

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var currentInstance: VpnServiceStarter? = null

        fun getInstance(context: Context): VpnServiceStarter {
            synchronized(this) {
                currentInstance?.let {
                    return it
                }
            }

            val newInstance = VpnServiceStarter(context)
            currentInstance = newInstance
            return newInstance
        }
    }
}