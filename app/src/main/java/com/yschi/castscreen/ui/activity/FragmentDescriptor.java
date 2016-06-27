package com.yschi.castscreen.ui.activity;

import android.content.Context;
import android.util.Log;

import com.yschi.castscreen.ui.main.MainFragment;
import com.yschi.castscreen.ui.main.WaitingWifiFragment;

/**
 * Created by kevinleperf on 23/03/16.
 */
public enum FragmentDescriptor {
    MAIN(BackType.NONE, ToolbarType.EXPANDABLE, MainFragment.class, null, false),
    WIFI(BackType.NONE, ToolbarType.EXPANDABLE, WaitingWifiFragment.class, null, false);

    private final static String TAG = FragmentDescriptor.class.getSimpleName();
    private BackType mBackType;
    private ToolbarType mToolbarScrollable;
    private boolean mRefreshEnabled;

    private Class<? extends AbstractPopableFragment> mClass;
    private Class<? extends AbstractActionButton> mActionClass;

    FragmentDescriptor(BackType back_type,
                       ToolbarType toolbar_scrollable,
                       Class<? extends AbstractPopableFragment> klass,
                       Class<? extends AbstractActionButton> action_klass,
                       boolean refreshEnabled) {
        mBackType = back_type;
        mToolbarScrollable = toolbar_scrollable;
        mClass = klass;
        mActionClass = action_klass;
        mRefreshEnabled = refreshEnabled;
    }

    public static FragmentDescriptor from(int id) {
        Log.d(TAG, "from: " + id);
        switch (id) {
            case 1:
                return WIFI;
            default:
                return MAIN;
        }
    }

    public AbstractPopableFragment newInstance() {
        try {
            return mClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public AbstractActionButton newButton(Context context) {
        try {
            if (mActionClass != null)
                return mActionClass.getConstructor(Context.class)
                        .newInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isLeftMenu() {
        return BackType.LEFT_MENU.equals(mBackType);
    }

    public boolean isBackable() {
        return BackType.BACK.equals(mBackType);
    }

    public ToolbarType getToolbarType() {
        return mToolbarScrollable;
    }

    public boolean isRefreshEnabled() {
        return mRefreshEnabled;
    }
}
