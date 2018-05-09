package com.hhs.waverecorder.listener;



public interface VoiceInputListener {
    void onFinishRecord(String path);
    void onFinishRecognition(String result, String filepath);

}
