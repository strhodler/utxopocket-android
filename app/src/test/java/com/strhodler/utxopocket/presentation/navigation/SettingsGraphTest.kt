package com.strhodler.utxopocket.presentation.navigation

import com.strhodler.utxopocket.presentation.navigation.graph.nodeStatusInitialTabIndex
import com.strhodler.utxopocket.presentation.wallets.WalletsNavigation
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsGraphTest {

    @Test
    fun `nodeStatusInitialTabIndex maps management tab to first index`() {
        val index = nodeStatusInitialTabIndex(WalletsNavigation.NodeStatusTabDestination.Management.argValue)

        assertEquals(0, index)
    }

    @Test
    fun `nodeStatusInitialTabIndex maps overview tab to second index`() {
        val index = nodeStatusInitialTabIndex(WalletsNavigation.NodeStatusTabDestination.Overview.argValue)

        assertEquals(1, index)
    }

    @Test
    fun `nodeStatusInitialTabIndex maps tor tab to third index`() {
        val index = nodeStatusInitialTabIndex(WalletsNavigation.NodeStatusTabDestination.Tor.argValue)

        assertEquals(2, index)
    }

    @Test
    fun `nodeStatusInitialTabIndex falls back to first index`() {
        val index = nodeStatusInitialTabIndex("unknown")

        assertEquals(0, index)
    }
}
