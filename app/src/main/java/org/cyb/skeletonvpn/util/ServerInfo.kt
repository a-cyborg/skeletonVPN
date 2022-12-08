package org.cyb.skeletonvpn.util

import android.content.Context
import android.net.InetAddresses
import android.os.Build
import android.util.Log
import java.util.InputMismatchException

data class ServerInfo(val serverAddr: String, val serverPort: String, val sharedSecret: String) {
    init {
        serverAddr.removeWhiteSpace()
        serverPort.removeWhiteSpace()
    }
}

fun ServerInfo.ifIsValidAddressThenSaveToSharedPrefs(context: Context) {
    if (isValidNetworkAddress()) {
        saveServerInfoToSharedPreferences(context, this)
    }
}

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