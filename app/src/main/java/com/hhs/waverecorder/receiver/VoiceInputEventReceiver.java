package com.hhs.waverecorder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hhs.waverecorder.listener.VoiceInputListener;

import static com.hhs.waverecorder.AppValue.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.AppValue.RECORD_FINISHED_ACTION;

public class VoiceInputEventReceiver extends BroadcastReceiver {


    private VoiceInputListener listener;

    public void setOnListener(VoiceInputListener listener) {
        this.listener = listener;

    }
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case RECORD_FINISHED_ACTION:
                    String outputPath = intent.getStringExtra("filepath");
                    if (listener != null)   listener.onFinishRecord(outputPath);
                    break;

                case RECOGNITION_FINISHED_ACTION:
                    String result = intent.getStringExtra("response");
                    String filepath = intent.getStringExtra("filepath");
                    if (listener != null)   listener.onFinishRecognition(result, filepath);
                    break;
            }
        }
    }
}