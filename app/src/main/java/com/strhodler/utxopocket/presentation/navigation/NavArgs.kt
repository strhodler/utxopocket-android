package com.strhodler.utxopocket.presentation.navigation

import android.os.Bundle
import androidx.navigation.NavBackStackEntry

internal fun parseLongArgValue(value: Any?): Long? = when (value) {
    is Long -> value
    is Int -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
}

internal fun parseIntArgValue(value: Any?): Int? = when (value) {
    is Int -> value
    is Long -> value.toInt()
    is String -> value.toIntOrNull()
    else -> null
}

internal inline fun <reified T : Enum<T>> parseEnumArgValue(rawValue: String?): T? {
    if (rawValue == null) return null
    return enumValues<T>().firstOrNull { enumValue -> enumValue.name == rawValue }
}

internal fun requireLongArgValue(key: String, value: Any?): Long =
    parseLongArgValue(value) ?: throw IllegalArgumentException("Missing required argument: $key")

internal fun Bundle?.longArgOrNull(key: String): Long? {
    val bundle = this ?: return null
    if (!bundle.containsKey(key)) return null

    val stringValue = bundle.getString(key)
    if (stringValue != null) return parseLongArgValue(stringValue)

    return runCatching { parseLongArgValue(bundle.getLong(key)) }.getOrNull()
        ?: runCatching { parseLongArgValue(bundle.getInt(key)) }.getOrNull()
}

internal fun Bundle?.intArgOrNull(key: String): Int? {
    val bundle = this ?: return null
    if (!bundle.containsKey(key)) return null

    val stringValue = bundle.getString(key)
    if (stringValue != null) return parseIntArgValue(stringValue)

    return runCatching { parseIntArgValue(bundle.getInt(key)) }.getOrNull()
        ?: runCatching { parseIntArgValue(bundle.getLong(key)) }.getOrNull()
}

internal fun Bundle?.stringArgOrNull(key: String): String? = this?.getString(key)

internal fun Bundle?.requireLongArg(key: String): Long =
    longArgOrNull(key) ?: throw IllegalArgumentException("Missing required argument: $key")

internal fun Bundle?.requireIntArg(key: String): Int =
    intArgOrNull(key) ?: throw IllegalArgumentException("Missing required argument: $key")

internal inline fun <reified T : Enum<T>> Bundle?.enumArgOrNull(key: String): T? =
    parseEnumArgValue(stringArgOrNull(key))

internal fun NavBackStackEntry.longArgOrNull(key: String): Long? = arguments.longArgOrNull(key)

internal fun NavBackStackEntry.intArgOrNull(key: String): Int? = arguments.intArgOrNull(key)

internal fun NavBackStackEntry.requireLongArg(key: String): Long = arguments.requireLongArg(key)

internal fun NavBackStackEntry.requireIntArg(key: String): Int = arguments.requireIntArg(key)

internal inline fun <reified T : Enum<T>> NavBackStackEntry.enumArgOrNull(key: String): T? =
    arguments.enumArgOrNull(key)
