package org.cyb.skeletonvpn.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

enum class Prefs (val key: String) {
    NAME("skeletonVpnPrefs"),
    SERVER_ADDRESS("serverAddress"),
    SERVER_PORT("serverPort"),
    SHARED_SECRET("sharedSecret"),
}

fun saveServerInfoToSharedPreferences(context: Context, serverInfo: ServerInfo) {
    with (context.getSharedPreferences(Prefs.NAME.key, MODE_PRIVATE).edit()) {
        putString(Prefs.SERVER_ADDRESS.key, serverInfo.serverAddr)
        putString(Prefs.SERVER_PORT.key, serverInfo.serverPort)
        putString(Prefs.SHARED_SECRET.key, serverInfo.sharedSecret)
        commit()
    }
}

fun getServerInfoFromSharedPreferences(context: Context) : ServerInfo {
    val preferences = context.getSharedPreferences(Prefs.NAME.key, MODE_PRIVATE)

    val serverAddr = preferences.getString(Prefs.SERVER_ADDRESS.key, "")
    val serverPort = preferences.getString(Prefs.SERVER_PORT.key, "")
    val sharedSecret = preferences.getString(Prefs.SHARED_SECRET.key, "")

    return ServerInfo(serverAddr!!, serverPort!!, sharedSecret!!)
}

