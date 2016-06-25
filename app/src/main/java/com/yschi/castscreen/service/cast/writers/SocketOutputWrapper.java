package com.yschi.castscreen.service.cast.writers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by kevinleperf on 24/06/2016.
 */

public class SocketOutputWrapper implements IWriterWrapper {
    private OutputStream mSocketOutputStream;

    private SocketOutputWrapper() {

    }

    public SocketOutputWrapper(OutputStream socketOutputStream) {
        mSocketOutputStream = socketOutputStream;
    }


    @Override
    public void initialize(int width, int height) {

    }

    @Override
    public boolean valid() {
        return mSocketOutputStream != null;
    }

    @Override
    public void write(byte[] data, long presentationTimeUs) throws IOException {
        mSocketOutputStream.write(data);
    }
}
