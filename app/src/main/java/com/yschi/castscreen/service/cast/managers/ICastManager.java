package com.yschi.castscreen.service.cast.managers;

import android.media.MediaCodec;

import com.yschi.castscreen.common.State;

/**
 * Created by kevinleperf on 24/06/2016.
 */

public interface ICastManager {
    void initialize(MediaCodec encoder);

    void startRecording();

    void stopRecording();

    boolean isRecording();

    State getState();
}
