package com.strhodler.utxopocket.domain.service

interface IncomingTxWatcher : IncomingTxChecker {
    fun setForeground(foreground: Boolean)
}
