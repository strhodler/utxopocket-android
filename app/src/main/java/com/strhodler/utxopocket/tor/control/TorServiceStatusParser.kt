package com.strhodler.utxopocket.tor.control

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton
import org.torproject.jni.TorService

@Singleton
class TorServiceStatusParser @Inject constructor() {

    fun parseIntent(intent: Intent): TorServiceStatus {
        return when (intent.action) {
            TorService.ACTION_STATUS -> parseStatus(
                status = intent.getStringExtra(TorService.EXTRA_STATUS)
            )

            TorService.ACTION_ERROR -> parseStatus(
                status = null,
                errorMessage = intent.getStringExtra(Intent.EXTRA_TEXT)
            )

            else -> parseStatus(status = null)
        }
    }

    fun parseStatus(status: String?, errorMessage: String? = null): TorServiceStatus {
        val normalizedError = errorMessage?.trim().orEmpty()
        if (normalizedError.isNotEmpty()) {
            return TorServiceStatus(
                state = TorServiceRuntimeState.ERROR,
                bootstrapPercent = 0,
                message = normalizedError
            )
        }

        val normalizedStatus = status?.trim().orEmpty()
        if (normalizedStatus.isBlank()) {
            return TorServiceStatus(
                state = TorServiceRuntimeState.UNKNOWN,
                bootstrapPercent = 0,
                message = ""
            )
        }

        val bootstrapPercent = extractBootstrapPercent(normalizedStatus)

        return when {
            bootstrapPercent != null && bootstrapPercent >= 100 -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.RUNNING,
                    bootstrapPercent = 100,
                    message = normalizedStatus
                )
            }

            bootstrapPercent != null -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.STARTING,
                    bootstrapPercent = bootstrapPercent,
                    message = normalizedStatus
                )
            }

            normalizedStatus.equals("ON", ignoreCase = true) -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.RUNNING,
                    bootstrapPercent = 100,
                    message = normalizedStatus
                )
            }

            normalizedStatus.equals("OFF", ignoreCase = true) -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.STOPPED,
                    bootstrapPercent = 0,
                    message = normalizedStatus
                )
            }

            normalizedStatus.equals("STARTING", ignoreCase = true) -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.STARTING,
                    bootstrapPercent = 0,
                    message = normalizedStatus
                )
            }

            normalizedStatus.equals("STOPPING", ignoreCase = true) -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.STOPPING,
                    bootstrapPercent = 0,
                    message = normalizedStatus
                )
            }

            else -> {
                TorServiceStatus(
                    state = TorServiceRuntimeState.UNKNOWN,
                    bootstrapPercent = 0,
                    message = normalizedStatus
                )
            }
        }
    }

    private fun extractBootstrapPercent(message: String): Int? {
        val match = BOOTSTRAP_PERCENT_PATTERN.find(message) ?: return null
        return match.groupValues
            .getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 100)
    }
}

data class TorServiceStatus(
    val state: TorServiceRuntimeState,
    val bootstrapPercent: Int,
    val message: String
)

enum class TorServiceRuntimeState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    ERROR,
    UNKNOWN
}

private val BOOTSTRAP_PERCENT_PATTERN = Regex("Bootstrapped\\s+(\\d+)%", RegexOption.IGNORE_CASE)
