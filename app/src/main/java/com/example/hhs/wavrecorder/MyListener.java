package com.example.hhs.wavrecorder;



public interface MyListener {
    void onFinishRecord(String path);
    void onFinishRecognition(String result, String label);
}
