package com.strhodler.utxopocket.data.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralizes strict Tink initialization and keyset handling for both AEAD
 * (small secrets) and StreamingAead (wallet bundle files).
 *
 * Contract: fail closed whenever the keystore-backed Tink path is unavailable.
 */
@Singleton
open class TinkCrypto @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val tinkRegistrationError: Throwable? =
        runCatching { TinkConfig.register() }.exceptionOrNull()

    open fun requireAead(): Aead = requirePrimitive(
        keysetName = AEAD_KEYSET_NAME,
        prefName = AEAD_KEYSET_PREFS_NAME,
        template = AesGcmKeyManager.aes256GcmTemplate(),
        primitiveClass = Aead::class.java,
        primitiveName = "AEAD"
    )

    open fun requireStreamingAead(): StreamingAead = requirePrimitive(
        keysetName = STREAMING_KEYSET_NAME,
        prefName = STREAMING_KEYSET_PREFS_NAME,
        template = AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate(),
        primitiveClass = StreamingAead::class.java,
        primitiveName = "StreamingAead"
    )

    protected open fun <T> loadPrimitiveBinding(
        keysetName: String,
        prefName: String,
        template: com.google.crypto.tink.KeyTemplate,
        primitiveClass: Class<T>
    ): PrimitiveBinding<T> {
        val keysetManager = AndroidKeysetManager.Builder()
            .withKeyTemplate(template)
            .withSharedPref(appContext, keysetName, prefName)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()

        val primitive = keysetManager.keysetHandle.getPrimitive(
            RegistryConfiguration.get(),
            primitiveClass
        )

        return PrimitiveBinding(
            primitive = primitive,
            keystoreBacked = keysetManager.isUsingKeystore
        )
    }

    private fun <T> requirePrimitive(
        keysetName: String,
        prefName: String,
        template: com.google.crypto.tink.KeyTemplate,
        primitiveClass: Class<T>,
        primitiveName: String
    ): T {
        val registrationFailure = tinkRegistrationError
        if (registrationFailure != null) {
            throw StrictCryptoException(
                reason = StrictFailureReason.TINK_NOT_READY,
                message = "Tink strict mode unavailable because registration failed",
                cause = registrationFailure
            )
        }

        val binding = runCatching {
            loadPrimitiveBinding(
                keysetName = keysetName,
                prefName = prefName,
                template = template,
                primitiveClass = primitiveClass
            )
        }.getOrElse { cause ->
            throw StrictCryptoException(
                reason = StrictFailureReason.PRIMITIVE_UNAVAILABLE,
                message = "Tink strict mode unavailable for $primitiveName",
                cause = cause
            )
        }

        if (!binding.keystoreBacked) {
            throw StrictCryptoException(
                reason = StrictFailureReason.KEYSTORE_NOT_ACTIVE,
                message = "Tink strict mode rejected non-keystore keyset for $primitiveName"
            )
        }

        return binding.primitive
    }

    protected data class PrimitiveBinding<T>(
        val primitive: T,
        val keystoreBacked: Boolean
    )

    class StrictCryptoException(
        val reason: StrictFailureReason,
        message: String,
        cause: Throwable? = null
    ) : IllegalStateException(message, cause)

    enum class StrictFailureReason {
        TINK_NOT_READY,
        PRIMITIVE_UNAVAILABLE,
        KEYSTORE_NOT_ACTIVE
    }

    companion object {
        private const val MASTER_KEY_URI = "android-keystore://utxopocket-tink-master"
        private const val AEAD_KEYSET_NAME = "utxopocket_aead_keyset"
        private const val STREAMING_KEYSET_NAME = "utxopocket_streaming_keyset"
        internal const val AEAD_KEYSET_PREFS_NAME = "tink_keyset_prefs"
        internal const val STREAMING_KEYSET_PREFS_NAME = "tink_streaming_keyset_prefs"
    }
}
