package com.strhodler.utxopocket.data.logs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class NetworkErrorLogDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NetworkErrorLogDatabase::class.java
    )

    @Test
    fun migrationFromOneToTwoPreservesSanitizedRowsAndAcceptsNewRows() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)

        val dbFile = context.getDatabasePath(TEST_DB)
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL(CREATE_V1_TABLE_SQL)
            execSQL(
                """
                INSERT INTO `network_error_logs` (
                    `id`, `timestamp`, `operation`, `endpointType`, `transport`,
                    `hostMask`, `hostHash`, `usedTor`, `errorKind`, `errorMessage`
                ) VALUES (1, 123, 'NodeConnect', 'Onion', 'TCP', 'onion:masked', 'hash123', 1, 'IOException', 'sanitized failure')
                """.trimIndent()
            )
            version = 1
            close()
        }

        val database = Room.databaseBuilder(context, NetworkErrorLogDatabase::class.java, TEST_DB)
            .addMigrations(NetworkErrorLogMigrations.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        helper.closeWhenFinished(database)
        try {
            val dao = database.networkErrorLogDao()
            val migrated = dao.observeLogs().first().single()
            assertEquals(1L, migrated.id)
            assertEquals(123L, migrated.timestamp)
            assertEquals("unknown", migrated.appVersion)
            assertEquals("unknown", migrated.androidVersion)
            assertEquals("NodeConnect", migrated.operation)
            assertEquals("Onion", migrated.endpointType)
            assertEquals("TCP", migrated.transport)
            assertEquals("onion:masked", migrated.hostMask)
            assertEquals("hash123", migrated.hostHash)
            assertNull(migrated.port)
            assertEquals(true, migrated.usedTor)
            assertEquals("IOException", migrated.errorKind)
            assertEquals("sanitized failure", migrated.errorMessage)
            assertEquals("Unknown", migrated.nodeSource)

            dao.insert(
                NetworkErrorLogEntity(
                    timestamp = 456L,
                    appVersion = "0.12.0 (14)",
                    androidVersion = "test",
                    networkType = "wifi",
                    operation = "NodeConnect",
                    endpointType = "Onion",
                    transport = "TCP",
                    hostMask = "onion:masked2",
                    hostHash = "hash456",
                    port = null,
                    usedTor = true,
                    torBootstrapPercent = null,
                    errorKind = "IOException",
                    errorMessage = "second sanitized failure",
                    durationMs = 50L,
                    retryCount = 1,
                    nodeSource = "Preset"
                )
            )

            val logs = dao.observeLogs().first()
            assertEquals(2, logs.size)
            assertEquals(456L, logs[0].timestamp)
            assertEquals(123L, logs[1].timestamp)
        } finally {
            database.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    private companion object {
        const val TEST_DB = "network-error-log-migration-test.db"
        const val CREATE_V1_TABLE_SQL =
            """
            CREATE TABLE IF NOT EXISTS `network_error_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `operation` TEXT NOT NULL,
                `endpointType` TEXT NOT NULL,
                `transport` TEXT NOT NULL,
                `hostMask` TEXT,
                `hostHash` TEXT,
                `usedTor` INTEGER NOT NULL,
                `errorKind` TEXT,
                `errorMessage` TEXT NOT NULL
            )
            """
    }
}
