package com.strhodler.utxopocket.presentation.common

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.qr.bbqr.BBQRDecoder

class UrMultiPartScanActivity : AppCompatActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var progressText: TextView
    private lateinit var hintText: TextView

    private var urDecoder = URDecoder()
    private var bbqrDecoder = BBQRDecoder()
    private var currentMode: ScanMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ur_scan)

        barcodeView = findViewById(R.id.barcode_scanner)
        progressText = findViewById(R.id.progress_text)
        hintText = findViewById(R.id.hint_text)

        barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        barcodeView.statusView.visibility = View.GONE
        updateStatus()

        barcodeView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        barcodeView.pause()
        super.onPause()
    }

    private val callback = BarcodeCallback { result: BarcodeResult ->
        val contents = result.text ?: return@BarcodeCallback
        when {
            contents.startsWith(UR.UR_PREFIX, ignoreCase = true) -> handleUr(contents)
            BBQRDecoder.isBBQRFragment(contents) -> handleBbqr(contents)
            else -> finishWith(contents)
        }
    }

    private fun handleUr(contents: String) {
        prepareForMode(ScanMode.UR)
        val accepted = urDecoder.receivePart(contents)
        if (!accepted) {
            hintText.text = getString(R.string.wallet_labels_import_scan_error)
            return
        }

        updateStatus()
        val state = urDecoder.result ?: return
        when (state.type) {
            ResultType.SUCCESS -> finishWith(state.ur.toString())
            ResultType.FAILURE -> {
                hintText.text = state.error ?: getString(R.string.wallet_labels_import_scan_error)
                urDecoder = URDecoder()
                updateStatus()
            }
        }
    }

    private fun handleBbqr(contents: String) {
        prepareForMode(ScanMode.BBQR)
        val accepted = bbqrDecoder.receivePart(contents)
        if (!accepted) {
            hintText.text = getString(R.string.wallet_labels_import_scan_error)
            return
        }

        updateStatus()
        val state = bbqrDecoder.result() ?: return
        if (state.isSuccess) {
            val payload = state.text ?: state.data?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            if (payload != null) {
                finishWith(payload)
            } else {
                hintText.text = getString(R.string.wallet_labels_import_scan_error)
                bbqrDecoder = BBQRDecoder()
                updateStatus()
            }
        } else {
            hintText.text = state.error ?: getString(R.string.wallet_labels_import_scan_error)
            bbqrDecoder = BBQRDecoder()
            updateStatus()
        }
    }

    private fun finishWith(contents: String) {
        val intent = Intent().apply {
            putExtra(Intents.Scan.RESULT, contents)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun prepareForMode(mode: ScanMode) {
        if (currentMode != mode) {
            urDecoder = URDecoder()
            bbqrDecoder = BBQRDecoder()
            currentMode = mode
        }
    }

    private fun updateStatus() {
        when (currentMode) {
            ScanMode.BBQR -> {
                val percent = (bbqrDecoder.percentComplete() * 100).toInt().coerceIn(0, 100)
                progressText.text = getString(R.string.wallet_labels_import_scan_progress, percent)
                hintText.text = getString(R.string.wallet_labels_import_scan_hint)
            }

            else -> {
                val expected = urDecoder.expectedPartCount
                val received = urDecoder.receivedPartIndexes.size.coerceAtLeast(0)
                val total = if (expected > 0) expected.toString() else "?"
                progressText.text = getString(R.string.wallet_labels_import_scan_frames, received, total)
                hintText.text = getString(R.string.wallet_labels_import_scan_hint)
            }
        }
    }

    private enum class ScanMode {
        UR, BBQR
    }
}
