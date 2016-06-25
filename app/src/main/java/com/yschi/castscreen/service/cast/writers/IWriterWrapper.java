package com.yschi.castscreen.service.cast.writers;

import java.io.IOException;

/**
 * Created by kevinleperf on 24/06/2016.
 */

public interface IWriterWrapper {

    void initialize(int width, int height);

    boolean valid();

    void write(byte[] data, long presentationTimeUs) throws IOException;
}
