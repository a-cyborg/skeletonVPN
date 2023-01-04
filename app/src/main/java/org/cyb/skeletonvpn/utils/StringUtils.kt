package org.cyb.skeletonvpn.utils

// Use Tag in any class.
val Any.TAG: String
    get() {
        return javaClass.simpleName
    }

fun String.removeWhiteSpace(): String {
    return this.filterNot { it.isWhitespace() }
}