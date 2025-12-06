package com.strhodler.utxopocket.data.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.AesGcmHkdfStreamingKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralizes Tink initialization and keyset handling for both AEAD (small secrets)
 * and StreamingAead (wallet bundle files). Falls back to null if the Keystore
 * cannot be reached so callers can use legacy storage paths.
 */
@Singleton
class TinkCrypto @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val tinkReady: Boolean = runCatching { TinkConfig.register(); true }.getOrDefault(false)

    fun aeadOrNull(): Aead? = getPrimitiveOrNull(
        keysetName = AEAD_KEYSET_NAME,
        prefName = AEAD_PREF_NAME,
        template = AesGcmKeyManager.aes256GcmTemplate(),
        primitiveClass = Aead::class.java
    )

    fun streamingAeadOrNull(): StreamingAead? = getPrimitiveOrNull(
        keysetName = STREAMING_KEYSET_NAME,
        prefName = STREAMING_PREF_NAME,
        template = AesGcmHkdfStreamingKeyManager.aes256GcmHkdf4KBTemplate(),
        primitiveClass = StreamingAead::class.java
    )

    private fun <T> getPrimitiveOrNull(
        keysetName: String,
        prefName: String,
        template: com.google.crypto.tink.KeyTemplate,
        primitiveClass: Class<T>
    ): T? {
        if (!tinkReady) return null
        return runCatching {
            AndroidKeysetManager.Builder()
                .withKeyTemplate(template)
                .withSharedPref(appContext, keysetName, prefName)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(primitiveClass)
        }.getOrNull()
    }

    private companion object {
        private const val MASTER_KEY_URI = "android-keystore://utxopocket-tink-master"
        private const val AEAD_KEYSET_NAME = "utxopocket_aead_keyset"
        private const val AEAD_PREF_NAME = "tink_keyset_prefs"
        private const val STREAMING_KEYSET_NAME = "utxopocket_streaming_keyset"
        private const val STREAMING_PREF_NAME = "tink_streaming_keyset_prefs"
    }
}
