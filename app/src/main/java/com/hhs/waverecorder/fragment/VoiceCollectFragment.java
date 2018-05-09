package com.hhs.waverecorder.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.Recognition;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.OnCursorChangedListener;
import com.hhs.waverecorder.listener.VoiceInputListener;
import com.hhs.waverecorder.receiver.VoiceInputEventReceiver;
import com.hhs.waverecorder.widget.MyText;
import com.hhs.waverecorder.widget.VolumeCircle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;

import static com.hhs.waverecorder.AppValue.*;
import static com.hhs.waverecorder.utils.MyFile.moveFile;
import static com.hhs.waverecorder.utils.Utils.lookTable;

@SuppressWarnings("all")
public class VoiceCollectFragment extends Fragment implements
        View.OnClickListener, AdapterView.OnItemSelectedListener,
        OnCursorChangedListener, VoiceInputListener {


    public static VoiceCollectFragment newInstance(String czJSONString) {
        VoiceCollectFragment mFragment = new VoiceCollectFragment();
        Bundle args = new Bundle();
        args.putString("czJSONString", czJSONString);
        mFragment.setArguments(args);
        return mFragment;
    }


    private final String TAG = "## " + getClass().getSimpleName();

    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    VoiceInputEventReceiver eventReceiver = new VoiceInputEventReceiver();
    Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_VOLUME_CIRCLE:
                    if (circle != null)
                        volView.removeView(circle);
                    int level = msg.arg1;
                    circle = new VolumeCircle(mContext, level, dpi);
                    volView.addView(circle);
                    break;

                case UPDATE_RECORDING_TEXT:
                    String recordingMsg = "錄音中";
                    for (int i = 0; i <= recordingDot; i++)
                        recordingMsg += ".";
                    recordingDot = (recordingDot + 1) % 3;
                    tvRecNOW.setText(recordingMsg);
                    break;
            }
        }
    };

    //UI Variable
    ImageButton btnRec;
    Button btnDel;
    FrameLayout volView;
    Spinner spMyLabel, spTone;
    MyText txtWord;
    TextView tvRecNOW, tvCorrect, tvTotal, tvPath, tvRes;
    VolumeCircle circle = null;

    //Global Data
    JSONObject czTable/*chineseToZhuyin*/;
    int width, height, dpi; // device resolution in pixels used for UI

    //Global Variable
    Deque<String> recordedPath = new LinkedList<>();
    String label = "";
    String tone = "";
    int correct = 0, total = 0;

    //State Variable
    private boolean isRecord = false;
    private int recordingDot = 0; // max 2

    private void initUI() {
        // get resolution
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        dpi = dm.densityDpi;

        volView = mView.findViewById(R.id.volume);
        txtWord = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);
        btnDel = mView.findViewById(R.id.btnDel);
        spMyLabel = mView.findViewById(R.id.spMyLabel);
        spTone = mView.findViewById(R.id.spTone);
        tvRecNOW = mView.findViewById(R.id.tvRecNOW);
        tvPath = mView.findViewById(R.id.tvPath);
        tvTotal = mView.findViewById(R.id.tvTotal);
        tvCorrect = mView.findViewById(R.id.tvCorrect);
        tvRes = mView.findViewById(R.id.tvRes);

        txtWord.setOnCursorChangedListener(this);
        spMyLabel.setOnItemSelectedListener(this);
        spTone.setOnItemSelectedListener(this);
        btnRec.setOnClickListener(this);
        btnDel.setOnClickListener(this);

        ArrayAdapter<String> ad = new ArrayAdapter<>(mContext, R.layout.myspinner,
                                    Arrays.asList("0", "1", "2", "3", "4"));
        ad.setDropDownViewResource(R.layout.myspinner);
        spTone.setAdapter(ad);
        spTone.setSelection(1, true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        Bundle args = getArguments();
        try {
            czTable = new JSONObject(args.getString("czJSONString"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mContext = getActivity();
        eventReceiver.setOnListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.fragment_voice_collect, container, false);
        initUI();
        return mView;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_FINISHED_ACTION);
        intentFilter.addAction(RECOGNITION_FINISHED_ACTION);
        mContext.registerReceiver(eventReceiver, intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(eventReceiver);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRec:
                if (!isRecord && label.length() > 0) {
                    SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                    final String path = "MyRecorder/" + label + "/" + tone +
                            "-" + df.format(new Date()) + ".wav";
                    Thread recorder = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WAVRecorder wavRecorder = new WAVRecorder(path, mContext, mUIHandler);
                            Log.d(TAG, "Start Recording");
                            wavRecorder.startRecording();
                            try {
                                isRecord = true;
                                for (int i = 0; i < 5; i++) {
                                    Thread.sleep(500);
                                    mUIHandler.sendEmptyMessage(UPDATE_RECORDING_TEXT);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                wavRecorder.stopRecording();
                                isRecord = false;
                                Log.d(TAG, "Stop Recording");
                            }
                        }
                    });
                    recorder.start();
                }
                break;

            case R.id.btnDel:
                if (recordedPath.size() > 0) {
                    String path = recordedPath.removeFirst();
                    File file = new File(path);
                    file.delete();
                    MediaScannerConnection.scanFile(mContext, new String[]{path}, null, null);
                    path = recordedPath.peekFirst();
                    tvPath.setText(path);
                    total--;
                    tvTotal.setText("已錄 : " + total);
                }
                break;
        }
    }

    @Override
    public void onCursorChanged(View view) {
        switch (view.getId()) {
            case R.id.txtMsg:
                String msg = txtWord.getText().toString().replaceAll("[^\u4e00-\u9fa6]", "");
                ArrayList<String> noToneLabels = new ArrayList<>();
                noToneLabels.add("-");
                int selection = 0;
                if (msg.length() > 0) {
                    String ch = msg.substring(0, 1);
                    try {
                        ArrayList<String> pronounces = lookTable(czTable, ch, "pronounces");
                        selection = 1;
                        for (String p:pronounces) {
                            String noToneLabel = p.replaceAll("[˙ˊˇˋ]$", "");
                            if (!noToneLabels.contains(noToneLabel)) {
                                noToneLabels.add(noToneLabel);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                ArrayAdapter<String> ad = new ArrayAdapter<>(mContext, R.layout.myspinner, noToneLabels);
                ad.setDropDownViewResource(R.layout.myspinner);
                spMyLabel.setAdapter(ad);
                spMyLabel.setSelection(selection, true);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        switch (adapterView.getId()) {
            case R.id.spMyLabel:
                label = "";
                if (pos > 0) {
                    label = ((TextView) view).getText().toString();
                    spTone.setSelection(1, true);
                }
                break;
            case R.id.spTone:
                tone = ((TextView) view).getText().toString();
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onFinishRecord(String path) {
        tvRecNOW.setText("");
        volView.removeView(circle);
        circle = null;
        File file = new File(path);
        if (file.length() > 44) {
            total++;
            tvTotal.setText("已錄 : " + total);
            recordedPath.addFirst(path);
            tvPath.setText(path);
            new Recognition(mContext, path, mUIHandler).start();
        } else {
            file.delete();
            MediaScannerConnection.scanFile(mContext, new String[]{path}, null, null);
        }
    }

    @Override
    public void onFinishRecognition(String result, String filepath) {
        File file = new File(filepath);
        String correctLabel = file.getParentFile().getName();
        try {
            JSONObject response = new JSONObject(result).getJSONObject("response");
            if (response.getBoolean("success")) {
                String myResult = "";
                JSONArray jsonArray = response.getJSONArray("result");
                for (int i = 0; i < jsonArray.length(); i++) {
                    myResult += jsonArray.getString(i) + ",";
                    if (correctLabel.contentEquals(jsonArray.getString(i)))
                        correct++;
                }
                myResult = myResult.replaceAll(",$", "");
                tvRes.setText(myResult);
                tvCorrect.setText("Accuracy : " + correct + " / " + total);
                recordedPath.removeFirst();
                String newPath = file.getParent() + "/uploaded-" + file.getName();
                recordedPath.addFirst(newPath);
                moveFile(filepath, newPath);
                MediaScannerConnection.scanFile(mContext, new String[]{filepath, newPath}, null, null);
                if (tvPath.getText().toString().contentEquals(filepath))
                    tvPath.setText(newPath);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
