package com.strhodler.utxopocket.presentation.node

import com.strhodler.utxopocket.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeStatusRouteTest {

    @Test
    fun customNodePrimaryActionIsDisabledWhileTesting() {
        assertFalse(
            customNodePrimaryActionEnabled(
                isEditing = false,
                hasChanges = false,
                formValid = true,
                isTesting = true
            )
        )
    }

    @Test
    fun customNodePrimaryActionUsesFormStateWhenIdle() {
        assertTrue(
            customNodePrimaryActionEnabled(
                isEditing = false,
                hasChanges = false,
                formValid = true,
                isTesting = false
            )
        )
        assertTrue(
            customNodePrimaryActionEnabled(
                isEditing = true,
                hasChanges = true,
                formValid = true,
                isTesting = false
            )
        )
        assertFalse(
            customNodePrimaryActionEnabled(
                isEditing = true,
                hasChanges = false,
                formValid = true,
                isTesting = false
            )
        )
    }

    @Test
    fun customNodeAddSuccessTargetsNodesTab() {
        assertEquals(
            NodeStatusTab.Nodes.ordinal,
            nodeStatusTabAfterCustomNodeSuccess(
                messageRes = R.string.node_custom_success,
                currentTabIndex = NodeStatusTab.Management.ordinal
            )
        )
    }

    @Test
    fun nonAddCustomNodeSuccessKeepsCurrentTab() {
        assertEquals(
            NodeStatusTab.Management.ordinal,
            nodeStatusTabAfterCustomNodeSuccess(
                messageRes = R.string.node_custom_updated,
                currentTabIndex = NodeStatusTab.Management.ordinal
            )
        )
    }
}
