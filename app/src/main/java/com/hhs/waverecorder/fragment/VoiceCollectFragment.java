package com.hhs.waverecorder.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.Recognition;
import com.hhs.waverecorder.core.RemoteDelete;
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
import static com.hhs.waverecorder.utils.Utils.getTone;
import static com.hhs.waverecorder.utils.Utils.lookTable;
import static com.hhs.waverecorder.utils.Utils.readTables;

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
                    int level = msg.arg1;
                    circle.setLevel(level);
                    String recordingMsg = "錄音中(" + level + "%)";
                    tvRecNOW.setText(recordingMsg);
                    break;
            }
        }
    };

    //UI Variable
    ProgressDialog loadingPage;
    ImageButton btnRec;
    Button btnDel, btnMoveCursor;
    FrameLayout volView;
    Spinner spMyLabel, spTone;
    MyText txtWord;
    CheckBox chkUpload;
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
    WAVRecorder recorder = null;
    ArrayList<String> chosenLabels = new ArrayList<>();

    //State Variable
    boolean isSentence = false;

    private void initUI() {
        // get resolution
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        dpi = dm.densityDpi;

        // loading page
        loadingPage = new ProgressDialog(mContext);
        loadingPage.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingPage.setTitle("辨識中");
        loadingPage.setMessage("請稍候");

        volView = mView.findViewById(R.id.volume);
        txtWord = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);
        btnDel = mView.findViewById(R.id.btnDel);
        btnMoveCursor = mView.findViewById(R.id.btnMoveCursor);
        spMyLabel = mView.findViewById(R.id.spMyLabel);
        spTone = mView.findViewById(R.id.spTone);
        tvRecNOW = mView.findViewById(R.id.tvRecNOW);
        tvPath = mView.findViewById(R.id.tvPath);
        tvTotal = mView.findViewById(R.id.tvTotal);
        tvCorrect = mView.findViewById(R.id.tvCorrect);
        tvRes = mView.findViewById(R.id.tvRes);
        chkUpload = mView.findViewById(R.id.chkUpload);

        txtWord.setOnCursorChangedListener(this);
        txtWord.addTextChangedListener(textWatcher);
        spMyLabel.setOnItemSelectedListener(this);
        spTone.setOnItemSelectedListener(this);
        btnRec.setOnClickListener(this);
        btnDel.setOnClickListener(this);
        btnMoveCursor.setOnClickListener(this);

        ArrayAdapter<String> ad = new ArrayAdapter<>(mContext, R.layout.myspinner,
                                    Arrays.asList("0", "1", "2", "3", "4"));
        ad.setDropDownViewResource(R.layout.myspinner);
        spTone.setAdapter(ad);
        spTone.setSelection(1, true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate : " + Thread.currentThread().getId());
        mContext = getActivity();
        eventReceiver.setOnListener(this);
        try {
            czTable = readTables(mContext).getJSONObject("czTable");
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
    public void onPause() {
        super.onPause();
        if (recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }
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
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
                String path = "MyRecorder/";
                int duration = -1;
                boolean toRec = false;
                if (!isSentence) {
                    duration = 2500;
                    toRec = label.length() > 0;
                    path += "withTone/" + label.replaceAll("[˙_ˊˇˋ]", "")
                            + "˙_ˊˇˋ".charAt(Integer.parseInt(tone)) + "/";
                } else {
                    String origin_msg = txtWord.getText().toString();
                    String dir = origin_msg.replaceAll("[^\u4e00-\u9fa6]+", "-");
                    toRec = dir.replaceAll("-", "").length() > 0 &&
                            origin_msg.replaceAll("[\u4e00-\u9fa6]", "").length() == 0;
                    path += "sentence/" + dir + "/";
                }

                path = path.replaceAll("\\s", "") + df.format(new Date()) + ".wav";
                if (!toRec) {
                    AlertDialog warn = new AlertDialog.Builder(mContext).setTitle("Warning")
                                           .setMessage("請輸入一個中文字或全中文句子!!!")
                                           .create();
                    warn.show();
                }
                if (recorder == null && toRec) {
                    recorder = new WAVRecorder(mContext, path, duration, mUIHandler);
                    circle = new VolumeCircle(mContext, 0, dpi);
                    volView.addView(circle);
                    Log.d(TAG, "Start Recording");
                    recorder.startRecording();
                } else if (recorder != null && recorder.isRecordNow()) {
                    recorder.stopRecording();
                }
                break;

            case R.id.btnDel:
                if (recordedPath.size() > 0) {
                    String recorded = recordedPath.removeFirst();
                    RemoteDelete remoteDelete = new RemoteDelete(recorded);
                    remoteDelete.executeRemoteDelete();
                    File file = new File(recorded);
                    file.delete();
                    MediaScannerConnection.scanFile(mContext, new String[]{recorded}, null, null);
                    recorded = recordedPath.peekFirst();
                    tvPath.setText(recorded);
                    total--;
                    tvTotal.setText("已錄 : " + total);
                }
                break;

            case R.id.btnMoveCursor:
                txtWord.setSelection((txtWord.getSelectionEnd() + 1) % (txtWord.length() + 1));
                break;
        }
    }
    TextWatcher textWatcher = new TextWatcher() {
        int cursor;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            Log.d(TAG, "Trigger TextWatcher");
            cursor = txtWord.getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            boolean insertMode = aftercount > beforecount;
            Log.d(TAG, "TextWatcher : " + s.toString());
            start = cursor;

            int endPos = insertMode ? start + aftercount - beforecount : start + beforecount - aftercount;
            if (insertMode) { // insert mode
                String addedText = s.toString().substring(start, endPos);
                for (int i = start; i < endPos; i++) {
                    String ch = addedText.substring(i - start, i - start + 1);
                    // avoid no mapping in czTable
                    chosenLabels.add(i, "-");
                    int selection = 0;
                    try {
                        ArrayList<String> candidate = lookTable(czTable, ch, "pronounces");
                        if (candidate.size() > 0) {
                            String myLabel = candidate.get(0);
                            chosenLabels.set(i, myLabel);
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "no Mapping In czTable");
                    }
                }
            } else { // delete
                for (int i = start - 1; i < endPos - 1; i++) {
                    chosenLabels.remove(i);
                }
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };
    @Override
    public void onCursorChanged(View view) {
        switch (view.getId()) {
            case R.id.txtMsg:
                int pos = txtWord.getSelectionEnd();
                String msg = txtWord.getText().toString();
                ArrayList<String> labels = new ArrayList<>();
                labels.add("-");
                int selection = 0;
                if (msg.length() >= 1) {
                    isSentence = !(msg.replaceAll("[^\u4e00-\u9fa6]", "").length() == 1);
                    String ch = (pos >= 1) ? msg.substring(pos - 1, pos) : msg.substring(0, 1);
                    try {
                        ArrayList<String> pronounces = lookTable(czTable, ch, "pronounces");
                        for (String p:pronounces) {
                            String label = p;
                            if (!labels.contains(label)) {
                                labels.add(label);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        int idx = (pos >= 1) ? pos - 1 : 0;
                        selection = labels.indexOf(chosenLabels.get(idx));
                    }
                }
                ArrayAdapter<String> ad = new ArrayAdapter<>(mContext, R.layout.myspinner, labels);
                ad.setDropDownViewResource(R.layout.myspinner);
                spMyLabel.setAdapter(ad);
                spMyLabel.setSelection(selection, true);
                recordedPath.clear();
                tvPath.setText("");
                total = 0;
                correct = 0;
                tvCorrect.setText("");
                tvTotal.setText("已錄 : " + total);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        switch (adapterView.getId()) {
            case R.id.spMyLabel:
                label = "";
                if (pos > 0) {
                    int idx = (txtWord.getSelectionEnd() >= 1) ? txtWord.getSelectionEnd() - 1 : 0;
                    label = ((TextView) view).getText().toString();
                    chosenLabels.set(idx, label);
                    spTone.setSelection(getTone(label), true);
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
        recorder = null;
        Log.d(TAG, "Stop Recording");
        // notify UI finish recording
        tvRecNOW.setText("");
        volView.removeView(circle);
        circle = null;
        // non UI
        File file = new File(path);
        if (file.exists()) {
            total++;
            tvTotal.setText("已錄 : " + total);
            recordedPath.addFirst(path);
            tvPath.setText(path);
            if (chkUpload.isChecked()) {
                loadingPage.show();
                JSONObject extra = new JSONObject();
                JSONArray zhuyin = new JSONArray();
                for (int i = 0; i < chosenLabels.size(); i++) {
                    zhuyin.put(chosenLabels.get(i));
                }
                try {
                    extra.put("zhuyin", zhuyin);
                } catch (JSONException e) {
                    e.printStackTrace();
                    extra = null;
                } finally {
                    new Recognition(mContext, path, mUIHandler, extra).start();
                }
            }
        }
    }

    @Override
    public void onFinishRecognition(String result, String filepath) {
        loadingPage.dismiss();
        File file = new File(filepath);
        String correctLabel = file.getParentFile().getName().replaceAll("[_˙ˊˇˋ]", "");
        try {
            JSONObject response = new JSONObject(result).getJSONObject("response");
            int numOfWord = response.getInt("success");
            boolean uploaded = response.getBoolean("uploaded");
            boolean flag = false;

            if (numOfWord > 0) {
                String myResult = "";
                JSONArray lists = response.getJSONArray("result_lists");
                if (numOfWord == 1) {
                    int pos = -1;
                    JSONArray jsonArray = lists.getJSONArray(0);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        myResult += jsonArray.getString(i) + ",";
                        if (correctLabel.contentEquals(jsonArray.getString(i))) {
                            pos = i + 1;
                            correct++;
                        }
                    }
                    myResult = "(" + pos + "/" + jsonArray.length() + ")\n" + myResult;
                } else {
                    for (int i = 0; i < numOfWord; i++) {
                        JSONArray jsonArray = lists.getJSONArray(i);
                        int pos = -1;
                        String label = chosenLabels.get(i).replaceAll("[˙ˊˇˋ_]", "");
                        for (int j = 0; j < jsonArray.length(); j++) {
                            String res = jsonArray.getString(j);
                            if (label.contentEquals(res)) {
                                pos = j + 1;
                                break;
                            }
                        }
                        myResult += "(" + pos + "/" + jsonArray.length() + "), ";
                    }
                    String sentence = response.getString("sentence");
                    myResult += "\n" + sentence;
                    if (correctLabel.contentEquals(sentence))
                        correct++;
                }

                myResult = myResult.replaceAll(",$", "");
                tvRes.setText(myResult);
                tvCorrect.setText("Accuracy : " + correct + " / " + total);

            } else {
                flag = true;
                file.delete();
                MediaScannerConnection.scanFile(mContext, new String[]{filepath}, null, null);
                recordedPath.removeFirst();
                String recorded = recordedPath.peekFirst();
                tvPath.setText(recorded);
                total--;
                tvTotal.setText("已錄 : " + total);
            }

            String root = Environment.getExternalStoragePublicDirectory("MyRecorder")
                    .getAbsolutePath();
            if (!flag && uploaded) {
                recordedPath.removeFirst();
                String relativePath = file.getAbsolutePath().replaceFirst(root + "/", "");
                String newPath = root + "/uploaded/" + relativePath;
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
