package org.cyb.skeletonvpn.models

import org.cyb.skeletonvpn.utils.NetworkAddressValidatorRegex
import java.util.*

data class ServerInfo(
    var serverAddress: String,
    var serverPort: String,
    var sharedSecret: String,
)

@Throws
fun ServerInfo.isValid(): Boolean {
    if (!NetworkAddressValidatorRegex.isValidIpAddress(serverAddress)) {
        throw InputMismatchException("Invalid IP address [$serverAddress].")
    } else if (!NetworkAddressValidatorRegex.isValidPort(serverPort)) {
        throw InputMismatchException("Invalid port [$serverPort].")
    } else {
        return true
    }
}