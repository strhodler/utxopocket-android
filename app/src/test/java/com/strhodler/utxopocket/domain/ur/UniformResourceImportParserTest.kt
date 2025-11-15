package com.strhodler.utxopocket.domain.ur

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.domain.model.ExtendedKeyScriptType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UniformResourceImportParserTest {

    @Test
    fun `crypto-output without branches falls back to extended key`() {
        val ur = "ur:crypto-output/taadmwtaaddlosaowkaxhdclaxdezehfnbjetispdrpldyrelstdsrldvlcevymssnskftoykeehbtsbbspswdlnvtaahdcxjejsfpuetplplukpiatkcpgtIagabwlrwewddlglsfahgsptfhosehcwgsrtrywmahtaadehoeadaeoadamtaaddyotadlncsghykadykaeykaocymnlajyqdaxaxaycysbrppsdaasihfwgagdeoesrokkbsot".lowercase()

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.TESTNET)
        val extended = assertIs<UniformResourceResult.ExtendedKey>(result)

        assertEquals("tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac", extended.extendedKey)
        assertEquals("m/84'/1'/0'", extended.derivationPath)
        assertEquals("8e8074b3", extended.masterFingerprint)
        assertEquals(BitcoinNetwork.TESTNET, extended.detectedNetwork)
        assertTrue(extended.includeChange)
        assertEquals(ExtendedKeyScriptType.P2WPKH, extended.scriptType)
    }


    @Test
    fun `parses crypto-output multisig descriptor`() {
        val ur = "ur:crypto-output/taadmetaadmtoeadadaolftaaddloxaxhdclaxsbsgptsolkltkndsmskiaelfhhmdimcnmnlgutzotecpsfveylgrbdhptbpsveosaahdcxhnganelacwldjnlschnyfxjyplrllfdrplpswdnbuyctlpwyfmmhgsgtwsrymtldamtaaddyoeadlaaxaeattaaddyoyadlnadwkaewklawktaaddloxaxhdclaoztnnhtwtpslgndfnwpzedrlomnclchrdfsayntlplplojznslfjejecpptlgbgwdaahdcxwtmhnyzmpkkbvdpyvwutglbeahmktyuogusnjonththhdwpsfzvdfpdlcndlkensamtaaddyoeadlfaewkaocyrycmrnvwattaaddyoyadlnaewkaewklawktdbsfttn"

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.MAINNET)
        val descriptor = assertIs<UniformResourceResult.Descriptor>(result)

        assertTrue(descriptor.descriptor.contains("wsh("))
        assertTrue(descriptor.descriptor.contains("multi("))
        assertNotNull(descriptor.changeDescriptor)
        assertTrue(descriptor.changeDescriptor!!.contains("multi("))
        assertTrue(descriptor.changeDescriptor != descriptor.descriptor)
    }

    @Test
    fun `parses crypto-hdkey into extended key payload`() {
        val ur =
            "ur:crypto-hdkey/onaxhdclaojlvoechgferkdpqdiabdrflawshlhdmdcemtfnlrctghchbdolvwsednvdztbgolaahdcxtottgostdkhfdahdlykkecbbweskrymwflvdylgerkloswtbrpfdbsticmwylklpahtaadehoyaoadamtaaddyoyadlecsdwykadykadykaewkadwkaycywlcscewfihbdaehn"

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.TESTNET)
        val extended = assertIs<UniformResourceResult.ExtendedKey>(result)

        assertEquals(
            "vpub5bQsZjhtY6xG5ENzvUMpDm5oW1uuYyWLpntuh7gr5bjXKc45R7F5xGT79PUPH18GNMG4h6tuUPwrLDmxkuwHTRqx67SEZXWs4hAsxye7pK7",
            extended.extendedKey
        )
        assertEquals("m/44'/1'/1'/0/1", extended.derivationPath)
        assertEquals("e9181cf3", extended.masterFingerprint)
        assertEquals(BitcoinNetwork.TESTNET, extended.detectedNetwork)
        assertNotNull(extended)
    }
}
