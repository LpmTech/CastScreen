package com.yschi.castscreen.ui.activity.events;

import android.content.Intent;

import eu.codlab.cypherx.ui._abstract.FragmentDescriptor;

/**
 * Created by kevinleperf on 25/03/16.
 */
public class OnEventPopFragment {

    private FragmentDescriptor mType;

    private Intent mData;

    private OnEventPopFragment() {

    }

    public OnEventPopFragment(FragmentDescriptor type, Intent data) {
        mType = type;
        mData = data;
    }

    public FragmentDescriptor getType() {
        return mType;
    }

    public Intent getData() {
        return mData;
    }
}
