package com.strhodler.utxopocket.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqlCipherPassphraseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val secureRandom = SecureRandom()

    fun obtainPassphrase(): ByteArray {
        val preferences = encryptedPreferences()
        val existing = preferences.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }

        val passphrase = generatePassphrase()
        persistPassphrase(passphrase, preferences)
        return passphrase
    }

    fun generatePassphrase(): ByteArray =
        ByteArray(PASSPHRASE_LENGTH_BYTES).also(secureRandom::nextBytes)

    fun persistPassphrase(passphrase: ByteArray) {
        persistPassphrase(passphrase, encryptedPreferences())
    }

    fun clearPassphrase() {
        encryptedPreferences().edit().remove(KEY_PASSPHRASE).commit()
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

    private fun persistPassphrase(passphrase: ByteArray, preferences: SharedPreferences) {
        val encoded = Base64.encodeToString(passphrase, Base64.NO_WRAP)
        preferences.edit().putString(KEY_PASSPHRASE, encoded).commit()
    }

    private companion object {
        const val PREFS_NAME = "secure_store"
        const val KEY_PASSPHRASE = "sqlcipher_passphrase"
        const val PASSPHRASE_LENGTH_BYTES = 64
    }
}
