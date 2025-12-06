package com.strhodler.utxopocket.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.strhodler.utxopocket.common.logging.SecureLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets

@Singleton
class SqlCipherPassphraseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tinkCrypto: TinkCrypto
) {

    private val secureRandom = SecureRandom()

    fun obtainPassphrase(): ByteArray {
        readFromTink()?.let { return it }

        val legacyPreferences = encryptedPreferencesOrNull()
        val legacyExisting = legacyPreferences?.getString(KEY_PASSPHRASE, null)
        if (legacyExisting != null) {
            val decoded = Base64.decode(legacyExisting, Base64.NO_WRAP)
            if (persistWithTink(decoded)) {
                legacyPreferences.edit().remove(KEY_PASSPHRASE).commit()
            }
            return decoded
        }

        val passphrase = generatePassphrase()
        persistPassphrase(passphrase)
        return passphrase
    }

    fun generatePassphrase(): ByteArray =
        ByteArray(PASSPHRASE_LENGTH_BYTES).also(secureRandom::nextBytes)

    fun persistPassphrase(passphrase: ByteArray) {
        if (persistWithTink(passphrase)) {
            clearLegacyPassphrase()
        } else if (!persistWithLegacy(passphrase)) {
            throw IllegalStateException("Unable to persist SQLCipher passphrase")
        }
    }

    fun clearPassphrase() {
        clearTinkPassphrase()
        clearLegacyPassphrase()
    }

    private fun encryptedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun encryptedPreferencesOrNull(): SharedPreferences? =
        runCatching { encryptedPreferences() }.getOrNull()

    private fun persistWithLegacy(passphrase: ByteArray): Boolean {
        val prefs = encryptedPreferencesOrNull() ?: return false
        val encoded = Base64.encodeToString(passphrase, Base64.NO_WRAP)
        val result = prefs.edit().putString(KEY_PASSPHRASE, encoded).commit()
        SecureLog.d(TAG) { "Persisted SQLCipher passphrase with legacy EncryptedSharedPreferences (success=$result)" }
        return result
    }

    private fun readFromTink(): ByteArray? {
        val aead = tinkCrypto.aeadOrNull() ?: return null
        val ciphertext = tinkPreferences().getString(TINK_KEY_PASSPHRASE, null) ?: return null
        val decoded = runCatching { Base64.decode(ciphertext, Base64.NO_WRAP) }.getOrNull() ?: return null
        val decrypted = runCatching { aead.decrypt(decoded, TINK_ASSOCIATED_DATA) }.getOrNull()
        if (decrypted != null) {
            SecureLog.d(TAG) { "Loaded SQLCipher passphrase from Tink" }
        }
        return decrypted
    }

    private fun persistWithTink(passphrase: ByteArray): Boolean {
        val aead = tinkCrypto.aeadOrNull() ?: return false
        val ciphertext = runCatching { aead.encrypt(passphrase, TINK_ASSOCIATED_DATA) }.getOrNull() ?: return false
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val result = tinkPreferences().edit().putString(TINK_KEY_PASSPHRASE, encoded).commit()
        SecureLog.d(TAG) { "Persisted SQLCipher passphrase with Tink (success=$result)" }
        return result
    }

    private fun tinkPreferences(): SharedPreferences =
        context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)

    private fun clearLegacyPassphrase() {
        encryptedPreferencesOrNull()?.edit()?.remove(KEY_PASSPHRASE)?.commit()
    }

    private fun clearTinkPassphrase() {
        tinkPreferences().edit().remove(TINK_KEY_PASSPHRASE).commit()
    }

    private companion object {
        const val PREFS_NAME = "secure_store"
        const val KEY_PASSPHRASE = "sqlcipher_passphrase"
        const val PASSPHRASE_LENGTH_BYTES = 64
        const val TINK_PREFS_NAME = "secure_store_tink"
        const val TINK_KEY_PASSPHRASE = "sqlcipher_passphrase_v2"
        val TINK_ASSOCIATED_DATA = "sqlcipher-passphrase".toByteArray(Charsets.UTF_8)
        const val TAG = "SqlCipherPassphrase"
    }
}
