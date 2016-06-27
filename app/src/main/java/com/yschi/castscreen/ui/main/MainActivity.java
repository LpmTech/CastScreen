/*
 * Copyright (C) 2016 Jones Chi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yschi.castscreen.ui.main;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.yschi.castscreen.R;
import com.yschi.castscreen.common.State;
import com.yschi.castscreen.network.wifi.IWifiBroadcastListener;
import com.yschi.castscreen.network.wifi.WifiBroadcastReceiver;
import com.yschi.castscreen.service.cast.CastService_;
import com.yschi.castscreen.service.cast.ICastService;
import com.yschi.castscreen.service.cast.ICastServiceCallback;
import com.yschi.castscreen.service.discovery.DiscoveryBinder;
import com.yschi.castscreen.service.discovery.DiscoveryService;
import com.yschi.castscreen.service.discovery.DiscoveryService_;
import com.yschi.castscreen.ui.activity.AbstractBackstackProvider;
import com.yschi.castscreen.ui.activity.AbstractControllerActivity;
import com.yschi.castscreen.ui.activity.FragmentDescriptor;
import com.yschi.castscreen.ui.activity.events.OnEventPushFragment;
import com.yschi.castscreen.ui.activity.events.OnEventRecordingState;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Set;


public class MainActivity extends AbstractControllerActivity implements IWifiBroadcastListener {
    private static final String TAG = "MainActivity";

    private static final String PREF_COMMON = "common";
    private static final String PREF_KEY_RECEIVER = "receiver";
    private static final String PREF_KEY_BITRATE = "bitrate";

    private static final int[] BITRATE_OPTIONS = {
            6144000, // 6 Mbps
            4096000, // 4 Mbps
            2048000, // 2 Mbps
            1024000 // 1 Mbps
    };

    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";
    private static final String STATE_RECEIVER_IP = "receiver_ip";

    private Context mContext;
    private MediaProjectionManager mMediaProjectionManager;

    private HashMap<String, String> mDiscoverdMap;

    private int mSelectedBitrate = BITRATE_OPTIONS[0];
    private String mReceiverIp = "";
    private int mResultCode;
    private Intent mResultData;

    private ICastService mCastService;
    private DiscoveryService mDiscoveryService;

    private MainActivityServiceConnection mServiceConnection = new MainActivityServiceConnection();
    private MainActivityServiceConnection mDiscoveryServiceConnection = new MainActivityServiceConnection();
    private WifiBroadcastReceiver mWifiBroadcastReceiver = new WifiBroadcastReceiver(this);

    private ICastServiceCallback mCastServiceCallback = new ICastServiceCallback.Stub() {

        @Override
        public void onRecordingStateChange(int state) throws RemoteException {
            EventBus.getDefault().post(new OnEventRecordingState(State.from(state)));
        }
    };

    public Set<String> keySet() {
        return mDiscoverdMap.keySet();
    }

    class MainActivityServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d(TAG, "onServiceConnected " + service.getClass().getSimpleName());
            if (service instanceof DiscoveryBinder) {
                mDiscoveryService = ((DiscoveryBinder) service).getService();

                checkDiscoveryServiceIsScanning();
            } else /*if (service instanceof CastBinder)*/ {
                mCastService = ICastService.Stub.asInterface(service);

                registerToService();
                checkRecordingService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            unregisterFromService();
            Log.d(TAG, "Service disconnected, name: " + name);
            mCastService = null;
            mDiscoveryService = null;
        }
    }


    @Override
    protected AbstractBackstackProvider createAbstractBackstackProvider() {
        return new DefaultBackstackProvider();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
            mReceiverIp = savedInstanceState.getString(STATE_RECEIVER_IP);
        }

        mContext = this;
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mDiscoverdMap = new HashMap<>();

        //mReceiverIp = mContext.getSharedPreferences(PREF_COMMON, 0).getString(PREF_KEY_RECEIVER, "");

        checkRecordingService();
    }

    @Override
    public void onResume() {
        super.onResume();

        // start discovery task
        EventBus eventBus = EventBus.getDefault();
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
        checkDiscoveryServiceIsScanning();
        checkRecordingService();
        startCaptureScreen(false);

        mWifiBroadcastReceiver.registerTo(this);
        if (mWifiBroadcastReceiver.isWifiConnected(this)) {
            onWifiConnected();
        } else {
            onWifiDisconnected();
        }

        registerToService();
    }

    @Override
    public void onPause() {
        unregisterFromService();

        mWifiBroadcastReceiver.unregister(this);

        EventBus.getDefault().unregister(this);

        stopDiscovery();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "User cancelled");
                Toast.makeText(mContext, R.string.user_cancelled, Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Starting screen capture");
            mResultCode = resultCode;
            mResultData = data;
            startCaptureScreen();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResultData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mResultData);
            outState.putString(STATE_RECEIVER_IP, mReceiverIp);
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Controller Overrides
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Override
    public void setCanEdit(boolean state) {

    }

    @Override
    public void notifyCollapsingSetChanged() {

    }

    @Override
    public void setEditTextFocusListenerFor(EditText edit_text) {

    }

    @Override
    public void scrollTo(View view) {

    }

    @Override
    public int getExpandableHeight() {
        return 0;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Manage Wifi states
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    @Override
    public void onWifiConnected() {
        EventBus.getDefault().post(new OnEventPushFragment(FragmentDescriptor.MAIN, new Bundle()));
    }

    @Override
    public void onWifiDisconnected() {
        stopScreenCapture();
        EventBus.getDefault().post(new OnEventPushFragment(FragmentDescriptor.WIFI, new Bundle()));
    }

    public void setReceiverName(String name) {
        String ip = mDiscoverdMap.get(name);
        Log.d(TAG, "Select receiver name: " + name + ", ip: " + ip);
        mReceiverIp = ip;
        mContext.getSharedPreferences(PREF_COMMON, 0).edit().putString(PREF_KEY_RECEIVER, mReceiverIp).commit();
    }

    public boolean hasReceiverIpSet() {
        return !TextUtils.isEmpty(mReceiverIp);
    }

    public void put(String name, String ip) {
        mDiscoverdMap.put(name, ip);
    }

    private void startCaptureScreen() {
        startCaptureScreen(false);
    }

    public void startCaptureScreen(boolean prompt) {
        if (mWifiBroadcastReceiver.isWifiConnected(this)) {
            if (mResultCode != 0 && mResultData != null
                    && !TextUtils.isEmpty(mReceiverIp)) {
                checkRecordingService();
                if (mCastService != null) {
                    try {
                        mCastService.startRecording(mReceiverIp, mResultCode,
                                mResultData, mSelectedBitrate);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mResultData = null;
                    mResultCode = 0;
                }
            } else if (prompt) {
                startActivityForResult(
                        mMediaProjectionManager.createScreenCaptureIntent(),
                        REQUEST_MEDIA_PROJECTION);
            }
        }
    }

    public void stopScreenCapture() {
        if (mCastService != null) {
            try {
                mCastService.stopRecording();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRecording() {
        boolean state = false;

        if(mCastService != null){
            try {
                state = mCastService.isRecording();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return state;
    }

    private void doUnbindService() {
        if (mCastService != null) {
            mCastService = null;
            unbindService(mServiceConnection);
        }
    }

    private void checkRecordingService() {
        if (mCastService == null) {
            Log.d(TAG, "checkRecordingService");
            Intent intent = new Intent(this, CastService_.class);
            startService(intent);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void checkDiscoveryServiceIsScanning() {
        if (mDiscoveryService != null) {
            mDiscoveryService.startDiscovery();
        } else {
            Intent discovery_intent = new Intent(this, DiscoveryService_.class);
            startService(discovery_intent);
            bindService(discovery_intent, mDiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void stopDiscovery() {
        try {
            if (mDiscoveryService != null) {
                mDiscoveryService.stopDiscovery();
            }
            unbindService(mDiscoveryServiceConnection);
        } catch (Exception e) {

        }
    }

    private void registerToService() {
        if (mCastService != null) {
            try {
                mCastService.registerCallback(mCastServiceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void unregisterFromService() {
        if (mCastService != null) {
            try {
                mCastService.unregisterCallback();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
