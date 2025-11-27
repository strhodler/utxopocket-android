package com.strhodler.utxopocket.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object WalletMigrations {
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE wallets ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            val nextOrderByNetwork = mutableMapOf<String, Int>()
            database.query("SELECT id, network FROM wallets ORDER BY network, name, id").use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("id")
                val networkIndex = cursor.getColumnIndexOrThrow("network")
                while (cursor.moveToNext()) {
                    val walletId = cursor.getLong(idIndex)
                    val network = cursor.getString(networkIndex)
                    val nextOrder = nextOrderByNetwork.getOrDefault(network, 0)
                    database.execSQL(
                        "UPDATE wallets SET sort_order = ? WHERE id = ?",
                        arrayOf(nextOrder, walletId)
                    )
                    nextOrderByNetwork[network] = nextOrder + 1
                }
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_16_17
    )
}
