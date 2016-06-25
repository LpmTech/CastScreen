package com.yschi.castscreen.service.cast.writers;

import com.yschi.castscreen.IvfWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by kevinleperf on 24/06/2016.
 */

public class IvfWriterWrapper implements IWriterWrapper {
    private OutputStream mSocketOutputStream;
    private IvfWriter mIvfWriter;

    private IvfWriterWrapper() {

    }

    public IvfWriterWrapper(OutputStream socketOutputStream) {
        this();

        mSocketOutputStream = socketOutputStream;
    }


    @Override
    public void initialize(int width, int height) {
        try {
            mIvfWriter = new IvfWriter(mSocketOutputStream, width, height);
            mIvfWriter.writeHeader();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
