package com.strhodler.utxopocket.presentation.more

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.components.DismissibleSnackbarHost
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfViewerRoute(onBack: () -> Unit) {
    PdfViewerScreen(onBack = onBack)
}

@Composable
private fun PdfViewerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showSnackbar = remember(coroutineScope, snackbarHostState) {
        { message: String, duration: SnackbarDuration ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = duration,
                    withDismissAction = true
                )
            }
        }
    }
    val documentState by produceState<PdfDocumentState>(initialValue = PdfDocumentState.Loading, key1 = context) {
        value = runCatching { PdfDocumentState.Ready(preparePdfDocument(context)) }
            .getOrElse { error -> PdfDocumentState.Error(error.message ?: context.getString(R.string.pdf_viewer_error)) }
    }
    val cacheFile = (documentState as? PdfDocumentState.Ready)?.file

    SetSecondaryTopBar(
        title = stringResource(id = R.string.pdf_viewer_title),
        onBackClick = onBack,
        actions = {
            if (cacheFile != null) {
                IconButton(onClick = {
                    cacheFile?.let { file ->
                        shareOrDownloadPdf(context, file)?.let { message ->
                            showSnackbar(message.text, message.duration)
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = stringResource(id = R.string.pdf_download_action))
                }
            }
        }
    )

    Scaffold(
        snackbarHost = { DismissibleSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        when (val current = documentState) {
            PdfDocumentState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = stringResource(id = R.string.pdf_viewer_loading))
                    }
                }
            }

            is PdfDocumentState.Ready -> {
                var renderedFile by remember { mutableStateOf<File?>(null) }
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    factory = { ctx ->
                        PDFView(ctx, null).apply {
                            setBackgroundColor(Color.TRANSPARENT)
                        }.also { pdfView ->
                            renderedFile = current.file
                            pdfView.fromFile(current.file)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .pageSnap(true)
                                .pageFling(true)
                                .spacing(12)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .load()
                        }
                    },
                    update = { pdfView ->
                        if (renderedFile != current.file) {
                            renderedFile = current.file
                            pdfView.fromFile(current.file)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .enableDoubletap(true)
                                .pageSnap(true)
                                .pageFling(true)
                                .spacing(12)
                                .pageFitPolicy(FitPolicy.WIDTH)
                                .load()
                        }
                    }
                )
            }

            is PdfDocumentState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .applyScreenPadding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private sealed interface PdfDocumentState {
    data object Loading : PdfDocumentState
    data class Ready(val file: File) : PdfDocumentState
    data class Error(val message: String) : PdfDocumentState
}

private data class SnackbarMessageData(
    val text: String,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

private suspend fun preparePdfDocument(context: Context): File {
    return withContext(Dispatchers.IO) {
        val cacheFile = File(context.cacheDir, "bitcoin.pdf")
        if (!cacheFile.exists()) {
            context.resources.openRawResource(R.raw.bitcoin).use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        cacheFile
    }
}

private fun shareOrDownloadPdf(context: Context, file: File): SnackbarMessageData? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val displayName = context.getString(R.string.pdf_download_filename)
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/UtxoPocket"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        val resolver = context.contentResolver
        val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (targetUri != null) {
            val saveResult = runCatching {
                resolver.openOutputStream(targetUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)
            }.onFailure {
                resolver.delete(targetUri, null, null)
            }
            if (saveResult.isSuccess) {
                return SnackbarMessageData(
                    text = context.getString(R.string.pdf_download_saved, "Downloads/UtxoPocket"),
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_download_action)))
        null
    }.getOrElse {
        SnackbarMessageData(
            text = context.getString(R.string.pdf_viewer_error),
            duration = SnackbarDuration.Long
        )
    }
}
