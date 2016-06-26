package com.yschi.castscreen.service.cast;

import android.os.Binder;
import android.support.annotation.NonNull;

import com.yschi.castscreen.service.discovery.DiscoveryService;

/**
 * Created by kevinleperf on 22/06/2016.
 */

public class CastBinder extends Binder {

    @NonNull
    private CastService mCastService;

    private CastBinder() {

    }

    CastBinder(@NonNull CastService discoveryService) {
        this();
        mCastService = discoveryService;
    }

    @NonNull
    public CastService getService() {
        return mCastService;
    }
}
