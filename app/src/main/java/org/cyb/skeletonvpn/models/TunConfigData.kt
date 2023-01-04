package org.cyb.skeletonvpn.models

data class TunConfigData(
    val Address: Pair<String, Int>,
    val Route: Pair<String, Int>,
    val MTU: Int,
    val DNS: String,
)
