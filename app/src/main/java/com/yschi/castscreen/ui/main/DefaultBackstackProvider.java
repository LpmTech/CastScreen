package com.yschi.castscreen.ui.main;

import android.os.Bundle;

import com.yschi.castscreen.ui.activity.AbstractBackstackProvider;
import com.yschi.castscreen.ui.activity.AbstractPopableFragment;
import com.yschi.castscreen.ui.activity.FragmentDescriptor;

/**
 * Created by kevinleperf on 23/03/16.
 */
public class DefaultBackstackProvider extends AbstractBackstackProvider {

    @Override
    protected boolean isBackable(int id) {
        return FragmentDescriptor.from(id).isBackable();
    }

    @Override
    protected AbstractPopableFragment createFragment(int id, Bundle data) {
        AbstractPopableFragment fragment = FragmentDescriptor.from(id).newInstance();
        if (data != null) fragment.setArguments(data);
        return fragment;
    }

    @Override
    public AbstractPopableFragment createDefault() {
        return createFragment(getHeadDefault(), null);
    }

    @Override
    public int getHeadDefault() {
        return FragmentDescriptor.MAIN.ordinal();
    }
}
