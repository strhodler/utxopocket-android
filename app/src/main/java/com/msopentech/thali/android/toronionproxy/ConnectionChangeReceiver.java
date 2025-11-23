package com.msopentech.thali.android.toronionproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.strhodler.utxopocket.common.logging.SecureLog;

public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static final String TAG = ConnectionChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean online = isConnected(context);
        NetworkManager.getInstance()
                .onlineObserver()
                .onNext(online);
        SecureLog.i(TAG, String.format("Switching to %s mode", online ? "ONLINE" : "OFFLINE"));
    }

    private static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

}
