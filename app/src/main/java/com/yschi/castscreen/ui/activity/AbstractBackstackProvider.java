package com.yschi.castscreen.ui.activity;

import android.os.Bundle;

/**
 * Created by kevinleperf on 23/03/16.
 */
public abstract class AbstractBackstackProvider {

    protected abstract boolean isBackable(int id);

    protected abstract AbstractPopableFragment createFragment(int id, Bundle data);

    public abstract AbstractPopableFragment createDefault();

    public abstract int getHeadDefault();
}
