package org.cyb.skeletonvpn.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class RegexNetworkAddressValidatorTest {
    @ParameterizedTest
    @MethodSource("validIPv4Provider")
    fun iPv4_isValid_should_return_true(valid: String) {
        assertTrue(NetworkAddressValidatorRegex.IPv4.isValid(valid),
            "\"${javaClass.simpleName}|IPv4_isAcceptable_should_return_true failed on [$valid]")
    }

    @ParameterizedTest
    @MethodSource("invalidIPv4Provider")
    fun iPv4_isAcceptable_should_return_false(invalid: String) {
        assertFalse(NetworkAddressValidatorRegex.IPv4.isValid(invalid),
            "${javaClass.simpleName}|IPv4_isAcceptable_should_return_false failed on [$invalid]")
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
            assertTrue(RegexNetworkAddressValidator.IPv6.isValid(acceptable),
                "${javaClass.simpleName}|IPv6_isAcceptable_should_return_true Failed on $acceptable")
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
            assertFalse(RegexNetworkAddressValidator.IPv6.isValid(rejectable),
                "${javaClass.simpleName}|IPv6_isAcceptable_should_return_false Failed on $rejectable")
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
            assertTrue(RegexNetworkAddressValidator.PORT.isValid(acceptable),
                "${javaClass.simpleName}|Port_isAcceptable_should_return_true failed on $acceptable")
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
           assertFalse(RegexNetworkAddressValidator.PORT.isValid(rejectable),
               "${javaClass.simpleName}|Port_isAcceptable_should_return_false failed on $rejectable")
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
           assertTrue(RegexNetworkAddressValidator.isValidIpAddress(acceptable),
               "${javaClass.simpleName}|isAcceptableIpAddress_should_return_true failed on $acceptable")
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
           assertFalse(RegexNetworkAddressValidator.isValidIpAddress(rejectable),
               "${javaClass.simpleName}|isAcceptableIPAddress_should_return_false failed on $rejectable")
        }
    }

    private fun validIPv4Provider() = Stream.of(
        "0.0.0.0",
        "0.0.0.1",
        "127.0.0.1",
        "1.2.3.4",              // 0-9
        "11.1.1.0",             // 10-99
        "101.1.1.0",            // 100-199
        "201.1.1.0",            // 200-249
        "255.255.255.255",      // 250-255
        "192.168.1.1",
        "192.168.1.255",
        "100.100.100.100",
    )

    private fun invalidIPv4Provider() = Stream.of(
        "",
        "00",
        "65537",
        "-123.23.45.22",
        "00000",
        "ppp",
        "localhost",
        "000.000.000.000",
        "00.00.00.00",
        "1.2.3.04",
        "1.02.03.4",
        "1.2",
        "1.2.3",
        "1.2.3.4.5",
        "192.168.1.1.1",
        "256.1.1.1",
        "1.256.1.1",
        "1.1.256.1",
        "1.1.1.256",
        "-100.1.1.1",
        "1.-100.1.1",
        "1.1.-100.1",
        "1.1.1.-100",
        "1...1",
        "1..1",
        "1.1.1.1.",
    )
}