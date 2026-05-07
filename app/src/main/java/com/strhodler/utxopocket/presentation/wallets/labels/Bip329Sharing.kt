package com.strhodler.utxopocket.presentation.wallets.labels

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LABEL_EXPORT_RETENTION_MS = 120_000L

// Share grants can outlive the export route, so cleanup is scheduled outside composition.
private val labelExportCleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

suspend fun shareBip329Labels(context: Context, export: WalletLabelExport): Boolean {
    val shareFile = try {
        prepareBip329ShareFile(context, export)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        return false
    }

    return try {
        withContext(Dispatchers.Main.immediate) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, shareFile.uri)
                putExtra(Intent.EXTRA_TITLE, export.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newRawUri(export.fileName, shareFile.uri)
            }
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.wallet_detail_export_chooser_title)
                )
            )
        }
        scheduleBip329ShareCleanup(context, shareFile)
        true
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        deleteBip329ShareFile(shareFile.file)
        false
    }
}

suspend fun writeBip329Labels(context: Context, uri: Uri, export: WalletLabelExport): Boolean {
    return try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.writer(Charsets.UTF_8).use { writer ->
                    writer.write(export.toJsonLines())
                }
            } ?: error("Unable to open output stream")
        }
        true
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        false
    }
}

private data class Bip329ShareFile(
    val file: File,
    val uri: Uri
)

private suspend fun prepareBip329ShareFile(
    context: Context,
    export: WalletLabelExport
): Bip329ShareFile = withContext(Dispatchers.IO) {
    val exportDir = File(context.cacheDir, "labels").apply {
        if (!exists()) mkdirs()
        listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }
    val file = File(exportDir, export.fileName)
    BufferedWriter(FileWriter(file, false)).use { writer ->
        writer.write(export.toJsonLines())
    }
    val authority = "${context.packageName}.fileprovider"
    Bip329ShareFile(
        file = file,
        uri = FileProvider.getUriForFile(context, authority, file)
    )
}

private fun scheduleBip329ShareCleanup(context: Context, shareFile: Bip329ShareFile) {
    val cleanupContext = context.applicationContext
    labelExportCleanupScope.launch {
        delay(LABEL_EXPORT_RETENTION_MS)
        withContext(Dispatchers.Main.immediate) {
            try {
                cleanupContext.revokeUriPermission(shareFile.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
        }
        deleteBip329ShareFile(shareFile.file)
    }
}

private suspend fun deleteBip329ShareFile(file: File) {
    withContext(Dispatchers.IO) {
        try {
            if (file.exists() && !file.delete()) {
                file.deleteOnExit()
            }
        } catch (_: Exception) {
        }
    }
}
