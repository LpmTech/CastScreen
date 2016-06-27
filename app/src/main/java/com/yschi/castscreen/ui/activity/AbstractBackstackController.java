package com.yschi.castscreen.ui.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import eu.codlab.cypherx.ui._abstract.activity.AbstractControllerActivity;

/**
 * Created by kevinleperf on 23/03/16.
 */
public abstract class AbstractBackstackController {

    private ViewGroup mLanding;
    private AbstractBackstackProvider mProvider;
    private FragmentManager mFragmentManager;

    private List<Integer> mIds;
    private List<Bundle> mDatas;
    private boolean mSending;
    private WeakReference<AbstractControllerActivity> mActivityReference;

    public AbstractBackstackController() {
        mIds = new ArrayList<>();
        mDatas = new ArrayList<>();
    }

    public void onCreate(AbstractControllerActivity activity, AbstractBackstackProvider provider,
                         FragmentManager fragment_manager,
                         ViewGroup landing) {
        mActivityReference = new WeakReference<>(activity);
        mProvider = provider;
        mFragmentManager = fragment_manager;
        mLanding = landing;

        mSending = false;
    }

    public void onResume() {
        if (!mSending) {
            setFragment();
            mSending = true;
        }
    }

    public void onPause() {

    }

    public void pop() {
        if (mIds.size() > 0) {
            mIds.remove(mIds.size() - 1);
            mDatas.remove(mDatas.size() - 1);
        }

        setFragment();
    }

    public void push(int id, Bundle data) {
        mIds.add(id);
        mDatas.add(data);

        setFragment();
    }

    private void setFragment() {
        AbstractPopableFragment fragment;
        int mId = mProvider.getHeadDefault();

        if (mIds.size() == 0) {
            fragment = mProvider.createDefault();
        } else {
            int id = mIds.size() - 1;
            mId = mIds.get(id);
            fragment = mProvider.createFragment(mId, mDatas.get(id));
        }

        mActivityReference.get().setLeftMenu(isRepresentingValueLeftMenu(mId));
        mActivityReference.get().setBackable(isRepresentingValueBackable(mId));
        mFragmentManager
                .beginTransaction()
                .replace(mLanding.getId(), fragment)
                .commit();
    }

    public int getHead() {
        if (mIds.size() == 0) {
            return mProvider.getHeadDefault();
        } else {
            int id = mIds.size() - 1;
            return mIds.get(id);
        }
    }

    public boolean canPop() {
        int id = mProvider.getHeadDefault();
        if (mIds.size() != 0) {
            id = mIds.size() - 1;
            id = mIds.get(id);
        }
        return isRepresentingValueBackable(id);
    }

    public void flush() {
        mIds.clear();
        mDatas.clear();

        setFragment();
    }

    protected abstract boolean isRepresentingValueLeftMenu(int value);

    protected abstract boolean isRepresentingValueBackable(int value);
}
