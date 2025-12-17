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
        PendingBip329LabelEntity::class
    ],
    version = 22,
    exportSchema = true
)
abstract class UtxoPocketDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        const val NAME: String = "utxopocket.db"
    }
}
