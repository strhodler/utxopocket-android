package com.strhodler.utxopocket.tor.control

interface TorControlFacade {
    suspend fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean
    suspend fun isRunning(): Boolean
    suspend fun setNetworkEnabled(enable: Boolean)
    fun getIpv4LocalHostSocksPort(): Int
    fun getLastLog(): String
    suspend fun stop()
    suspend fun newIdentity(): Boolean
}
