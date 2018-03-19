package com.example.hhs.wavrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    public final static String RECORD_FINISHED_ACTION = "com.hhs.record_finished_action";
    public final static String RECOGNITION_FINISHED_ACTION = "com.hhs.recognition_finished_action";
    private MyListener listener;

    public void setOnListener(MyListener listener) {
        this.listener = listener;

    }
    @Override
    public void onReceive(Context context, Intent intent) {

        switch (intent.getAction()) {
            case RECORD_FINISHED_ACTION:
                String outputPath = intent.getStringExtra("filepath");
                if (listener != null)   listener.onFinishRecord(outputPath);
                break;

            case RECOGNITION_FINISHED_ACTION:
                String result = intent.getStringExtra("response");
                String label = intent.getStringExtra("label");
                if (listener != null)   listener.onFinishRecognition(result, label);
                break;
        }
    }
}