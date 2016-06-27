package com.yschi.castscreen.ui.main;

import com.yschi.castscreen.ui.activity.AbstractPopableFragment;

/**
 * Created by kevinleperf on 27/06/2016.
 */

public class WaitingWifiFragment extends AbstractPopableFragment {

    public WaitingWifiFragment() {

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
