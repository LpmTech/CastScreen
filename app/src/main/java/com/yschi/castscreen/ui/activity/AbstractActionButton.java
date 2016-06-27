package com.yschi.castscreen.ui.activity;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import butterknife.ButterKnife;

/**
 * Created by kevinleperf on 25/03/16.
 */
public abstract class AbstractActionButton extends LinearLayout {

    private final static int BASE = 87100;

    public AbstractActionButton(Context context) {
        super(context);

        init();
    }

    public AbstractActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public AbstractActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AbstractActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init();
    }

    public static int getIdFor(FragmentDescriptor fragment) {
        return BASE + fragment.ordinal();
    }

    private void init() {
        LayoutInflater.from(getContext())
                .inflate(getLayoutView(), this, true);

        ButterKnife.bind(this);


        setId(createId());
    }

    protected abstract int createId();

    protected abstract int getLayoutView();
}
