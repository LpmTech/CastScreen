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

package com.yschi.castscreen.service.cast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.yschi.castscreen.Common;
import com.yschi.castscreen.IvfWriter;
import com.yschi.castscreen.R;
import com.yschi.castscreen.common.State;
import com.yschi.castscreen.service.cast.managers.DefaultCast;
import com.yschi.castscreen.service.cast.writers.IWriterWrapper;
import com.yschi.castscreen.service.cast.writers.IvfWriterWrapper;
import com.yschi.castscreen.service.cast.writers.SocketOutputWrapper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

@EService
public class CastService extends Service {
    private final String TAG = "CastService";
    private final int NT_ID_CASTING = 0;
    private Handler mHandler = new Handler(new ServiceHandlerCallback());
    private Messenger mMessenger = new Messenger(mHandler);
    private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private IntentFilter mBroadcastIntentFilter;

    private static final String HTTP_MESSAGE_TEMPLATE = "POST /api/v1/h264 HTTP/1.1\r\n" +
            "Connection: close\r\n" +
            "X-WIDTH: %1$d\r\n" +
            "X-HEIGHT: %2$d\r\n" +
            "\r\n";

    // 1280x720@25
    private static final byte[] H264_PREDEFINED_HEADER_1280x720 = {
            (byte) 0x21, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x67, (byte) 0x42, (byte) 0x80, (byte) 0x20, (byte) 0xda, (byte) 0x01, (byte) 0x40, (byte) 0x16,
            (byte) 0xe8, (byte) 0x06, (byte) 0xd0, (byte) 0xa1, (byte) 0x35, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x68, (byte) 0xce, (byte) 0x06, (byte) 0xe2, (byte) 0x32, (byte) 0x24, (byte) 0x00,
            (byte) 0x00, (byte) 0x7a, (byte) 0x83, (byte) 0x3d, (byte) 0xae, (byte) 0x37, (byte) 0x00, (byte) 0x00};

    // 800x480@25
    private static final byte[] H264_PREDEFINED_HEADER_800x480 = {
            (byte) 0x21, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x67, (byte) 0x42, (byte) 0x80, (byte) 0x20, (byte) 0xda, (byte) 0x03, (byte) 0x20, (byte) 0xf6,
            (byte) 0x80, (byte) 0x6d, (byte) 0x0a, (byte) 0x13, (byte) 0x50, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x68, (byte) 0xce, (byte) 0x06, (byte) 0xe2, (byte) 0x32, (byte) 0x24, (byte) 0x00,
            (byte) 0x00, (byte) 0x7a, (byte) 0x83, (byte) 0x3d, (byte) 0xae, (byte) 0x37, (byte) 0x00, (byte) 0x00};

    private MediaProjectionManager mMediaProjectionManager;
    private String mReceiverIp;
    private int mResultCode;
    private Intent mResultData;
    private String mSelectedFormat;
    private int mSelectedWidth;
    private int mSelectedHeight;
    private int mSelectedDpi;
    private int mSelectedBitrate;
    //private boolean mMuxerStarted = false;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mInputSurface;
    //private MediaMuxer mMuxer;
    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    //private int mTrackIndex = -1;
    private ServerSocket mServerSocket;
    private Socket mSocket;
    private IvfWriter mIvfWriter;

