package com.strhodler.utxopocket.data.bdk

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.strhodler.utxopocket.common.logging.SecureLog
import com.strhodler.utxopocket.data.security.TinkCrypto
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.jvm.Volatile
import kotlin.text.Charsets
import javax.inject.Inject
import javax.inject.Singleton

interface WalletStorage {
    fun connectionPath(walletId: Long, network: BitcoinNetwork): String
    fun seal(connectionPath: String)
    fun remove(walletId: Long, network: BitcoinNetwork)
    fun materializationState(connectionPath: String): WalletMaterializationState?
}

enum class WalletMaterializationSource {
    ENCRYPTED_BUNDLE,
    LEGACY_PLAINTEXT,
    EMPTY
}

data class WalletMaterializationState(
    val source: WalletMaterializationSource
) {
    val isFresh: Boolean
        get() = source == WalletMaterializationSource.EMPTY
}

@Singleton
class DefaultWalletStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tinkCrypto: TinkCrypto
) : WalletStorage {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sessions = ConcurrentHashMap<String, WalletSession>()

    override fun connectionPath(walletId: Long, network: BitcoinNetwork): String {
        val key = sessionKey(walletId, network)
        val session = sessions[key] ?: createSession(walletId, network).also { sessions[key] = it }
        session.openCount.incrementAndGet()
        synchronized(session.lock) {
            if (!session.materialized.get()) {
                materializeSession(session)
                session.materialized.set(true)
            }
        }
        return session.databaseFile.absolutePath
    }

    override fun seal(connectionPath: String) {
        val session = sessions[connectionPath] ?: return
        if (session.openCount.decrementAndGet() > 0) {
            return
        }
        synchronized(session.lock) {
            persistSession(session)
            session.materialized.set(false)
            sessions.remove(connectionPath)
        }
    }

    override fun remove(walletId: Long, network: BitcoinNetwork) {
        val sessionPath = sessionKey(walletId, network)
        sessions.remove(sessionPath)?.let { session ->
            synchronized(session.lock) {
                session.openCount.set(0)
                cleanWorkingDir(session)
            }
        }
        val encryptedBundle = encryptedBundleFile(walletId, network)
        if (encryptedBundle.exists()) {
            encryptedBundle.delete()
        }
        val legacyBase = legacyBaseFile(walletId, network)
        listOf(
            legacyBase,
            File("${legacyBase.absolutePath}-wal"),
            File("${legacyBase.absolutePath}-shm")
        ).forEach { file ->
            if (file.exists()) {
                runCatching { file.delete() }
            }
        }
        val legacyDir = legacyBase.parentFile
        if (legacyDir?.exists() == true && legacyDir.list()?.isEmpty() == true) {
            runCatching { legacyDir.delete() }
        }
        workingDir(walletId, network).deleteRecursively()
    }

    private fun materializeSession(session: WalletSession) {
        cleanWorkingDir(session)
        session.workingDir.mkdirs()
        val source = when {
            session.encryptedBundle.exists() -> {
                decryptBundle(session)
                WalletMaterializationSource.ENCRYPTED_BUNDLE
            }
            session.legacyBase.exists() -> {
                migrateLegacyPlaintext(session)
                WalletMaterializationSource.LEGACY_PLAINTEXT
            }
            else -> {
                ensureFileExists(session.databaseFile)
                WalletMaterializationSource.EMPTY
            }
        }
        session.materializationState = WalletMaterializationState(source)
    }

    private fun persistSession(session: WalletSession) {
        if (!session.workingDir.exists()) return
        val encrypted = encryptBundle(session)
        if (!encrypted) return
        listOf(session.databaseFile, session.walFile, session.shmFile).forEach { secureDelete(it) }
        session.workingDir.deleteRecursively()
        listOf(
            session.legacyBase,
            File("${session.legacyBase.absolutePath}-wal"),
            File("${session.legacyBase.absolutePath}-shm")
        ).forEach { legacy ->
            if (legacy.exists()) {
                legacy.delete()
            }
        }
    }

    private fun decryptBundle(session: WalletSession) {
        val tinkSucceeded = decryptBundleWithTink(session)
        if (!tinkSucceeded) {
            decryptBundleWithLegacy(session)
        }
        ensureFileExists(session.databaseFile)
    }

    private fun decryptBundleWithTink(session: WalletSession): Boolean {
        val streamingAead = tinkCrypto.streamingAeadOrNull() ?: return false
        return runCatching {
            FileInputStream(session.encryptedBundle).use { input ->
                streamingAead.newDecryptingStream(input, associatedData(session)).use { cipherIn ->
                    ZipInputStream(BufferedInputStream(cipherIn)).use { zip ->
                        unzipEntries(session, zip)
                    }
                }
            }
        }.onSuccess {
            SecureLog.d(TAG) { "Decrypted bundle with Tink for wallet=${session.walletId} network=${session.network}" }
        }.onFailure {
            SecureLog.w(TAG, it) { "Failed to decrypt bundle with Tink for wallet=${session.walletId} network=${session.network}" }
        }.isSuccess
    }

    private fun decryptBundleWithLegacy(session: WalletSession) {
        val encryptedFile = EncryptedFile.Builder(
            context,
            session.encryptedBundle,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileInput().use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                unzipEntries(session, zip)
            }
        }
    }

    private fun encryptBundle(session: WalletSession): Boolean {
        if (encryptBundleWithTink(session)) return true
        return encryptBundleWithLegacy(session)
    }

    private fun encryptBundleWithTink(session: WalletSession): Boolean {
        val streamingAead = tinkCrypto.streamingAeadOrNull() ?: return false
        val tempFile = File("${session.encryptedBundle.absolutePath}.tmp")
        return runCatching {
            tempFile.parentFile?.mkdirs()
            FileOutputStream(tempFile).use { raw ->
                streamingAead.newEncryptingStream(raw, associatedData(session)).use { cipherOut ->
                    ZipOutputStream(BufferedOutputStream(cipherOut)).use { zip ->
                        writeEntry(zip, session.databaseFile, DB_ENTRY)
                        writeEntry(zip, session.walFile, WAL_ENTRY)
                        writeEntry(zip, session.shmFile, SHM_ENTRY)
                    }
                }
            }
            if (session.encryptedBundle.exists()) {
                session.encryptedBundle.delete()
            }
            if (!tempFile.renameTo(session.encryptedBundle)) {
                throw IllegalStateException("Unable to move encrypted bundle into place")
            }
        }.onSuccess {
            SecureLog.d(TAG) { "Encrypted bundle with Tink for wallet=${session.walletId} network=${session.network}" }
        }.onFailure {
            tempFile.delete()
            SecureLog.w(TAG, it) { "Failed to encrypt bundle with Tink for wallet=${session.walletId} network=${session.network}" }
        }.isSuccess
    }

    private fun encryptBundleWithLegacy(session: WalletSession): Boolean =
        runCatching {
            if (session.encryptedBundle.exists()) {
                session.encryptedBundle.delete()
            }
            val encryptedFile = EncryptedFile.Builder(
                context,
                session.encryptedBundle,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput().use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                    writeEntry(zip, session.databaseFile, DB_ENTRY)
                    writeEntry(zip, session.walFile, WAL_ENTRY)
                    writeEntry(zip, session.shmFile, SHM_ENTRY)
                }
            }
        }.onSuccess {
            SecureLog.d(TAG) { "Encrypted bundle with legacy EncryptedFile for wallet=${session.walletId} network=${session.network}" }
        }.onFailure {
            SecureLog.w(TAG, it) { "Failed to encrypt bundle with legacy EncryptedFile for wallet=${session.walletId} network=${session.network}" }
        }.isSuccess

    private fun unzipEntries(session: WalletSession, zip: ZipInputStream) {
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
            val target = when (entry.name) {
                DB_ENTRY -> session.databaseFile
                WAL_ENTRY -> session.walFile
                SHM_ENTRY -> session.shmFile
                else -> null
            }
            if (target != null) {
                target.parentFile?.mkdirs()
                FileOutputStream(target).use { output ->
                    zip.copyTo(output)
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    private fun associatedData(session: WalletSession): ByteArray =
        "wallet-${session.walletId}-${session.network.name}".toByteArray(Charsets.UTF_8)

    private fun migrateLegacyPlaintext(session: WalletSession) {
        ensureFileExists(session.databaseFile)
        listOf(
            session.legacyBase to session.databaseFile,
            File("${session.legacyBase.absolutePath}-wal") to session.walFile,
            File("${session.legacyBase.absolutePath}-shm") to session.shmFile
        ).forEach { (legacy, target) ->
            if (legacy.exists()) {
                legacy.copyTo(target, overwrite = true)
            }
        }
    }

    private fun cleanWorkingDir(session: WalletSession) {
        if (session.workingDir.exists()) {
            session.workingDir.deleteRecursively()
        }
    }

    override fun materializationState(connectionPath: String): WalletMaterializationState? =
        sessions[connectionPath]?.materializationState

    private fun writeEntry(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        FileInputStream(file).use { input ->
            zip.putNextEntry(ZipEntry(entryName))
            input.copyTo(zip)
            zip.closeEntry()
        }
    }

    private fun secureDelete(file: File) {
        if (!file.exists()) return
        runCatching {
            RandomAccessFile(file, "rws").use { raf ->
                val total = raf.length()
                raf.seek(0)
                val buffer = ByteArray(8192)
                var remaining = total
                while (remaining > 0) {
                    val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                    raf.write(buffer, 0, toWrite)
                    remaining -= toWrite
                }
                raf.fd.sync()
            }
        }
        runCatching { file.delete() }
    }

    private fun ensureFileExists(file: File) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
    }

    private fun sessionKey(walletId: Long, network: BitcoinNetwork): String {
        return File(workingDir(walletId, network), walletFilename(walletId)).absolutePath
    }

    private fun createSession(walletId: Long, network: BitcoinNetwork): WalletSession {
        val databaseFile = File(workingDir(walletId, network), walletFilename(walletId))
        return WalletSession(
            walletId = walletId,
            network = network,
            encryptedBundle = encryptedBundleFile(walletId, network),
            workingDir = databaseFile.parentFile!!,
            databaseFile = databaseFile,
            walFile = File("${databaseFile.absolutePath}-wal"),
            shmFile = File("${databaseFile.absolutePath}-shm"),
            legacyBase = legacyBaseFile(walletId, network),
            lock = Any()
        )
    }

    private fun encryptedBundleFile(walletId: Long, network: BitcoinNetwork): File {
        val dir = File(baseDir(), networkDirectory(network))
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${walletFilename(walletId)}$ENCRYPTED_BUNDLE_SUFFIX")
    }

    private fun legacyBaseFile(walletId: Long, network: BitcoinNetwork): File {
        val dir = File(baseDir(), networkDirectory(network))
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, walletFilename(walletId))
    }

    private fun workingDir(walletId: Long, network: BitcoinNetwork): File {
        val dir = File(context.cacheDir, "bdk-working/${networkDirectory(network)}/$walletId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun baseDir(): File {
        val databaseDir = context.getDatabasePath(DATABASE_PLACEHOLDER).parentFile
        val root = databaseDir ?: File(context.filesDir, "bdk")
        val directory = File(root, "wallets")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun networkDirectory(network: BitcoinNetwork): String =
        network.name.lowercase(Locale.US)

    private fun walletFilename(walletId: Long): String = "$walletId.sqlite"

    private data class WalletSession(
        val walletId: Long,
        val network: BitcoinNetwork,
        val encryptedBundle: File,
        val workingDir: File,
        val databaseFile: File,
        val walFile: File,
        val shmFile: File,
        val legacyBase: File,
        val lock: Any,
        val openCount: AtomicInteger = AtomicInteger(0),
        val materialized: AtomicBoolean = AtomicBoolean(false),
        @Volatile var materializationState: WalletMaterializationState? = null
    )

    private companion object {
        private const val DATABASE_PLACEHOLDER = "bdk-wallets-placeholder"
        private const val ENCRYPTED_BUNDLE_SUFFIX = ".enc"
        private const val DB_ENTRY = "db"
        private const val WAL_ENTRY = "wal"
        private const val SHM_ENTRY = "shm"
        private const val TAG = "WalletStorage"
    }
}
