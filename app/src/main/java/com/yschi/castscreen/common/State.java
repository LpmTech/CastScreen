package com.yschi.castscreen.common;

/**
 * Created by kevinleperf on 22/06/2016.
 */

public enum State {
    STOP,
    START;

    public static State from(int state) {
        switch (state) {
            case 1:
                return START;
            default:
                return STOP;
        }
    }
}
