package org.cyb.skeletonvpn.util

import android.content.Context
import android.net.InetAddresses
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

data class UserInput(val serverAddr: String, val serverPort: String, val sharedSecret: String) {
    init {
        serverAddr.removeWhiteSpace()
        serverPort.removeWhiteSpace()
    }
}

fun UserInput.isValidNetworkAddress(): Boolean {
    return isValidIpAddress(serverAddr) && isValidPortNumber(serverPort)
}

fun UserInput.saveToSharedPreferences(context: Context) {
    with (context.getSharedPreferences(Prefs.NAME.key, AppCompatActivity.MODE_PRIVATE).edit()) {
        putString(Prefs.SERVER_ADDRESS.key, serverAddr)
        putString(Prefs.SERVER_PORT.key, serverPort)
        putString(Prefs.SHARED_SECRET.key, sharedSecret)
        commit()
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
