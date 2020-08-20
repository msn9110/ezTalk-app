package com.hhs.waverecorder.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.hhs.waverecorder.AppValue.*;

@SuppressWarnings("all")
public class WAVRecorder {
    private Context mContext;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final double BASE_VOLUME = 327.67;

    private boolean isSilence = true;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean stopByTimer = false;
    private String output;
    private Handler mUIHandler; // callback handler
    private double updateDuration = 1.0;
    private int n; // will recording n * 500 ms

    public boolean isRecordNow() {
        return isRecording;
    }

    private Thread timer = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                for (int i = 0; i < n && isRecording; i++) {
                    Thread.sleep(500);
                    //mUIHandler.sendEmptyMessage(UPDATE_RECORDING_TEXT);
                }
                stopByTimer = true;
                stopRecording();
            } catch (InterruptedException e) {
                Log.d("TIMER", "STOP");
            }
        }
    });

    public WAVRecorder(Context context, String path, int millis, Handler handler) {
        bufferSize = (int) (AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * updateDuration * 3);
        this.mContext = context;
        mUIHandler = handler;
        output = Environment.getExternalStorageDirectory() + "/" + path;
        final int max = 300 * 1000; // max duration
        if (millis < 0)
            millis = max;
        else if (millis < 1000)
            millis = 1000;
        else if (millis > max)
            millis = max;
        n = millis % 500 == 0 ? millis / 500 : millis / 500 + 1;

    }


    private String getFilename() {
        return (output);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        int i = recorder.getState();
        if (i == 1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        timer.start();
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;
        if (null != os) {
            Thread getMaxVolume = null;
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

                if (read > 0 && mUIHandler != null) {
                    final byte[] copy = Arrays.copyOf(data, bufferSize);
                    getMaxVolume = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int max = 0;
                            for (int i = 0; i < copy.length / 2; i++) {
                                short num = (short) ((((short) copy[2 * i + 1]) & 0xff) << 8);
                                num += ((short) copy[2 * i]) & 0xff;
                                num = (num >= 0) ? num : (short) -num; // abs value
                                if (num > max)
                                    max = num;
                            }
                            int level = (int) ((double) max / BASE_VOLUME);
                            Message msg = new Message();
                            msg.what = UPDATE_VOLUME_CIRCLE; // volume level
                            msg.arg1 = level;
                            if (isSilence && level >= 25)
                                isSilence = false;
                            mUIHandler.sendMessage(msg);
                        }
                    });
                    getMaxVolume.start();
                }

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (getMaxVolume != null)
                            try {
                                getMaxVolume.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (getMaxVolume != null) {
                try {
                    getMaxVolume.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stopRecording() {
        if (!stopByTimer && timer.isAlive()) {
            timer.interrupt();
        }

        if (null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        if (!isSilence && new File(getTempFilename()).length() >= RECORDER_SAMPLERATE) {
            copyWaveFile(getTempFilename(), getFilename());
            MediaScannerConnection.scanFile(mContext, new String[] {output},
                    null, null);
        }
        deleteTempFile();
        Intent intent = new Intent(RECORD_FINISHED_ACTION);
        intent.putExtra("filepath", output);
        mContext.sendBroadcast(intent);
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        File dir = new File(outFilename).getParentFile();
        if (!dir.exists())
            dir.mkdirs();
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = ((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1 : 2);
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (((RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1
                : 2) * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
}