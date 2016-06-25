package com.yschi.castscreen.service.discovery;

import android.os.Binder;
import android.support.annotation.NonNull;

/**
 * Created by kevinleperf on 22/06/2016.
 */

public class DiscoveryBinder extends Binder {

    @NonNull
    private DiscoveryService mDiscoveryService;

    private DiscoveryBinder() {

    }

    DiscoveryBinder(@NonNull DiscoveryService discoveryService) {
        this();
        mDiscoveryService = discoveryService;
    }

    @NonNull
    public DiscoveryService getService() {
        return mDiscoveryService;
    }
}
