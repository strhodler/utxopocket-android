package com.strhodler.utxopocket.common.logging

import android.util.Log
import com.strhodler.utxopocket.BuildConfig

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
            Log.d(tag, message())
        }
    }

    /**
     * Emits an info line if logging is enabled.
     */
    inline fun i(tag: String, message: () -> String) {
        if (enabled()) {
            Log.i(tag, message())
        }
    }

    /**
     * Emits a warning, optionally with a [Throwable], when logging is enabled.
     */
    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled()) {
            if (throwable != null) {
                Log.w(tag, message(), throwable)
            } else {
                Log.w(tag, message())
            }
        }
    }

    /**
     * Emits an error, optionally with a [Throwable], when logging is enabled.
     */
    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled()) {
            if (throwable != null) {
                Log.e(tag, message(), throwable)
            } else {
                Log.e(tag, message())
            }
        }
    }

    // Java-friendly overloads (no lambdas)
    @JvmStatic
    fun d(tag: String, message: String) {
        if (enabled()) {
            Log.d(tag, message)
        }
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (enabled()) {
            Log.i(tag, message)
        }
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        if (enabled()) {
            Log.w(tag, message)
        }
    }

    @JvmStatic
    fun w(tag: String, throwable: Throwable, message: String) {
        if (enabled()) {
            Log.w(tag, message, throwable)
        }
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        if (enabled()) {
            Log.e(tag, message)
        }
    }

    @JvmStatic
    fun e(tag: String, throwable: Throwable, message: String) {
        if (enabled()) {
            Log.e(tag, message, throwable)
        }
    }
}
