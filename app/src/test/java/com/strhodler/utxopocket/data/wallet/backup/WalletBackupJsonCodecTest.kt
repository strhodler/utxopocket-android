package com.strhodler.utxopocket.data.wallet.backup

import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletBackupJsonCodecTest {

    @Test
    fun encodeDecodeRoundtripPreservesPayload() {
        val payload = WalletBackupPayload(
            wallets = listOf(
                WalletBackupWallet(
                    walletRef = "wallet-1",
                    meta = WalletBackupWalletMeta(
                        name = "Primary",
                        network = "TESTNET4",
                        descriptor = "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/0/*)",
                        changeDescriptor = "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/1/*)",
                        sharedDescriptors = false,
                        viewOnly = true,
                        color = "orange",
                        sortOrder = 0
                    ),
                    labels = WalletBackupLabels(),
                    collections = emptyList(),
                    collectionMemberships = emptyList(),
                    canvasItems = emptyList()
                )
            ),
            appPreferences = null,
            walletDetailPreferences = emptyList()
        )

        val encoded = WalletBackupJsonCodec.encode(
            payload = payload,
            passphrase = "correct horse battery staple".toCharArray(),
            iterations = 150_000,
            createdAtMillis = 1_770_000_000_000,
            secureRandom = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(42L) }
        )

        val decoded = WalletBackupJsonCodec.decode(
            encoded = encoded,
            passphrase = "correct horse battery staple".toCharArray()
        )

        val success = assertIs<WalletBackupDecodeResult.Success>(decoded)
        assertEquals(1_770_000_000_000, success.createdAtMillis)
        assertEquals(payload, success.payload)
    }

    @Test
    fun decodeRejectsUnknownEnvelopeField() {
        val payload = WalletBackupPayload(
            wallets = emptyList(),
            appPreferences = null,
            walletDetailPreferences = emptyList()
        )
        val encoded = WalletBackupJsonCodec.encode(
            payload = payload,
            passphrase = "passphrase".toCharArray(),
            iterations = 150_000,
            createdAtMillis = 1_770_000_000_000,
            secureRandom = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(7L) }
        )
        val mutated = String(encoded, Charsets.UTF_8)
            .replaceFirst("{", "{\"unexpected\":true,")
            .toByteArray(Charsets.UTF_8)

        val decoded = WalletBackupJsonCodec.decode(
            encoded = mutated,
            passphrase = "passphrase".toCharArray()
        )

        val failure = assertIs<WalletBackupDecodeResult.Failure>(decoded)
        assertIs<WalletBackupFailure.InvalidPayload>(failure.failure)
    }

    @Test
    fun decodeWithWrongPassphraseFailsClosed() {
        val payload = WalletBackupPayload(
            wallets = emptyList(),
            appPreferences = null,
            walletDetailPreferences = emptyList()
        )
        val encoded = WalletBackupJsonCodec.encode(
            payload = payload,
            passphrase = "correct".toCharArray(),
            iterations = 150_000,
            createdAtMillis = 1_770_000_000_000,
            secureRandom = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(100L) }
        )

        val decoded = WalletBackupJsonCodec.decode(
            encoded = encoded,
            passphrase = "wrong".toCharArray()
        )

        val failure = assertIs<WalletBackupDecodeResult.Failure>(decoded)
        assertEquals(WalletBackupFailure.WrongPassphraseOrCorrupt, failure.failure)
    }
}
