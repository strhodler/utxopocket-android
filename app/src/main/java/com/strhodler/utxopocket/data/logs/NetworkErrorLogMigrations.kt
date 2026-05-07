package com.strhodler.utxopocket.data.logs

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object NetworkErrorLogMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            if (!db.tableExists(TABLE_NAME)) {
                db.execSQL(createTableSql(TABLE_NAME))
                return
            }

            val columns = db.columnNames(TABLE_NAME)
            db.execSQL(createTableSql(TEMP_TABLE_NAME))
            db.execSQL(
                """
                INSERT INTO `$TEMP_TABLE_NAME` (${V2_COLUMNS.joinToString()})
                SELECT ${selectExpressions(columns).joinToString()}
                FROM `$TABLE_NAME`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `$TABLE_NAME`")
            db.execSQL("ALTER TABLE `$TEMP_TABLE_NAME` RENAME TO `$TABLE_NAME`")
        }
    }

    private fun SupportSQLiteDatabase.tableExists(tableName: String): Boolean =
        query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use { cursor ->
            cursor.moveToFirst()
        }

    private fun SupportSQLiteDatabase.columnNames(tableName: String): Set<String> =
        query("PRAGMA table_info(`$tableName`)").use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    add(cursor.getString(nameIndex))
                }
            }
        }

    private fun selectExpressions(columns: Set<String>): List<String> = listOf(
        columnOrDefault(columns, "id", "NULL"),
        nonNullColumnOrDefault(columns, "timestamp", "0"),
        nonNullColumnOrDefault(columns, "appVersion", "'unknown'"),
        nonNullColumnOrDefault(columns, "androidVersion", "'unknown'"),
        columnOrDefault(columns, "networkType", "NULL"),
        nonNullColumnOrDefault(columns, "operation", "'NodeConnect'"),
        nonNullColumnOrDefault(columns, "endpointType", "'Unknown'"),
        nonNullColumnOrDefault(columns, "transport", "'Unknown'"),
        columnOrDefault(columns, "hostMask", "NULL"),
        columnOrDefault(columns, "hostHash", "NULL"),
        columnOrDefault(columns, "port", "NULL"),
        nonNullColumnOrDefault(columns, "usedTor", "0"),
        columnOrDefault(columns, "torBootstrapPercent", "NULL"),
        columnOrDefault(columns, "errorKind", "NULL"),
        nonNullColumnOrDefault(columns, "errorMessage", "''"),
        columnOrDefault(columns, "durationMs", "NULL"),
        columnOrDefault(columns, "retryCount", "NULL"),
        nonNullColumnOrDefault(columns, "nodeSource", "'Unknown'")
    )

    private fun columnOrDefault(columns: Set<String>, name: String, defaultValue: String): String =
        if (name in columns) "`$name`" else defaultValue

    private fun nonNullColumnOrDefault(columns: Set<String>, name: String, defaultValue: String): String =
        if (name in columns) "COALESCE(`$name`, $defaultValue)" else defaultValue

    private fun createTableSql(tableName: String): String =
        """
        CREATE TABLE IF NOT EXISTS `$tableName` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `appVersion` TEXT NOT NULL,
            `androidVersion` TEXT NOT NULL,
            `networkType` TEXT,
            `operation` TEXT NOT NULL,
            `endpointType` TEXT NOT NULL,
            `transport` TEXT NOT NULL,
            `hostMask` TEXT,
            `hostHash` TEXT,
            `port` INTEGER,
            `usedTor` INTEGER NOT NULL,
            `torBootstrapPercent` INTEGER,
            `errorKind` TEXT,
            `errorMessage` TEXT NOT NULL,
            `durationMs` INTEGER,
            `retryCount` INTEGER,
            `nodeSource` TEXT NOT NULL
        )
        """.trimIndent()

    private const val TABLE_NAME = "network_error_logs"
    private const val TEMP_TABLE_NAME = "network_error_logs_v2"
    private val V2_COLUMNS = listOf(
        "id",
        "timestamp",
        "appVersion",
        "androidVersion",
        "networkType",
        "operation",
        "endpointType",
        "transport",
        "hostMask",
        "hostHash",
        "port",
        "usedTor",
        "torBootstrapPercent",
        "errorKind",
        "errorMessage",
        "durationMs",
        "retryCount",
        "nodeSource"
    ).map { "`$it`" }
}
