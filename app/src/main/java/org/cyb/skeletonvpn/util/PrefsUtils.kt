package org.cyb.skeletonvpn.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

enum class Prefs (val key: String) {
    NAME("skeletonVpnPrefs"),
    SERVER_ADDRESS("serverAddress"),
    SERVER_PORT("serverPort"),
    SHARED_SECRET("sharedSecret"),
}

fun saveStringToShardPrefs(context: Context, keyStringMap: Map<Prefs, String>) {
    with (context.getSharedPreferences(Prefs.NAME.key, MODE_PRIVATE).edit()) {
        keyStringMap.forEach {
            putString(it.key.key, it.value)
        }
        commit()
    }
}

fun getStringFromSharedPrefs(context: Context, prefs: List<Prefs>) : MutableList<String> {
    val preferences = context.getSharedPreferences(Prefs.NAME.key, MODE_PRIVATE)
    val values: MutableList<String> = mutableListOf()

    prefs.forEach { pref ->
        preferences.getString(pref.key, "")?.let {
            values.add(it)
        }
    }

    return values
}