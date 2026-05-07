package com.strhodler.utxopocket.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TinkCryptoStrictContractTest {

    private lateinit var context: Context

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun requireAeadReturnsPrimitiveWhenKeystoreBackedBindingIsAvailable() {
        val expected = createAead()
        val crypto = object : TinkCrypto(context) {
            @Suppress("UNCHECKED_CAST")
            override fun <T> loadPrimitiveBinding(
                keysetName: String,
                prefName: String,
                template: com.google.crypto.tink.KeyTemplate,
                primitiveClass: Class<T>
            ): PrimitiveBinding<T> =
                PrimitiveBinding(
                    primitive = expected as T,
                    keystoreBacked = true
                )
        }

        val actual = crypto.requireAead()

        assertSame(expected, actual)
    }

    @Test
    fun requireAeadRejectsBindingsWithoutKeystoreBacking() {
        val expected = createAead()
        val crypto = object : TinkCrypto(context) {
            @Suppress("UNCHECKED_CAST")
            override fun <T> loadPrimitiveBinding(
                keysetName: String,
                prefName: String,
                template: com.google.crypto.tink.KeyTemplate,
                primitiveClass: Class<T>
            ): PrimitiveBinding<T> =
                PrimitiveBinding(
                    primitive = expected as T,
                    keystoreBacked = false
                )
        }

        val error = assertFailsWith<TinkCrypto.StrictCryptoException> {
            crypto.requireAead()
        }

        assertEquals(TinkCrypto.StrictFailureReason.KEYSTORE_NOT_ACTIVE, error.reason)
    }

    @Test
    fun requireAeadWrapsPrimitiveLoadFailuresWithDeterministicExceptionType() {
        val crypto = object : TinkCrypto(context) {
            override fun <T> loadPrimitiveBinding(
                keysetName: String,
                prefName: String,
                template: com.google.crypto.tink.KeyTemplate,
                primitiveClass: Class<T>
            ): PrimitiveBinding<T> {
                throw IllegalStateException("boom")
            }
        }

        val error = assertFailsWith<TinkCrypto.StrictCryptoException> {
            crypto.requireAead()
        }

        assertEquals(TinkCrypto.StrictFailureReason.PRIMITIVE_UNAVAILABLE, error.reason)
        assertIs<IllegalStateException>(error.cause)
        assertEquals("boom", error.cause?.message)
    }

    private fun createAead(): Aead {
        TinkConfig.register()
        val keysetHandle = KeysetHandle.generateNew(AesGcmKeyManager.aes256GcmTemplate())
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}
