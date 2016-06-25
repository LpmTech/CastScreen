package com.yschi.castscreen.service.discovery;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yschi.castscreen.Common;
import com.yschi.castscreen.common.State;
import com.yschi.castscreen.common.Utils;
import com.yschi.castscreen.service.discovery.events.EventDiscoveryClient;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EService;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Created by kevinleperf on 22/06/2016.
 */

@EService
public class DiscoveryService extends Service {

    private final static String TAG = DiscoveryService.class.getSimpleName();
    private final DiscoveryBinder mBinder;
    private State mState;

    public DiscoveryService() {
        mBinder = new DiscoveryBinder(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mState = State.STOP;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startDiscovery() {
        if (State.STOP.equals(mState)) {
            mState = State.START;

            startDiscoveryBackground();
        }
    }

    public void stopDiscovery() {
        if (State.START.equals(mState)) {
            mState = State.STOP;
        }
    }

    private void onNewClient(String name, String ip) {
        EventBus.getDefault().post(new EventDiscoveryClient(name, ip));
    }

    @Background
    protected void startDiscoveryBackground() {
        try {
            DatagramSocket discoverUdpSocket = new DatagramSocket();
            Log.d(TAG, "Bind local port: " + discoverUdpSocket.getLocalPort());
            discoverUdpSocket.setSoTimeout(3000);
            byte[] buf = new byte[1024];
            while (State.START.equals(mState)) {
                if (!Utils.sendBroadcastMessage(this, discoverUdpSocket, Common.DISCOVER_PORT, Common.DISCOVER_MESSAGE)) {
                    Log.w(TAG, "Failed to send discovery message");
                }
                Arrays.fill(buf, (byte) 0);
                DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
                try {
                    discoverUdpSocket.receive(receivePacket);
                    String ip = receivePacket.getAddress().getHostAddress();
                    Log.d(TAG, "Receive discover response from " + ip + ", length: " + receivePacket.getLength());
                    if (receivePacket.getLength() > 9) {
                        byte[] data = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                        String respMsg = new String(data);
                        Log.d(TAG, "Discover response message: " + respMsg);
                        try {
                            JSONObject json = new JSONObject(respMsg);
                            String name = json.getString("name");
                            //String id = json.getString("id");
                            String width = json.getString("width");
                            String height = json.getString("height");

                            onNewClient(name, ip);
                            Log.d(TAG, "Got receiver name: " + name + ", ip: " + ip + ", width: " + width + ", height: " + height);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (SocketTimeoutException e) {
                }

                Thread.sleep(3000);
            }
        } catch (SocketException e) {
            Log.d(TAG, "Failed to create socket for discovery");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
