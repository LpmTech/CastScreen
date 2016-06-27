// ICastService.aidl
package com.yschi.castscreen.service.cast;

import com.yschi.castscreen.service.cast.ICastServiceCallback;

interface ICastService {

    /**
     * Set the remote process to start the recording
     */
    void startRecording(String receiverIp, int resultCode, in Intent resultData, int selectedBitrate);

    /**
     * Set the remote process to stop the recording
     */
    void stopRecording();

    /**
     * Ask the remote to tell if the service is recording
     */
     boolean isRecording();

     void registerCallback(ICastServiceCallback listener);

     void unregisterCallback();
}
