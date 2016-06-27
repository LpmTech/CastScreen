package com.yschi.castscreen.ui.activity.events;

import android.os.Bundle;

import eu.codlab.cypherx.ui._abstract.FragmentDescriptor;

/**
 * Created by kevinleperf on 25/03/16.
 */
public class OnEventPushFragment {

    private FragmentDescriptor mType;

    private Bundle mData;

    private OnEventPushFragment() {

    }

    public OnEventPushFragment(FragmentDescriptor type, Bundle data) {
        mType = type;
        mData = data;
    }

    public FragmentDescriptor getType() {
        return mType;
    }

    public Bundle getData() {
        return mData;
    }
}
