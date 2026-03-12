package com.strhodler.utxopocket.tor.control

interface TorControlFacade {
    fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean
    fun isRunning(): Boolean
    fun setNetworkEnabled(enable: Boolean)
    fun getIpv4LocalHostSocksPort(): Int
    fun getLastLog(): String
    fun stop()
    fun newIdentity(): Boolean
}
