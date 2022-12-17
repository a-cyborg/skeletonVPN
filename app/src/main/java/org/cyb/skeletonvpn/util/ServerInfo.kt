package org.cyb.skeletonvpn.util

import android.content.Context
import android.net.InetAddresses
import android.os.Build
import java.util.InputMismatchException

data class ServerInfo(
    val serverAddr: String,
    val serverPort: String,
    val sharedSecret: String,
) {
    init {
        serverAddr.removeWhiteSpace()
        serverPort.removeWhiteSpace()
    }
}

fun ServerInfo.ifValidSaveToSharedPrefs(context: Context) {
    if (isValidNetworkAddress()) {
        val prefServerInfoMap = mapOf(
            Prefs.SERVER_ADDRESS to serverAddr,
            Prefs.SERVER_PORT to serverPort,
            Prefs.SHARED_SECRET to sharedSecret,
        )

        saveStringToShardPrefs(context, prefServerInfoMap)
    }
}

fun getValidServerInfoFromShardPrefs(context: Context): ServerInfo {
    val values = getStringFromSharedPrefs(
        context,
        listOf(Prefs.SERVER_ADDRESS, Prefs.SERVER_PORT, Prefs.SHARED_SECRET)
    )

    val serverInfo = ServerInfo(values[0], values[1], values[2])
    serverInfo.isValidNetworkAddress()

    return serverInfo
}

@Throws
private fun ServerInfo.isValidNetworkAddress(): Boolean {
    if (!isValidIpAddress(serverAddr)) {
        throw InputMismatchException("Invalid IP address [$serverAddr].")
    } else if (!isValidPortNumber(serverPort)) {
        throw InputMismatchException("Invalid port [$serverPort].")
    } else {
        return true
    }
}

private fun isValidIpAddress(addr: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(addr)
    } else {
        NetworkAddressValidatorRegex.isValidIpAddress(addr)
    }
}

private fun isValidPortNumber(port: String): Boolean {
    return NetworkAddressValidatorRegex.PORT.isValid(port)
}