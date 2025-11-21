package com.strhodler.utxopocket.presentation.common

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * ZXing capture activity that forces portrait orientation so the scanner UI
 * matches the app's vertical layout expectations.
 */
class PortraitCaptureActivity : CaptureActivity()
