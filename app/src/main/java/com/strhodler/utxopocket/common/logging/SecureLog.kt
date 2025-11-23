package com.strhodler.utxopocket.common.logging

import android.util.Log
import com.strhodler.utxopocket.BuildConfig

/**
 * Security-first logger that is a no-op in release builds to avoid leaking data to system logs.
 */
object SecureLog {
    @PublishedApi
    internal fun enabled(): Boolean = BuildConfig.DEBUG

    // Kotlin-friendly, lazy builders
    inline fun d(tag: String, message: () -> String) {
        if (enabled()) {
            Log.d(tag, message())
        }
    }

    inline fun i(tag: String, message: () -> String) {
        if (enabled()) {
            Log.i(tag, message())
        }
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (enabled()) {
            if (throwable != null) {
                Log.w(tag, message(), throwable)
            } else {
                Log.w(tag, message())
            }
        }
    }

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
