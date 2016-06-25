package com.yschi.castscreen.service.cast.managers;

import android.media.MediaCodec;
import android.util.Log;

import com.yschi.castscreen.common.State;
import com.yschi.castscreen.service.cast.CastService;
import com.yschi.castscreen.service.cast.writers.IWriterWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by kevinleperf on 24/06/2016.
 */

public class DefaultCast implements ICastManager {

    private IWriterWrapper mWriterWrapper;
    private State mState;
    private CastService mParent;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private MediaCodec mVideoEncoder;
    private String TAG = DefaultCast.class.getSimpleName();

    private DefaultCast() {
        mState = State.STOP;
    }

    public DefaultCast(CastService parent, IWriterWrapper writerWrapper) {
        this();
        mParent = parent;
        mWriterWrapper = writerWrapper;
    }

    @Override
    public void initialize(MediaCodec encoder) {
        mVideoEncoder = encoder;
        mState = State.STOP;
    }

    @Override
    public void startRecording() {
        mVideoBufferInfo = new MediaCodec.BufferInfo();

        mState = State.START;
        //mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        while (State.START.equals(mState)) {
            while (true) {
                int bufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER;

                try {
                    bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);
                } catch (Exception e) {
                    stopRecording();
                }

                if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // nothing available yet
                    break;
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once
                    //if (mTrackIndex >= 0) {
                    //    throw new RuntimeException("format changed twice");
                    //}
                    //mTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
                    //if (!mMuxerStarted && mTrackIndex >= 0) {
                    //    mMuxer.start();
                    //    mMuxerStarted = true;
                    //}
                } else if (bufferIndex < 0) {
                    // not sure what's going on, ignore it
                } else {
                    ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
                    if (encodedData == null) {
                        throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
                    }
                    // Fixes playability issues on certain h264 decoders including omxh264dec on raspberry pi
                    // See http://stackoverflow.com/a/26684736/4683709 for explanation
                    //if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    //    mVideoBufferInfo.size = 0;
                    //}

                    //Log.d(TAG, "Video buffer offset: " + mVideoBufferInfo.offset + ", size: " + mVideoBufferInfo.size);
                    if (mVideoBufferInfo.size != 0) {
                        encodedData.position(mVideoBufferInfo.offset);
                        encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);

                    }
                    if (mWriterWrapper.valid()) {
                        try {
                            byte[] b = new byte[encodedData.remaining()];
                            encodedData.get(b);
                            mWriterWrapper.write(b, mVideoBufferInfo.presentationTimeUs);
                        } catch (IOException e) {
                            Log.d(TAG, "Failed to write data to socket, stop casting");
                            e.printStackTrace();
                            stopRecording();
                            return;// false;
                        }
                    }

                    mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void stopRecording() {
        mState = State.STOP;
        mParent.stopScreenCapture();
    }
}
