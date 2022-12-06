package org.cyb.skeletonvpn.util

import org.junit.Assert.*

import org.junit.Test

class RegexNetworkAddressValidatorTest {
    @Test
    fun iPv4_isAcceptable_should_return_true() {
        val acceptableList = listOf(
            "0.0.0.0",
            "138.199.21.219",
            "192.168.45.33",
            "8.8.8.8",
            "255.255.255.255",
        )

        acceptableList.forEach { acceptable ->
            assertTrue("${javaClass.simpleName}|IPv4_isAcceptable_should_return_true failed on $acceptable"
                ,RegexNetworkAddressValidator.IPv4.isValid(acceptable))
        }
    }

    @Test
    fun iPv4_isAcceptable_should_return_false() {
        val rejectableList = listOf(
            "-1.2.3.4",
            "1.2.3.4.5",
            "138.299.21.256",
            "1.2.",
            "xxx",
        )

        rejectableList.forEach { rejectable ->
            assertFalse("${javaClass.simpleName}|IPv4_isAcceptable_should_return_false failed on $rejectable"
                ,RegexNetworkAddressValidator.IPv4.isValid(rejectable))
        }
    }

    @Test
    fun iPv6_isAcceptable_should_return_true() {
        val acceptableList = listOf(
            // Normal
            "::",
            "::1234:5678",
            "684d:1111:222:3333:4444:5555:6:77",
            "2001:db8::",
            "2001:db8::1234:5678",
            "2001:db8:1::ab9:C0A8:102",
            "2001:db8:3333:4444:5555:6666:7777:8888",
            "2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF",
            // Dual (Only supported form of dual address type)
            "::11.22.33.44",
        )

        acceptableList.forEach { acceptable ->
            assertTrue("${javaClass.simpleName}|IPv6_isAcceptable_should_return_true Failed on $acceptable"
                ,RegexNetworkAddressValidator.IPv6.isValid(acceptable)
            )
        }
    }

    @Test
    fun iPv6_isAcceptable_should_return_false() {
        val rejectableList = listOf(
            "fe80:2030:31:24",
            "56FE::2159:5BBC::6594",
            "2001:gd8::",
            "x::x::x:::x:",
            "\\00\\x00\\x00",
            // Not supported but valid dual type address
            "2001:db8:3333:4444:5555:6666:1.2.3.4",
            "::1234:5678:91.123.4.56",
        )

        rejectableList.forEach { rejectable ->
            assertFalse("${javaClass.simpleName}|IPv6_isAcceptable_should_return_false Failed on $rejectable"
                ,RegexNetworkAddressValidator.IPv6.isValid(rejectable))
        }
    }

    @Test
    fun port_isAcceptable_should_return_true() {
        val acceptableList = listOf(
            "8080",
            "3333",
            "65535",
            "21",
        )

        acceptableList.forEach { acceptable ->
            assertTrue("${javaClass.simpleName}|Port_isAcceptable_should_return_true failed on $acceptable",
                RegexNetworkAddressValidator.PORT.isValid(acceptable))
        }
    }

    @Test
    fun port_isAcceptable_should_return_false() {
        val rejectableList = listOf(
            "00",
            "65537",
            "-123",
            "00000",
            "ppp",
        )

       rejectableList.forEach { rejectable ->
           assertFalse("${javaClass.simpleName}|Port_isAcceptable_should_return_false failed on $rejectable"
               ,RegexNetworkAddressValidator.PORT.isValid(rejectable))
       }
    }

    @Test
    fun regexNetworkAddressValidator_isAcceptableIpAddress_should_return_true() {
       val acceptableList = listOf(
           "0.0.0.0",
           "138.199.21.219",
           "192.168.45.33",
           "8.8.8.8",
           "255.255.255.255",
           "::",
           "::1234:5678",
           "684d:1111:222:3333:4444:5555:6:77",
           "2001:db8::",
           "2001:db8::1234:5678",
           "2001:db8:1::ab9:C0A8:102",
       )

        acceptableList.forEach { acceptable ->
           assertTrue("${javaClass.simpleName}|isAcceptableIpAddress_should_return_true failed on $acceptable"
               ,RegexNetworkAddressValidator.isValidIpAddress(acceptable))
        }

    }

    @Test
    fun isAcceptableIpAddress_should_return_false() {
        val rejectableList = listOf(
            "56FE::2159:5BBC::6594",
            "138.299.21.256",
            "-",
            "2001:gd8::",
            "-1.2.3.4",
            "1.2.3.4.5",
            "138.299.21.256",
            "1.2.",
            "xxx",
        )

        rejectableList.forEach { rejectable ->
           assertFalse("${javaClass.simpleName}|isAcceptableIPAddress_should_return_false failed on $rejectable"
               ,RegexNetworkAddressValidator.isValidIpAddress(rejectable))
        }
    }
}