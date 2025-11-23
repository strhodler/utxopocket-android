package com.strhodler.utxopocket.presentation.wallets.labels

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletLabelExport
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

private const val LABEL_EXPORT_RETENTION_MS = 120_000L

fun shareBip329Labels(context: Context, export: WalletLabelExport): Boolean {
    return runCatching {
        val exportDir = File(context.cacheDir, "labels").apply {
            if (!exists()) mkdirs()
            listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        }
        val file = File(exportDir, export.fileName)
        BufferedWriter(FileWriter(file, false)).use { writer ->
            writer.write(export.toJsonLines())
        }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, export.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(export.fileName, uri)
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.wallet_detail_export_chooser_title)
            )
        )
        Handler(Looper.getMainLooper()).postDelayed(
            {
                runCatching {
                    context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                if (file.exists() && !file.delete()) {
                    file.deleteOnExit()
                }
            },
            LABEL_EXPORT_RETENTION_MS
        )
        true
    }.isSuccess
}

fun writeBip329Labels(context: Context, uri: Uri, export: WalletLabelExport): Boolean {
    return runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.writer(Charsets.UTF_8).use { writer ->
                writer.write(export.toJsonLines())
            }
        } ?: error("Unable to open output stream")
    }.isSuccess
}
