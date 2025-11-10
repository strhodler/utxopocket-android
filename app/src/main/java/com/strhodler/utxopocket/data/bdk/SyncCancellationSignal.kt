package com.strhodler.utxopocket.data.bdk

/**
 * Lightweight hook that lets long-running Electrum sync operations observe whether the app
 * has moved to the background and should stop emitting additional requests.
 */
fun interface SyncCancellationSignal {
    /**
     * @return true when the ongoing sync should cancel as soon as possible.
     */
    fun shouldCancel(): Boolean
}

