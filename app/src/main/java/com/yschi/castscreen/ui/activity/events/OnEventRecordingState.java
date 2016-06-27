package com.yschi.castscreen.ui.activity.events;

import com.yschi.castscreen.common.State;

/**
 * Created by kevinleperf on 27/06/2016.
 */

public class OnEventRecordingState {

    private State mState;

    private OnEventRecordingState() {

    }

    public OnEventRecordingState(State state) {
        mState = state;
    }

    public State getState() {
        return mState;
    }

}
