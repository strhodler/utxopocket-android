package com.strhodler.utxopocket.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WalletEntity::class,
        WalletTransactionEntity::class,
        WalletTransactionInputEntity::class,
        WalletTransactionOutputEntity::class,
        WalletUtxoEntity::class,
        TransactionHealthEntity::class,
        UtxoHealthEntity::class,
        WalletHealthEntity::class
    ],
    version = 18,
    exportSchema = true
)
abstract class UtxoPocketDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        const val NAME: String = "utxopocket.db"
    }
}
