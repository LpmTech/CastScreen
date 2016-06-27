package com.yschi.castscreen.ui.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yschi.castscreen.R;
import com.yschi.castscreen.ui.activity.AbstractPopableFragment;

/**
 * Created by kevinleperf on 27/06/2016.
 */

public class WaitingWifiFragment extends AbstractPopableFragment {

    public WaitingWifiFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_wifi, container, false);
    }

    @Override
    protected boolean canEdit() {
        return false;
    }

    @Override
    protected String getTitle() {
        return null;
    }

    @Override
    protected boolean hasList() {
        return false;
    }
}
