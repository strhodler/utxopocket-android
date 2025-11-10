package com.strhodler.utxopocket.tor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class TorActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, TorForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
