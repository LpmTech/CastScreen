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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.yschi.castscreen.R;
import com.yschi.castscreen.service.cast.CastBinder;
import com.yschi.castscreen.service.cast.CastService;
import com.yschi.castscreen.service.cast.CastService_;
import com.yschi.castscreen.service.discovery.DiscoveryBinder;
import com.yschi.castscreen.service.discovery.DiscoveryService;
import com.yschi.castscreen.service.discovery.DiscoveryService_;
import com.yschi.castscreen.service.discovery.events.EventDiscoveryClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static final String PREF_COMMON = "common";
    private static final String PREF_KEY_RECEIVER = "receiver";
    private static final String PREF_KEY_BITRATE = "bitrate";

    private static final int[] BITRATE_OPTIONS = {
            12288000, // 12 Mbps
            6144000, // 6 Mbps
            4096000, // 4 Mbps
            2048000, // 2 Mbps
            1024000 // 1 Mbps
    };

    private static final int REQUEST_MEDIA_PROJECTION = 100;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private Context mContext;
    private MediaProjectionManager mMediaProjectionManager;
    private TextView mReceiverTextView;

    private ListView mDiscoverListView;
    private ArrayAdapter<String> mDiscoverAdapter;
    private HashMap<String, String> mDiscoverdMap;

    private int mSelectedBitrate = BITRATE_OPTIONS[0];
    private String mReceiverIp = "";
    private int mResultCode;
    private Intent mResultData;

    private CastService mCastService;
    private DiscoveryService mDiscoveryService;

    private MainActivityServiceConnection mServiceConnection = new MainActivityServiceConnection();
    private MainActivityServiceConnection mDiscoveryServiceConnection = new MainActivityServiceConnection();

    class MainActivityServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d(TAG, "onServiceConnected " + service.getClass().getSimpleName());
            if (service instanceof DiscoveryBinder) {
                mDiscoveryService = ((DiscoveryBinder) service).getService();

                checkDiscoveryServiceIsScanning();
            } else if (service instanceof CastBinder) {
                mCastService = ((CastBinder) service).getService();

                checkRecordingService();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected, name: " + name);
            mCastService = null;
            mDiscoveryService = null;
        }
    }

    ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }

        mContext = this;
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mDiscoverdMap = new HashMap<>();
        mDiscoverListView = (ListView) findViewById(R.id.discover_listview);
        mDiscoverAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1);
        mDiscoverAdapter.addAll(mDiscoverdMap.keySet());
        mDiscoverListView.setAdapter(mDiscoverAdapter);
        mDiscoverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = mDiscoverAdapter.getItem(i);
                String ip = mDiscoverdMap.get(name);
                Log.d(TAG, "Select receiver name: " + name + ", ip: " + ip);
                mReceiverIp = ip;
                updateReceiverStatus();
                mContext.getSharedPreferences(PREF_COMMON, 0).edit().putString(PREF_KEY_RECEIVER, mReceiverIp).commit();
            }
        });

        mReceiverTextView = (TextView) findViewById(R.id.receiver_textview);

        Spinner bitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);
        ArrayAdapter<CharSequence> bitrateAdapter = ArrayAdapter.createFromResource(this,
                R.array.bitrate_options, android.R.layout.simple_spinner_item);
        bitrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bitrateSpinner.setAdapter(bitrateAdapter);
        bitrateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mSelectedBitrate = BITRATE_OPTIONS[i];
                mContext.getSharedPreferences(PREF_COMMON, 0).edit().putInt(PREF_KEY_BITRATE, i).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                mSelectedBitrate = BITRATE_OPTIONS[0];
                mContext.getSharedPreferences(PREF_COMMON, 0).edit().putInt(PREF_KEY_BITRATE, 0).commit();
            }
        });
        bitrateSpinner.setSelection(mContext.getSharedPreferences(PREF_COMMON, 0).getInt(PREF_KEY_BITRATE, 0));

        mReceiverIp = mContext.getSharedPreferences(PREF_COMMON, 0).getString(PREF_KEY_RECEIVER, "");
        updateReceiverStatus();

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
    }

    @Override
    public void onPause() {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start:
                Log.d(TAG, "==== start ==== " + mReceiverIp);
                //if (!TextUtils.isEmpty(mReceiverIp))
            {
                startCaptureScreen(true);
            }
            return true;
            case R.id.action_stop:
                Log.d(TAG, "==== stop ====");
                stopScreenCapture();
                return true;
        }

        return super.onOptionsItemSelected(item);
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
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventDiscoveryClient event) {
        if (event != null) {
            mDiscoverdMap.put(event.getName(), event.getIp());
            mDiscoverAdapter.clear();
            mDiscoverAdapter.addAll(mDiscoverdMap.keySet());
        }
    }

    private void updateReceiverStatus() {
        if (mReceiverIp.length() > 0) {
            mReceiverTextView.setText(String.format(mContext.getString(R.string.receiver), mReceiverIp));
        } else {
            mReceiverTextView.setText(R.string.no_receiver);
        }
    }

    private void startCaptureScreen() {
        startCaptureScreen(false);
    }

    private void startCaptureScreen(boolean prompt) {
        if (mResultCode != 0 && mResultData != null
                && !TextUtils.isEmpty(mReceiverIp)) {
            checkRecordingService();
            if (mCastService != null) {
                mCastService.startRecording(mReceiverIp, mResultCode,
                        mResultData, mSelectedBitrate);
                mResultData = null;
                mResultCode = 0;
            }
        } else if (prompt) {
            startActivityForResult(
                    mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
    }

    private void stopScreenCapture() {
        //if (mCastService != null) {
        mCastService.stopRecording();
        //}
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
}
