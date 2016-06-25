package com.yschi.castscreen.service.discovery.events;

/**
 * Created by kevinleperf on 22/06/2016.
 */
public class EventDiscoveryClient {

    private String mName;
    private String mIp;

    private EventDiscoveryClient() {

    }

    public EventDiscoveryClient(String name, String ip) {
        this();
        mName = name;
        mIp = ip;
    }

    public String getName() {
        return mName;
    }

    public String getIp() {
        return mIp;
    }
}
