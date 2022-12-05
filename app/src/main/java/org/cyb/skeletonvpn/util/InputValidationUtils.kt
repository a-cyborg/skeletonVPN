package org.cyb.skeletonvpn.util

import android.net.InetAddresses
import android.os.Build

fun isAcceptableIpAddress(addr: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        InetAddresses.isNumericAddress(addr.removeWhiteSpace())
    } else {
        RegexNetworkAddressValidator.isAcceptableIpAddress(addr.removeWhiteSpace())
    }
}

fun isAcceptablePortNumber(port: String): Boolean {
    return RegexNetworkAddressValidator.PORT.isAcceptable(port.removeWhiteSpace())
}