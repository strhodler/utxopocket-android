package com.strhodler.utxopocket.presentation.common

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.client.android.Intents
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.sparrowwallet.hummingbird.ResultType
import com.sparrowwallet.hummingbird.UR
import com.sparrowwallet.hummingbird.URDecoder
import com.strhodler.utxopocket.R

class UrMultiPartScanActivity : AppCompatActivity() {
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var progressText: TextView
    private lateinit var hintText: TextView

    private var decoder = URDecoder()

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
        if (!contents.startsWith(UR.UR_PREFIX, ignoreCase = true)) {
            finishWith(contents)
            return@BarcodeCallback
        }

        val accepted = decoder.receivePart(contents)
        if (!accepted) {
            hintText.text = getString(R.string.wallet_labels_import_scan_error)
            return@BarcodeCallback
        }

        updateStatus()
        val state = decoder.result ?: return@BarcodeCallback
        when (state.type) {
            ResultType.SUCCESS -> finishWith(state.ur.toString())
            ResultType.FAILURE -> {
                hintText.text = state.error ?: getString(R.string.wallet_labels_import_scan_error)
                decoder = URDecoder()
                updateStatus()
            }
        }
    }

    private fun finishWith(contents: String) {
        val intent = Intent().apply {
            putExtra(Intents.Scan.RESULT, contents)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun updateStatus() {
        val expected = decoder.expectedPartCount
        val received = decoder.receivedPartIndexes.size.coerceAtLeast(0)
        val total = if (expected > 0) expected.toString() else "?"
        progressText.text = getString(R.string.wallet_labels_import_scan_frames, received, total)
        hintText.text = getString(R.string.wallet_labels_import_scan_hint)
    }
}
