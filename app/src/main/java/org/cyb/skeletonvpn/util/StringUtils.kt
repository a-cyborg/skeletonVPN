package org.cyb.skeletonvpn.util

fun String.removeWhiteSpace() : String {
    return this.filterNot { it.isWhitespace() }
}