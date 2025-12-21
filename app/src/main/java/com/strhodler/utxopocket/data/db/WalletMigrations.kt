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
                        arrayOf<Any?>(nextOrder.toLong(), walletId)
                    )
                    nextOrderByNetwork[network] = nextOrder + 1
                }
            }
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_session_id TEXT")
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_tip_height INTEGER")
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_tip_hash TEXT")
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_applied INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_started_at INTEGER")
            database.execSQL("ALTER TABLE wallets ADD COLUMN sync_completed_at INTEGER")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE wallets ADD COLUMN last_active_external_index INTEGER")
            database.execSQL("ALTER TABLE wallets ADD COLUMN last_active_change_index INTEGER")
            database.execSQL("ALTER TABLE wallet_transaction_outputs ADD COLUMN derivation_index INTEGER")
        }
    }

    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("DROP TABLE IF EXISTS transaction_health")
            database.execSQL("DROP TABLE IF EXISTS utxo_health")
            database.execSQL("DROP TABLE IF EXISTS wallet_health")
        }
    }

    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wallet_label_pending (
                    wallet_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    ref TEXT NOT NULL,
                    label TEXT,
                    spendable INTEGER,
                    has_spendable INTEGER NOT NULL,
                    key_path TEXT NOT NULL,
                    overwrite_existing INTEGER NOT NULL,
                    PRIMARY KEY(wallet_id, type, ref, key_path)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_wallet_label_pending_wallet_id
                ON wallet_label_pending(wallet_id)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            if (!database.hasTable("wallet_label_pending")) {
                // Table missing (fresh install or previous version); create it with the final schema.
                MIGRATION_20_21.migrate(database)
                return
            }

            val hasOverwriteColumn = database.hasColumn("wallet_label_pending", "overwrite_existing")
            database.execSQL("DROP TABLE IF EXISTS wallet_label_pending_new")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wallet_label_pending_new (
                    wallet_id INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    ref TEXT NOT NULL,
                    label TEXT,
                    spendable INTEGER,
                    has_spendable INTEGER NOT NULL,
                    key_path TEXT NOT NULL,
                    overwrite_existing INTEGER NOT NULL,
                    PRIMARY KEY(wallet_id, type, ref, key_path)
                )
                """.trimIndent()
            )

            val insertSql = if (hasOverwriteColumn) {
                """
                INSERT INTO wallet_label_pending_new (
                    wallet_id, type, ref, label, spendable, has_spendable, key_path, overwrite_existing
                )
                SELECT wallet_id, type, ref, label, spendable, COALESCE(has_spendable, 0), COALESCE(key_path, ''), overwrite_existing
                FROM wallet_label_pending
                """.trimIndent()
            } else {
                """
                INSERT INTO wallet_label_pending_new (
                    wallet_id, type, ref, label, spendable, has_spendable, key_path, overwrite_existing
                )
                SELECT wallet_id, type, ref, label, spendable, COALESCE(has_spendable, 0), COALESCE(key_path, ''), 1
                FROM wallet_label_pending
                """.trimIndent()
            }
            database.execSQL(insertSql)
            database.execSQL("DROP TABLE wallet_label_pending")
            database.execSQL("ALTER TABLE wallet_label_pending_new RENAME TO wallet_label_pending")
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_wallet_label_pending_wallet_id
                ON wallet_label_pending(wallet_id)
                """.trimIndent()
            )
        }
    }

    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS utxo_collections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    wallet_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    color_key TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_utxo_collections_wallet_id_name
                ON utxo_collections(wallet_id, name)
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_utxo_collections_wallet_id
                ON utxo_collections(wallet_id)
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS utxo_collection_memberships (
                    wallet_id INTEGER NOT NULL,
                    txid TEXT NOT NULL,
                    vout INTEGER NOT NULL,
                    collection_id INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    PRIMARY KEY(wallet_id, txid, vout),
                    FOREIGN KEY(collection_id) REFERENCES utxo_collections(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_utxo_collection_memberships_wallet_id
                ON utxo_collection_memberships(wallet_id)
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_utxo_collection_memberships_collection_id
                ON utxo_collection_memberships(collection_id)
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS utxo_canvas_items (
                    wallet_id INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    ref_id TEXT NOT NULL,
                    position_index INTEGER NOT NULL,
                    PRIMARY KEY(wallet_id, item_type, ref_id)
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_utxo_canvas_items_wallet_id
                ON utxo_canvas_items(wallet_id)
                """.trimIndent()
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_16_17,
        MIGRATION_17_18,
        MIGRATION_18_19,
        MIGRATION_19_20,
        MIGRATION_20_21,
        MIGRATION_21_22,
        MIGRATION_22_23
    )
}

private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        if (nameIndex == -1) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                return true
            }
        }
    }
    return false
}

private fun SupportSQLiteDatabase.hasTable(table: String): Boolean {
    query(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(table)
    ).use { cursor ->
        return cursor.moveToFirst()
    }
}
