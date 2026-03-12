package com.strhodler.utxopocket.tor.control

import android.content.Context
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyTorControlFacade @Inject constructor(
    @ApplicationContext context: Context
) : TorControlFacade {

    private val onionProxyManager = AndroidOnionProxyManager(context, TOR_DIRECTORY)

    override fun startWithRepeat(totalSecondsPerTorStartup: Int, totalTriesPerTorStartup: Int): Boolean {
        return onionProxyManager.startWithRepeat(totalSecondsPerTorStartup, totalTriesPerTorStartup)
    }

    override fun isRunning(): Boolean = onionProxyManager.isRunning

    override fun setNetworkEnabled(enable: Boolean) {
        onionProxyManager.enableNetwork(enable)
    }

    override fun getIpv4LocalHostSocksPort(): Int = onionProxyManager.getIPv4LocalHostSocksPort()

    override fun getLastLog(): String = onionProxyManager.getLastLog()

    override fun stop() {
        onionProxyManager.stop()
    }

    override fun newIdentity(): Boolean = onionProxyManager.newIdentity()
}

private const val TOR_DIRECTORY = "torfiles"
