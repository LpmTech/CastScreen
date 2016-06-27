package com.yschi.castscreen.ui.activity;

import android.content.Context;
import android.support.v4.BuildConfig;
import android.support.v4.app.Fragment;
import android.view.View;

import de.greenrobot.event.EventBus;
import eu.codlab.cypherx.R;

/**
 * Created by kevinleperf on 23/03/16.
 */
public abstract class AbstractPopableFragment extends Fragment {
    private IToolbarManipulation mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IToolbarManipulation) {
            mListener = (IToolbarManipulation) context;
        } else {
            throw new IllegalStateException("the context does not implements "
                    + IToolbarManipulation.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    protected IToolbarManipulation getListener() {
        return mListener;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }

        getListener().setCanTakePhoto(canTakePhoto());
        getListener().setCanEdit(canEdit());

        getListener().setToolbarImage(getImage());

        String title = getTitle();

        if (title == null) {
            getActivity().setTitle(R.string.app_name);
        } else {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    protected boolean tryScrollFromError(View view, boolean notified) {
        if (notified) return notified;
        getListener().scrollTo(view);
        return true;
    }


    protected abstract boolean canTakePhoto();

    protected abstract boolean canEdit();

    protected abstract String getImage();

    protected abstract String getTitle();

    protected abstract boolean hasList();

}