    private State mState = State.STOP;
    //private Handler mDrainHandler = new Handler();
    private Runnable mStartEncodingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!startScreenCapture()) {
                Log.e(TAG, "Failed to start capturing screen");
            }
        }
    };
    private int _screen_width;
    private int _screen_height;
    private int _screen_dpi;
    private DefaultCast mCastManager;
    private IWriterWrapper mWriterWrapper;

    private class ServiceHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            Log.d(TAG, "Handler got event, what: " + msg.what);
            switch (msg.what) {
                case Common.MSG_REGISTER_CLIENT: {
                    mClients.add(msg.replyTo);
                    break;
                }
                case Common.MSG_UNREGISTER_CLIENT: {
                    mClients.remove(msg.replyTo);
                    break;
                }
                case Common.MSG_STOP_CAST: {
                    stopScreenCapture();
                    closeSocket(true);
                    stopSelf();
                }
            }
            return false;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Service receive broadcast action: " + action);
            if (action == null) {
                return;
            }
            if (Common.ACTION_STOP_CAST.equals(action)) {
                stopScreenCapture();
                closeSocket(true);
                stopSelf();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        _screen_dpi = metrics.densityDpi;
        _screen_width = metrics.widthPixels / 2;
        _screen_height = metrics.heightPixels / 2;

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mBroadcastIntentFilter = new IntentFilter();
        mBroadcastIntentFilter.addAction(Common.ACTION_STOP_CAST);
        registerReceiver(mBroadcastReceiver, mBroadcastIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroy service");
        stopScreenCapture();
        closeSocket(true);
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        mReceiverIp = intent.getStringExtra(Common.EXTRA_RECEIVER_IP);
        mResultCode = intent.getIntExtra(Common.EXTRA_RESULT_CODE, -1);
        mResultData = intent.getParcelableExtra(Common.EXTRA_RESULT_DATA);
        Log.d(TAG, "Remove IP: " + mReceiverIp);
        if (mReceiverIp == null) {
            return START_NOT_STICKY;
        }

        mSelectedBitrate = intent.getIntExtra(Common.EXTRA_VIDEO_BITRATE, Common.DEFAULT_VIDEO_BITRATE);
        mSelectedFormat = MediaFormat.MIMETYPE_VIDEO_AVC;

        mSelectedWidth = _screen_width;
        mSelectedHeight = _screen_height;
        mSelectedDpi = _screen_dpi;

        Log.d(TAG, "Start with client mode");
        if (!createSocket()) {
            Log.e(TAG, "Failed to create socket to receiver, ip: " + mReceiverIp);
            return START_NOT_STICKY;
        }
        if (!startScreenCapture()) {
            Log.e(TAG, "Failed to start capture screen");
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void showNotification() {
        final Intent notificationIntent = new Intent(Common.ACTION_STOP_CAST);
        PendingIntent notificationPendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(Notification.DEFAULT_ALL)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.casting_screen))
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), notificationPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NT_ID_CASTING, builder.build());
    }

    private void dismissNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NT_ID_CASTING);
    }

    private boolean startScreenCapture() {
        Log.d(TAG, "mResultCode: " + mResultCode + ", mResultData: " + mResultData);
        if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection();
            showNotification();
            startRecording();
            return true;
        }
        return false;
    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void startRecording() {
        Log.d(TAG, "startRecording");
        prepareVideoEncoder();
        mCastManager.initialize(mVideoEncoder);

        Log.d(TAG, "startRecording: " + mSelectedWidth + " " + mSelectedHeight + " " + mSelectedDpi);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("Recording Display", mSelectedWidth,
                mSelectedHeight, mSelectedDpi, 0 /* flags */, mInputSurface,
                null /* callback */, null /* handler */);

        // Start the encoders
        drainEncoder();
    }

    private void prepareVideoEncoder() {
        MediaFormat format = MediaFormat.createVideoFormat(mSelectedFormat, mSelectedWidth, mSelectedHeight);
        int frameRate = Common.DEFAULT_VIDEO_FPS;

        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mSelectedBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(mSelectedFormat);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (IOException e) {
            Log.e(TAG, "Failed to initial encoder, e: " + e);
            releaseEncoders();
        }
    }

    @Background
    protected void drainEncoder() {
        mCastManager.startRecording();
        return;
    }

    public void stopScreenCapture() {
        dismissNotification();
        releaseEncoders();
        closeSocket();
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    private void releaseEncoders() {
        mState = State.STOP;
        try {
            if (mVideoEncoder != null) {
                mVideoEncoder.stop();
                mVideoEncoder.release();
                mVideoEncoder = null;
            }
        } catch (Exception e) {

        }
        try {
            if (mInputSurface != null) {
                mInputSurface.release();
                mInputSurface = null;
            }
        } catch (Exception e) {

        }
        try {
            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        } catch (Exception e) {

        }
        try {
            if (mIvfWriter != null) {
                mIvfWriter = null;
            }
        } catch (Exception e) {

        }
        //mResultCode = 0;
        //mResultData = null;
        mVideoBufferInfo = null;
        //mTrackIndex = -1;
    }

    private boolean createSocket() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddr = InetAddress.getByName(mReceiverIp);
                    mSocket = new Socket(serverAddr, Common.VIEWER_PORT);
                    OutputStream outputStream = mSocket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(outputStream);
                    osw.write(String.format(Locale.getDefault(),
                            HTTP_MESSAGE_TEMPLATE, mSelectedWidth, mSelectedHeight));
                    osw.flush();
                    outputStream.flush();

                    switch (mSelectedFormat) {
                        case MediaFormat.MIMETYPE_VIDEO_AVC:
                            mWriterWrapper = new SocketOutputWrapper(outputStream);
                            break;
                        case MediaFormat.MIMETYPE_VIDEO_VP8:
                            mWriterWrapper = new IvfWriterWrapper(outputStream);
                            break;
                        default:
                            Log.e(TAG, "Unknown format: " + mSelectedFormat);
                            outputStream.close();
                            mSocket.close();
                            mSocket = null;
                    }

                    if (mWriterWrapper != null) {
                        mCastManager = new DefaultCast(CastService.this, mWriterWrapper);
                        mWriterWrapper.initialize(mSelectedWidth, mSelectedHeight);
                    }
                    return;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                mSocket = null;
            }
        });
        th.start();
        try {
            th.join();
            if (mSocket != null) {
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void closeSocket() {
        closeSocket(false);
    }

    private void closeSocket(boolean closeServerSocket) {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (closeServerSocket) {
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mServerSocket = null;
        }
        mSocket = null;
        mWriterWrapper = null;
    }
}
