package com.hhs.waverecorder.listener;



public interface MyListener {
    void onFinishRecord(String path);
    void onFinishRecognition(String result, String filepath);

}
