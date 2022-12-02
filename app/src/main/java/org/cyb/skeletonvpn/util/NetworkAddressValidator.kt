package org.cyb.skeletonvpn.util

enum class NetworkAddressValidator(private val regex: Regex) {
    // This regex does not filter `special purpose address registry entry`
    // for example, It will return true for the address 0.123.123.123 (range in 0.0.0.0/8)
    IPv4(Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}${'$'}""")),
    Port(Regex("""^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}"""
            + """||65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])${'$'}""")),
    ;

    open fun isAcceptable(input: String): Boolean {
        return regex.matches(input)
    }
}