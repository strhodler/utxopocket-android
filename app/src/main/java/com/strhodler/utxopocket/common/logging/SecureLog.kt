package com.strhodler.utxopocket.common.logging

import android.util.Log
import com.strhodler.utxopocket.BuildConfig
import com.strhodler.utxopocket.tor.sanitization.TorTextSanitizer
import java.security.MessageDigest

/**
 * Security-focused wrapper around [Log] that short-circuits when `BuildConfig.DEBUG` is false,
 * ensuring diagnostics never reach release builds or leak sensitive wallet data.
 */
object SecureLog {
    @PublishedApi
    internal fun enabled(): Boolean = BuildConfig.DEBUG

    // Kotlin-friendly, lazy builders
    /**
     * Emits a debug line if logging is enabled.
     * @param tag Android log tag, usually the class name.
     * @param message Lambda evaluated only when logging is on to avoid touching sensitive data.
     */
    inline fun d(tag: String, message: () -> String) {
        if (enabled()) {
            emit { Log.d(tag, message()) }
        }
    }

    /**
     * Emits an info line if logging is enabled.
     */
    inline fun i(tag: String, message: () -> String) {
        if (enabled()) {
            emit { Log.i(tag, message()) }
        }
    }

    inline fun iTor(tag: String, message: () -> String) {
        i(tag) { sanitizeTorMessage(message()) }
    }

    /**
     * Emits a warning, optionally with a [Throwable], when logging is enabled.
     */
    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled()) {
            if (throwable != null) {
                emit { Log.w(tag, message(), throwable) }
            } else {
                emit { Log.w(tag, message()) }
            }
        }
    }

    inline fun wTor(tag: String, throwable: Throwable? = null, message: () -> String) {
        w(tag, throwable) { sanitizeTorMessage(message()) }
    }

    /**
     * Emits an error, optionally with a [Throwable], when logging is enabled.
     */
    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled()) {
            if (throwable != null) {
                emit { Log.e(tag, message(), throwable) }
            } else {
                emit { Log.e(tag, message()) }
            }
        }
    }

    inline fun eTor(tag: String, throwable: Throwable? = null, message: () -> String) {
        e(tag, throwable) { sanitizeTorMessage(message()) }
    }

    // Java-friendly overloads (no lambdas)
    @JvmStatic
    fun d(tag: String, message: String) {
        if (enabled()) {
            emit { Log.d(tag, message) }
        }
    }

    @JvmStatic
    fun dTor(tag: String, message: String) {
        d(tag, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (enabled()) {
            emit { Log.i(tag, message) }
        }
    }

    @JvmStatic
    fun iTor(tag: String, message: String) {
        i(tag, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        if (enabled()) {
            emit { Log.w(tag, message) }
        }
    }

    @JvmStatic
    fun wTor(tag: String, message: String) {
        w(tag, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun w(tag: String, throwable: Throwable, message: String) {
        if (enabled()) {
            emit { Log.w(tag, message, throwable) }
        }
    }

    @JvmStatic
    fun wTor(tag: String, throwable: Throwable, message: String) {
        w(tag, throwable, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        if (enabled()) {
            emit { Log.e(tag, message) }
        }
    }

    @JvmStatic
    fun eTor(tag: String, message: String) {
        e(tag, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun e(tag: String, throwable: Throwable, message: String) {
        if (enabled()) {
            emit { Log.e(tag, message, throwable) }
        }
    }

    @JvmStatic
    fun eTor(tag: String, throwable: Throwable, message: String) {
        e(tag, throwable, sanitizeTorMessage(message))
    }

    @JvmStatic
    fun fingerprint(value: String?, length: Int = 12): String {
        if (value.isNullOrBlank()) return "na"
        val normalized = value.trim()
        val digest = runCatching {
            MessageDigest.getInstance("SHA-256")
                .digest(normalized.toByteArray(Charsets.UTF_8))
                .toHexString()
        }.getOrElse {
            normalized.hashCode().toUInt().toString(16)
        }
        return digest.take(length.coerceAtLeast(4))
    }

    @PublishedApi
    internal inline fun emit(block: () -> Unit) {
        try {
            block()
        } catch (error: RuntimeException) {
            if (!isAndroidStubFailure(error)) {
                throw error
            }
        }
    }

    @PublishedApi
    internal fun isAndroidStubFailure(error: RuntimeException): Boolean {
        val message = error.message ?: return false
        return message.contains("Method") && message.contains("not mocked")
    }

    @PublishedApi
    internal fun ByteArray.toHexString(): String = buildString(size * 2) {
        for (byte in this@toHexString) {
            append(((byte.toInt() ushr 4) and 0x0F).toString(16))
            append((byte.toInt() and 0x0F).toString(16))
        }
    }

    @PublishedApi
    internal fun sanitizeTorMessage(message: String): String {
        return TorTextSanitizer.sanitizeForPublicDisplay(message)
    }
}
