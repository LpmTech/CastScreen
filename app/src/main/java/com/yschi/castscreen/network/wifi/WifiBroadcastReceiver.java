package com.yschi.castscreen.network.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by kevinleperf on 27/06/2016.
 */

public class WifiBroadcastReceiver extends BroadcastReceiver {

    private final static String TAG = WifiBroadcastReceiver.class.getSimpleName();
    private IWifiBroadcastListener mWifiBroadcastListener;

    private WifiBroadcastReceiver() {

    }

    public WifiBroadcastReceiver(IWifiBroadcastListener wifiBroadcastListener) {
        this();
        mWifiBroadcastListener = wifiBroadcastListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        final String action = intent.getAction();
        boolean wifi = false;
        Log.d(TAG, "onReceive: " + WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
            if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                wifi = true;
            }
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            if (isWifiConnected(context)) {
                wifi = true;
            }
        }

        wifi = isWifiConnected(context);
        if (wifi) {
            mWifiBroadcastListener.onWifiConnected();
        } else {
            mWifiBroadcastListener.onWifiDisconnected();
        }
    }

    public void registerTo(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, intentFilter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

    public boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        Log.d(TAG, "isWifiConnected: " + wifi.isConnected());
        return wifi.isConnected();
    }
}
