// ICastService.aidl
package com.yschi.castscreen.service.cast;

// Declare any non-default types here with import statements

interface ICastService {

    void startRecording(String receiverIp, int resultCode, in Intent resultData, int selectedBitrate);

    void stopRecording();
}
