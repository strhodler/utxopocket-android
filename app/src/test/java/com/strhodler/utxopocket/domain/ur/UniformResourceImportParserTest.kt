package com.strhodler.utxopocket.domain.ur

import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UniformResourceImportParserTest {

    @Test
    fun `invalid checksum payload returns failure`() {
        val ur = "ur:crypto-output/taadmwtaaddlosaowkaxhdclaxdezehfnbjetispdrpldyrelstdsrldvlcevymssnskftoykeehbtsbbspswdlnvtaahdcxjejsfpuetplplukpiatkcpgtIagabwlrwewddlglsfahgsptfhosehcwgsrtrywmahtaadehoeadaeoadamtaaddyotadlncsghykadykaeykaocymnlajyqdaxaxaycysbrppsdaasihfwgagdeoesrokkbsot"

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.TESTNET)
        val failure = assertIs<UniformResourceResult.Failure>(result)

        assertTrue(failure.reason.contains("checksum", ignoreCase = true))
    }


    @Test
    fun `parses crypto-output multisig descriptor`() {
        val ur = "ur:crypto-output/taadmetaadmtoeadadaolftaaddloxaxhdclaxsbsgptsolkltkndsmskiaelfhhmdimcnmnlgutzotecpsfveylgrbdhptbpsveosaahdcxhnganelacwldjnlschnyfxjyplrllfdrplpswdnbuyctlpwyfmmhgsgtwsrymtldamtaaddyoeadlaaxaeattaaddyoyadlnadwkaewklawktaaddloxaxhdclaoztnnhtwtpslgndfnwpzedrlomnclchrdfsayntlplplojznslfjejecpptlgbgwdaahdcxwtmhnyzmpkkbvdpyvwutglbeahmktyuogusnjonththhdwpsfzvdfpdlcndlkensamtaaddyoeadlfaewkaocyrycmrnvwattaaddyoyadlnaewkaewklawktdbsfttn"

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.MAINNET)
        val descriptor = assertIs<UniformResourceResult.Descriptor>(result)

        assertTrue(descriptor.descriptor.contains("wsh("))
        assertTrue(descriptor.descriptor.contains("multi("))
        assertEquals(null, descriptor.changeDescriptor)
    }

    @Test
    fun `parses crypto-hdkey into extended key payload`() {
        val ur =
            "ur:crypto-hdkey/onaxhdclaojlvoechgferkdpqdiabdrflawshlhdmdcemtfnlrctghchbdolvwsednvdztbgolaahdcxtottgostdkhfdahdlykkecbbweskrymwflvdylgerkloswtbrpfdbsticmwylklpahtaadehoyaoadamtaaddyoyadlecsdwykadykadykaewkadwkaycywlcscewfihbdaehn"

        val result = UniformResourceImportParser.parse(ur, BitcoinNetwork.TESTNET)
        val extended = assertIs<UniformResourceResult.ExtendedKey>(result)

        assertEquals(
            "tpubDHW3GtnVrTatx38EcygoSf9UhUd9Dx1rht7FAL8unrMo8r2NWhJuYNqDFS7cZFVbDaxJkV94MLZAr86XFPsAPYcoHWJ7sWYsrmHDw5sKQ2K",
            extended.extendedKey
        )
        assertEquals("m/44'/1'/1'/0/1", extended.derivationPath)
        assertEquals(null, extended.masterFingerprint)
        assertEquals(BitcoinNetwork.TESTNET, extended.detectedNetwork)
        assertNotNull(extended)
    }
}
