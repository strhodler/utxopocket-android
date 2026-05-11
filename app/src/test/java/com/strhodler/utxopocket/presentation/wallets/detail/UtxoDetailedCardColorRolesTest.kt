package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.domain.model.WalletAddressType
import kotlin.test.Test
import kotlin.test.assertEquals

class UtxoDetailedCardColorRolesTest {

    @Test
    fun changeUtxosUseErrorColorRoles() {
        val roles = utxoDetailedCardColorRoles(WalletAddressType.CHANGE)

        assertEquals(UtxoDetailedCardColorRole.ErrorContainerMuted, roles.container)
        assertEquals(UtxoDetailedCardColorRole.OnErrorContainer, roles.content)
        assertEquals(UtxoDetailedCardColorRole.OnErrorContainerMuted, roles.supporting)
        assertEquals(UtxoDetailedCardColorRole.Error, roles.amount)
        assertEquals(UtxoDetailedCardColorRole.Error, roles.badgeContainer)
        assertEquals(UtxoDetailedCardColorRole.OnError, roles.badgeContent)
    }

    @Test
    fun externalUtxosKeepDefaultColorRoles() {
        val roles = utxoDetailedCardColorRoles(WalletAddressType.EXTERNAL)

        assertEquals(UtxoDetailedCardColorRole.SurfaceContainer, roles.container)
        assertEquals(UtxoDetailedCardColorRole.OnSurface, roles.content)
        assertEquals(UtxoDetailedCardColorRole.OnSurfaceVariant, roles.supporting)
        assertEquals(UtxoDetailedCardColorRole.OnSurface, roles.amount)
        assertEquals(UtxoDetailedCardColorRole.SecondaryContainer, roles.badgeContainer)
        assertEquals(UtxoDetailedCardColorRole.OnSecondaryContainer, roles.badgeContent)
    }

    @Test
    fun unknownAddressTypeKeepsDefaultColorRoles() {
        val roles = utxoDetailedCardColorRoles(addressType = null)

        assertEquals(UtxoDetailedCardColorRole.SurfaceContainer, roles.container)
        assertEquals(UtxoDetailedCardColorRole.OnSurface, roles.content)
        assertEquals(UtxoDetailedCardColorRole.OnSurfaceVariant, roles.supporting)
        assertEquals(UtxoDetailedCardColorRole.OnSurface, roles.amount)
        assertEquals(UtxoDetailedCardColorRole.SecondaryContainer, roles.badgeContainer)
        assertEquals(UtxoDetailedCardColorRole.OnSecondaryContainer, roles.badgeContent)
    }
}
