package com.strhodler.utxopocket.data.security

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SqlCipherPassphraseProviderTest {

    private lateinit var context: Context
    private lateinit var provider: SqlCipherPassphraseProvider

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = SqlCipherPassphraseProvider(
            context = context,
            tinkCrypto = TestTinkCrypto(
                context = context,
                aead = createAead()
            )
        )
        provider.clearPassphrase()
    }

    @AfterTest
    fun tearDown() {
        provider.clearPassphrase()
        keysetPreferences().edit().clear().commit()
        streamingKeysetPreferences().edit().clear().commit()
    }

    @Test
    fun obtainPassphraseGeneratesAndPersistsWhenStoreIsEmpty() {
        val first = provider.obtainPassphrase()

        assertEquals(64, first.size)
        assertNotNull(
            tinkPreferences().getString(TINK_KEY_PASSPHRASE, null),
            "Expected SQLCipher passphrase ciphertext in Tink preferences"
        )

        val second = provider.obtainPassphrase()
        assertContentEquals(first, second)
    }

    @Test
    fun persistObtainAndClearLifecycleUsesTinkPreferenceStore() {
        val passphrase = ByteArray(64) { index -> (index + 3).toByte() }

        provider.persistPassphrase(passphrase)
        assertNotNull(tinkPreferences().getString(TINK_KEY_PASSPHRASE, null))

        val obtained = provider.obtainPassphrase()
        assertContentEquals(passphrase, obtained)

        provider.clearPassphrase()
        assertNull(tinkPreferences().getString(TINK_KEY_PASSPHRASE, null))
    }

    @Test
    fun clearPassphraseDoesNotDeleteTinkKeysetMetadataPreferences() {
        keysetPreferences().edit().putString(AEAD_KEYSET_NAME, "baseline").commit()
        streamingKeysetPreferences().edit().putString(STREAMING_KEYSET_NAME, "baseline").commit()
        provider.persistPassphrase(ByteArray(64) { 0x21 })

        provider.clearPassphrase()

        assertNull(tinkPreferences().getString(TINK_KEY_PASSPHRASE, null))
        assertNotNull(keysetPreferences().getString(AEAD_KEYSET_NAME, null))
        assertNotNull(streamingKeysetPreferences().getString(STREAMING_KEYSET_NAME, null))
    }

    @Test
    fun clearAllCryptoArtifactsDeletesKeysetMetadataPreferences() {
        keysetPreferences().edit().putString(AEAD_KEYSET_NAME, "baseline").commit()
        streamingKeysetPreferences().edit().putString(STREAMING_KEYSET_NAME, "baseline").commit()
        provider.persistPassphrase(ByteArray(64) { 0x21 })

        provider.clearAllCryptoArtifacts()

        assertNull(tinkPreferences().getString(TINK_KEY_PASSPHRASE, null))
        assertNull(keysetPreferences().getString(AEAD_KEYSET_NAME, null))
        assertNull(streamingKeysetPreferences().getString(STREAMING_KEYSET_NAME, null))
    }

    @Test
    fun obtainPassphraseFailsClosedWhenStrictAeadIsUnavailable() {
        provider = SqlCipherPassphraseProvider(
            context = context,
            tinkCrypto = FailingAeadTinkCrypto(
                context = context,
                reason = TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE
            )
        )

        val error = assertFailsWith<TinkCrypto.StrictCryptoException> {
            provider.obtainPassphrase()
        }

        assertEquals(TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE, error.reason)
    }

    @Test
    fun obtainPassphraseDoesNotFallbackToLegacyPreferenceArtifacts() {
        val legacyEncoded = Base64.encodeToString(ByteArray(64) { 0x11 }, Base64.NO_WRAP)
        context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LEGACY_KEY_PASSPHRASE, legacyEncoded)
            .commit()

        provider = SqlCipherPassphraseProvider(
            context = context,
            tinkCrypto = FailingAeadTinkCrypto(
                context = context,
                reason = TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE
            )
        )

        val error = assertFailsWith<TinkCrypto.StrictCryptoException> {
            provider.obtainPassphrase()
        }

        assertEquals(TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE, error.reason)
    }

    private fun tinkPreferences() =
        context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)

    private fun keysetPreferences() =
        context.getSharedPreferences(KEYSET_PREFS_NAME, Context.MODE_PRIVATE)

    private fun streamingKeysetPreferences() =
        context.getSharedPreferences(STREAMING_KEYSET_PREFS_NAME, Context.MODE_PRIVATE)

    private fun createAead(): Aead {
        TinkConfig.register()
        val keysetHandle = KeysetHandle.generateNew(AesGcmKeyManager.aes256GcmTemplate())
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private class TestTinkCrypto(
        context: Context,
        private val aead: Aead
    ) : TinkCrypto(context) {
        override fun requireAead(): Aead = aead

        override fun requireStreamingAead(): StreamingAead {
            throw UnsupportedOperationException("StreamingAead is not used in this test")
        }
    }

    private class FailingAeadTinkCrypto(
        context: Context,
        private val reason: TinkCrypto.StrictFailureReason
    ) : TinkCrypto(context) {
        override fun requireAead(): Aead {
            throw TinkCrypto.StrictCryptoException(reason, "strict failure")
        }

        override fun requireStreamingAead(): StreamingAead {
            throw UnsupportedOperationException("StreamingAead is not used in this test")
        }
    }

    private companion object {
        private const val TINK_PREFS_NAME = "secure_store_tink"
        private const val TINK_KEY_PASSPHRASE = "sqlcipher_passphrase_v2"
        private const val KEYSET_PREFS_NAME = "tink_keyset_prefs"
        private const val STREAMING_KEYSET_PREFS_NAME = "tink_streaming_keyset_prefs"
        private const val AEAD_KEYSET_NAME = "utxopocket_aead_keyset"
        private const val STREAMING_KEYSET_NAME = "utxopocket_streaming_keyset"
        private const val LEGACY_PREFS_NAME = "secure_store"
        private const val LEGACY_KEY_PASSPHRASE = "sqlcipher_passphrase"
    }
}
