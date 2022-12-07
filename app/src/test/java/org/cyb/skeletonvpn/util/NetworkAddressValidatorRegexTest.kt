package org.cyb.skeletonvpn.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(PER_CLASS)
class NetworkAddressValidatorRegexTest {
    @ParameterizedTest
    @MethodSource("validIPv4Provider")
    fun iPv4_isValid_should_return_true(valid: String) {
        assertTrue(NetworkAddressValidatorRegex.IPv4.isValid(valid),
            "\"${javaClass.simpleName}|IPv4_isValid_should_return_true failed on [$valid]")
    }

    @ParameterizedTest
    @MethodSource("invalidIPv4Provider")
    fun iPv4_isValid_should_return_false(invalid: String) {
        assertFalse(NetworkAddressValidatorRegex.IPv4.isValid(invalid),
            "${javaClass.simpleName}|IPv4_isValid_should_return_false failed on [$invalid]")
    }


    @ParameterizedTest
    @MethodSource("validIPv6Provider")
    fun iPv6_isValid_should_return_true(valid: String) {
        assertTrue(NetworkAddressValidatorRegex.IPv6.isValid(valid),
            "${javaClass.simpleName}|IPv6_isValid_should_return_true Failed on [$valid]")
    }

    @ParameterizedTest
    @MethodSource("invalidIPv6Provider")
    fun iPv6_isValid_should_return_false(invalid: String) {
        assertFalse(NetworkAddressValidatorRegex.IPv6.isValid(invalid),
            "${javaClass.simpleName}|IPv6_isValid_should_return_false Failed on [$invalid]")
    }

    @ParameterizedTest
    @MethodSource("validPortProvider")
    fun port_isValid_should_return_true(valid: String) {
        assertTrue(NetworkAddressValidatorRegex.PORT.isValid(valid),
            "${javaClass.simpleName}|Port_isValid_should_return_true failed on [$valid]")
    }

    @ParameterizedTest
    @MethodSource("invalidPortProvider")
    fun port_isValid_should_return_false(invalid: String) {
       assertFalse(NetworkAddressValidatorRegex.PORT.isValid(invalid),
           "${javaClass.simpleName}|Port_isValid_should_return_false failed on [$invalid]")
    }

    @ParameterizedTest
    @MethodSource("validIPv4Provider", "validIPv6Provider")
    fun isValidAddress_should_return_true(valid: String) {
       assertTrue(NetworkAddressValidatorRegex.isValidIpAddress(valid),
           "${javaClass.simpleName}|isValidIpAddress_should_return_true failed on [$valid]")
    }

    @ParameterizedTest
    @MethodSource("invalidIPv4Provider", "invalidIPv6Provider")
    fun isAcceptableIpAddress_should_return_false(invalid: String) {
       assertFalse(NetworkAddressValidatorRegex.isValidIpAddress(invalid),
           "${javaClass.simpleName}|isValidIPAddress_should_return_false failed on [$invalid]")
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

    private fun validIPv6Provider() = Stream.of(
        "::",
        "::1234:5678",
        "684d:1111:222:3333:4444:5555:6:77",
        "2001:db8::",
        "2001:db8::1234:5678",
        "2001:db8:1::ab9:C0A8:102",
        "2001:db8:3333:4444:5555:6666:7777:8888",
        "2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF",
        "::11.22.33.44",
    )

    private fun invalidIPv6Provider() = Stream.of(
        "",
        "-",
        "1.2.",
        "xxx",
        "/x00/x00/x00",
        "56FE::2159:5BBC::6594",
        "138.299.21.256",
        "2001:gd8::",
        "-1.2.3.4",
        "1.2.3.4.5",
        "138.299.21.256",
    )

    private fun validPortProvider() = Stream.of(
        "0",
        "8",
        "21",
        "333",
        "100",
        "443",
        "8080",
        "3333",
        "65535",
    )

    private fun invalidPortProvider() = Stream.of(
        "",
        "00",
        "65537",
        "66537",
        "-123",
        "00000",
        "ppp",
        "-^S*",
    )
}