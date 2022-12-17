package org.cyb.skeletonvpn.util

data class ServerConfig(
    val Address: Pair<String, Int>,
    val Route: Pair<String, Int>,
    val MTU: Int,
    val DNS: String,
)
