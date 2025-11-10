package com.strhodler.utxopocket.data.db

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton
import com.strhodler.utxopocket.data.security.SqlCipherPassphraseProvider

@Singleton
class EncryptedSupportFactoryProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passphraseProvider: SqlCipherPassphraseProvider
) {

    fun create(): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        val passphrase = passphraseProvider.obtainPassphrase()
        return SupportFactory(passphrase)
    }
}
