package com.strhodler.utxopocket.tor.fakes

import com.strhodler.utxopocket.tor.control.TorControlFacade

class FakeTorControlFacade(
    var startResult: Boolean = true,
    var runningAfterStart: Boolean = true,
    var socksPort: Int = 9050,
    var latestLog: String = ""
) : TorControlFacade {

    var startWithRepeatCalls: Int = 0
        private set

    var stopCalls: Int = 0
        private set

    var newIdentityCalls: Int = 0
        private set

    var getLastLogCalls: Int = 0
        private set

    val networkEnableRequests: MutableList<Boolean> = mutableListOf()

    private var running: Boolean = false

    override fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean {
        startWithRepeatCalls += 1
        if (startResult) {
            running = runningAfterStart
        }
        return startResult
    }

    override fun isRunning(): Boolean = running

    override fun setNetworkEnabled(enable: Boolean) {
        networkEnableRequests += enable
    }

    override fun getIpv4LocalHostSocksPort(): Int = socksPort

    override fun getLastLog(): String {
        getLastLogCalls += 1
        return latestLog
    }

    override fun stop() {
        stopCalls += 1
        running = false
    }

    override fun newIdentity(): Boolean {
        newIdentityCalls += 1
        return running
    }

    fun setRunning(value: Boolean) {
        running = value
    }
}
