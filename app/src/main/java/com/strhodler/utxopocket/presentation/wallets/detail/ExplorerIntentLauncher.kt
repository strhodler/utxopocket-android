package com.strhodler.utxopocket.presentation.wallets.detail

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.strhodler.utxopocket.domain.model.BlockExplorerBucket

private val PREFERRED_TOR_PACKAGES = listOf("org.torproject.torbrowser", "org.torproject.android")

internal fun openExplorerUri(context: Context, option: BlockExplorerOption) {
    val baseIntent = Intent(Intent.ACTION_VIEW, option.url.toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (option.bucket == BlockExplorerBucket.ONION) {
        PREFERRED_TOR_PACKAGES.forEach { pkg ->
            val torIntent = Intent(baseIntent).apply { `package` = pkg }
            val launched = runCatching { context.startActivity(torIntent) }.isSuccess
            if (launched) {
                return
            }
        }
    }
    val chooser = Intent.createChooser(baseIntent, null).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}
