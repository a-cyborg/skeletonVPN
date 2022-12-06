package org.cyb.skeletonvpn.util

interface NetworkAddressValidator {
    fun isValid(input: String): Boolean
}

enum class RegexNetworkAddressValidator(private val regex: Regex) : NetworkAddressValidator {
    // This regex does not filter `special purpose address registry entry`
    // for example, It will return true for the address 0.123.123.123 (range in 0.0.0.0/8)
    IPv4(Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}${'$'}""")),
    IPv6(Regex("""(?:^|(?<=\s))(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))(?=\s|${'$'})""")),
    PORT(Regex("""^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}||65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])${'$'}""")),
    ;

    companion object {
        fun isValidIpAddress(input: String): Boolean  {
           return IPv4.regex.matches(input) or IPv6.regex.matches(input)
        }
    }

    override fun isValid(input: String): Boolean {
        return regex.matches(input)
    }
}
