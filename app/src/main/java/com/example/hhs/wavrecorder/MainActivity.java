package com.example.hhs.wavrecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.hhs.wavrecorder.MyReceiver.RECOGNITION_FINISHED_ACTION;
import static com.example.hhs.wavrecorder.MyReceiver.RECORD_FINISHED_ACTION;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        MyListener
{

    // Constants that control the behavior of the recognition code and model
    // settings. See the audio recognition tutorial for a detailed explanation of
    // all these, but you should customize them to match your training settings if
    // you are running your own model.
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_DURATION_MS = 1000;
    private static final int RECORDING_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    private static final long AVERAGE_WINDOW_DURATION_MS = 500;
    private static final float DETECTION_THRESHOLD = 0.70f;
    private static final int SUPPRESSION_MS = 1500;
    private static final int MINIMUM_COUNT = 3;
    private static final long MINIMUM_TIME_BETWEEN_SAMPLES_MS = 30;
    private static final String LABEL_FILENAME = "file:///android_asset/labels.txt";
    private static final String MODEL_FILENAME = "file:///android_asset/no_tone1.pb";
    private static final String INPUT_DATA_NAME = "decoded_sample_data:0";
    private static final String SAMPLE_RATE_NAME = "decoded_sample_data:1";
    private static final String OUTPUT_SCORES_NAME = "labels_softmax";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private WAVRecorder wavRecorder;
    private boolean isRecord = false;
    private ListView listLabels, listTones;
    private TextView txtRes, txtAcc, txtRecState;
    private Handler mHandler = new Handler();
    private static final int REQUEST_RECORD_AUDIO = 13;
    private Context mContext;
    private MyReceiver myReceiver = new MyReceiver();

    private List<String> labels = new ArrayList<>();
    private List<String> displayedLabels = new ArrayList<>();
    private int total = 0, count = 0;
    String tone = "1";
    
    MyAdapter toneAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        requestPermission();
        listLabels = (ListView) findViewById(R.id.listLabels);
        listTones = (ListView) findViewById(R.id.listPron);
        txtRes = (TextView) findViewById(R.id.txtRes);
        txtAcc = (TextView) findViewById(R.id.txtAcc);
        txtRecState = (TextView) findViewById(R.id.txtRecState);

        // Load the labels for the model, but only display those that don't start
        // with an underscore.
        String actualFilename = LABEL_FILENAME.split("file:///android_asset/")[1];
        Log.i(LOG_TAG, "Reading labels from: " + actualFilename);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(getAssets().open(actualFilename)));
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
                if (line.charAt(0) != '_') {
                    displayedLabels.add(line.substring(0, 1).toUpperCase() + line.substring(1));
                }
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!", e);
        }

        // Build a list view based on these labels.
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayedLabels);
        listLabels.setAdapter(arrayAdapter);
        listLabels.setOnItemClickListener(this);

        List<String> tones = new ArrayList<>(Arrays.asList("0", "1", "2", "3", "4"));
        toneAdapter = new MyAdapter(this, tones);
        listTones.setAdapter(toneAdapter);
        listTones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                toneAdapter.setSelectPosition(i);
                toneAdapter.notifyDataSetChanged();
                tone = ((ViewHolder) view.getTag()).name.getText().toString();
            }
        });
        toneAdapter.setSelectPosition(1);
        //listTones.setSelection(1);
        myReceiver.setOnListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_FINISHED_ACTION);
        intentFilter.addAction(RECOGNITION_FINISHED_ACTION);
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(myReceiver);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String select = ((TextView) view).getText().toString();
        switch (adapterView.getId()) {
            case R.id.listLabels:
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                final String path = "MyRecorder/" + select + "/" + tone +
                                                             "-" + df.format(new Date()) + ".wav";
                Thread worker = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        wavRecorder = new WAVRecorder(path, mContext);
                        System.out.println("Start Recording");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                txtRecState.setVisibility(View.VISIBLE);
                            }
                        });
                        wavRecorder.startRecording();
                        try {
                            isRecord = true;
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            wavRecorder.stopRecording();
                            isRecord = false;
                            System.out.println("recording finished!");
                        }
                    }
                });
                if (!isRecord)
                    worker.start();
                break;
        }
    }

    private void startRecognition(File file) {
        Toast.makeText(mContext, "saved to : " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        new FTPManager(mContext, file).execute();
    }

    @Override
    public void onFinishRecord(String path) {
        txtRecState.setVisibility(View.GONE);
        final File file = new File(path);

        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
                                            .setTitle("是否保存")
                                            .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (file.length() <= 44) {
                                                        file.delete();
                                                        MediaScannerConnection.scanFile(mContext,
                                                                new String[] {file.getAbsolutePath()}, null, null);
                                                    } else {
                                                        startRecognition(file);
                                                    }

                                                }
                                            })
                                            .setNegativeButton("否", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    file.delete();
                                                    MediaScannerConnection.scanFile(mContext,
                                                            new String[] {file.getAbsolutePath()}, null, null);
                                                }
                                            });
        dialog.show();
    }

    @Override
    public void onFinishRecognition(String result, String label) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject response = jsonObject.getJSONObject("response");
            boolean success = response.getBoolean("success");
            txtRes.setText("");
            if (success) {
                total += 1;
                JSONArray pronouces = response.getJSONArray("result");
                ArrayList<String> myResults = new ArrayList<>();
                String text = "";
                for (int i = 0; i < pronouces.length(); i++) {
                    String element = pronouces.getString(i);
                    if (element.contentEquals(label))
                        count += 1;
                    myResults.add(element);
                    text += element + ",";

                }
                System.out.println(text);
                txtRes.setText(text);
                String accuracy = String.valueOf(count) + "/" + String.valueOf(total);
                txtAcc.setText(accuracy);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}
