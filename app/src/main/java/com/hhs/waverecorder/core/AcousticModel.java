package com.hhs.waverecorder.core;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class AcousticModel {
    private TensorFlowInferenceInterface top, mid, bot, syllable;

    public AcousticModel() {
    }
}
