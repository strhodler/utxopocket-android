package com.strhodler.utxopocket.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.strhodler.utxopocket.common.logging.SecureLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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

        val passphrase = generatePassphrase()
        persistPassphrase(passphrase)
        return passphrase
    }

    fun generatePassphrase(): ByteArray =
        ByteArray(PASSPHRASE_LENGTH_BYTES).also(secureRandom::nextBytes)

    fun persistPassphrase(passphrase: ByteArray) {
        persistWithTink(passphrase)
        clearLegacyPassphraseArtifacts()
    }

    fun clearPassphrase() {
        clearTinkPassphrase()
        clearLegacyPassphraseArtifacts()
    }

    fun clearAllCryptoArtifacts() {
        clearPassphrase()
        wipeSharedPreferencesFile(TinkCrypto.AEAD_KEYSET_PREFS_NAME)
        wipeSharedPreferencesFile(TinkCrypto.STREAMING_KEYSET_PREFS_NAME)
    }

    private fun readFromTink(): ByteArray? {
        val aead = tinkCrypto.requireAead()
        val ciphertext = tinkPreferences().getString(TINK_KEY_PASSPHRASE, null) ?: return null
        val decoded = runCatching {
            Base64.decode(ciphertext, Base64.NO_WRAP)
        }.getOrElse { cause ->
            throw IllegalStateException("Stored SQLCipher passphrase payload is invalid", cause)
        }
        val decrypted = runCatching {
            aead.decrypt(decoded, TINK_ASSOCIATED_DATA)
        }.getOrElse { cause ->
            throw IllegalStateException("Unable to decrypt SQLCipher passphrase from strict Tink store", cause)
        }
        SecureLog.d(TAG) { "Loaded SQLCipher passphrase from Tink" }
        return decrypted
    }

    private fun persistWithTink(passphrase: ByteArray) {
        val aead = tinkCrypto.requireAead()
        val ciphertext = runCatching {
            aead.encrypt(passphrase, TINK_ASSOCIATED_DATA)
        }.getOrElse { cause ->
            throw IllegalStateException("Unable to encrypt SQLCipher passphrase with strict Tink store", cause)
        }
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val prefs = tinkPreferences()
        prefs.edit(commit = true) {
            putString(TINK_KEY_PASSPHRASE, encoded)
        }
        val persisted = prefs.getString(TINK_KEY_PASSPHRASE, null) == encoded
        if (!persisted) {
            throw IllegalStateException("Unable to persist SQLCipher passphrase in strict Tink store")
        }
        SecureLog.d(TAG) { "Persisted SQLCipher passphrase with Tink (success=$persisted)" }
    }

    private fun tinkPreferences(): SharedPreferences =
        context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)

    private fun clearLegacyPassphraseArtifacts() {
        runCatching { deleteSharedPreferencesCompat(PREFS_NAME) }
    }

    private fun wipeSharedPreferencesFile(name: String) {
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            clear()
        }
        if (prefs.all.isNotEmpty()) {
            throw IllegalStateException("Unable to clear shared preferences for $name")
        }
        val deleted = deleteSharedPreferencesCompat(name)
        val sharedPrefsFile = sharedPreferencesFile(name)
        if (!deleted && sharedPrefsFile.exists()) {
            throw IllegalStateException("Unable to delete shared preferences file for $name")
        }
    }

    private fun deleteSharedPreferencesCompat(name: String): Boolean {
        val deletedByApi = runCatching { context.deleteSharedPreferences(name) }.getOrDefault(false)
        val file = sharedPreferencesFile(name)
        return if (file.exists()) {
            file.delete()
        } else {
            deletedByApi
        }
    }

    private fun sharedPreferencesFile(name: String): File =
        File(File(context.applicationInfo.dataDir, SHARED_PREFS_DIR), "$name.xml")

    private fun clearTinkPassphrase() {
        val prefs = tinkPreferences()
        prefs.edit(commit = true) {
            remove(TINK_KEY_PASSPHRASE)
        }
        if (prefs.contains(TINK_KEY_PASSPHRASE)) {
            throw IllegalStateException("Unable to clear SQLCipher passphrase in strict Tink store")
        }
    }

    companion object {
        const val PREFS_NAME = "secure_store"
        const val PASSPHRASE_LENGTH_BYTES = 64
        const val TINK_PREFS_NAME = "secure_store_tink"
        const val TINK_KEY_PASSPHRASE = "sqlcipher_passphrase_v2"
        val TINK_ASSOCIATED_DATA = "sqlcipher-passphrase".toByteArray(Charsets.UTF_8)
        const val TAG = "SqlCipherPassphrase"
        private const val SHARED_PREFS_DIR = "shared_prefs"
    }
}
