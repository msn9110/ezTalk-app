package com.hhs.waverecorder;

public interface AppValue {
    String CZTABLE = "czTable.json";
    String ZCTABLE = "zcTable_noTone.json";

    int UPDATE_VOLUME_CIRCLE = 1;
    int UPDATE_RECORDING_TEXT = 2;

    int REQUEST_RECORD_AUDIO = 13;

    String RECORD_FINISHED_ACTION = "com.hhs.record_finished_action";
    String RECOGNITION_FINISHED_ACTION = "com.hhs.recognition_finished_action";
}
