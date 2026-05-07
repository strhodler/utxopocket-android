package com.strhodler.utxopocket.domain.model

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class IncomingWatcherPolicy(
    val baseIntervalSeconds: Int = 30,
    val maxIntervalSeconds: Int = 180,
    val backoffMultiplier: Double = 2.0,
    val jitterSeconds: Int = 2
) {
    init {
        require(baseIntervalSeconds > 0) { "baseIntervalSeconds must be positive" }
        require(maxIntervalSeconds >= baseIntervalSeconds) { "maxIntervalSeconds must be >= baseIntervalSeconds" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
        require(jitterSeconds >= 0) { "jitterSeconds must be >= 0" }
    }

    fun nextDelayMillis(currentSeconds: Int, success: Boolean): Long {
        val targetSeconds = if (success) {
            baseIntervalSeconds
        } else {
            min(maxIntervalSeconds, max(baseIntervalSeconds, (currentSeconds * backoffMultiplier).toInt()))
        }
        val jitter = if (jitterSeconds == 0) 0 else Random.nextInt(-jitterSeconds, jitterSeconds + 1)
        val adjusted = (targetSeconds + jitter).coerceAtLeast(baseIntervalSeconds)
        return adjusted * 1_000L
    }
}
