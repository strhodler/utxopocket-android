package com.strhodler.utxopocket.presentation.node

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun rememberNodeQrScanner(
    onParsed: (NodeQrParseResult) -> Unit,
    onPermissionDenied: () -> Unit,
    onInvalid: () -> Unit,
    onSuccess: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents.isNullOrBlank()) {
            onInvalid()
            return@rememberLauncherForActivityResult
        }
        when (val parsed = parseNodeQrContent(contents)) {
            is NodeQrParseResult.HostPort,
            is NodeQrParseResult.Onion -> {
                onParsed(parsed)
                onSuccess()
            }
            is NodeQrParseResult.Error -> onInvalid()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scanLauncher.launch(defaultScanOptions())
        } else {
            onPermissionDenied()
        }
    }

    return remember(context, scanLauncher, permissionLauncher) {
        {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (permissionGranted) {
                scanLauncher.launch(defaultScanOptions())
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun defaultScanOptions(): ScanOptions = ScanOptions().apply {
    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    setBeepEnabled(false)
    setBarcodeImageEnabled(false)
    setOrientationLocked(true)
}
