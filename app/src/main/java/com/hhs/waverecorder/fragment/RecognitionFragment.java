package com.hhs.waverecorder.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.adapter.RadioItemViewAdapter;
import com.hhs.waverecorder.adapter.ViewHolder;
import com.hhs.waverecorder.core.Recognition;
import com.hhs.waverecorder.core.Updater;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.OnCursorChangedListener;
import com.hhs.waverecorder.listener.VoiceInputListener;
import com.hhs.waverecorder.receiver.VoiceInputEventReceiver;
import com.hhs.waverecorder.utils.MyFile;
import com.hhs.waverecorder.widget.VolumeCircle;
import com.hhs.waverecorder.widget.MyText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.hhs.waverecorder.AppValue.*;
import static com.hhs.waverecorder.utils.Utils.getTone;
import static com.hhs.waverecorder.utils.Utils.lookTable;
import static com.hhs.waverecorder.utils.Utils.readTables;
import static com.hhs.waverecorder.utils.Utils.sortJSONArrayByCount;
import static com.hhs.waverecorder.utils.Utils.storeTable;
import static com.hhs.waverecorder.utils.Utils.updateOAO;

@SuppressWarnings("all")
public class RecognitionFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener, View.OnClickListener,
        OnCursorChangedListener, VoiceInputListener {

    public static RecognitionFragment newInstance(String czJSONString, String zcJSONString) {
        RecognitionFragment mFragment = new RecognitionFragment();
        Bundle args = new Bundle();
        args.putString("czJSONString", czJSONString);
        args.putString("zcJSONString", zcJSONString);
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
    Spinner spMyLabel;
    ListView lvWords, lvResult;
    MyText txtMsg;
    ImageButton btnRec;
    Button btnTalk, btnClear;
    FrameLayout volView;
    VolumeCircle circle = null;
    TextView tvRecNOW;
    View recordingView;
    PopupWindow popupWindow;
    ProgressDialog loadingPage;
    ArrayList<String> recognitionList = new ArrayList<>(); // to store recognition zhuyin without tone
    ArrayList<String> wordList = new ArrayList<>(); // to show all possible chinese word according to selected no tone zhuyin
    ArrayList<String> displayLabelList = new ArrayList<>(); // to show all zhuyin with tone of the first chinese word behind cursor
    ArrayList<String> myLabelList = new ArrayList<>(); // store the choice for displayLabelList
    ArrayList<String> noToneLabelList = new ArrayList<>(); // store choice for recognitionList
    ArrayList<String> waveFiles = new ArrayList<>(); // store path of recording wave file if input is not by recording will store ""
    ArrayAdapter<String> ad2, ad4;
    RadioItemViewAdapter ad3;

    //Global Data
    JSONObject zcTable, czTable/*chineseToZhuyin*/;
    int width, height, dpi; // device resolution in pixels used for UI

    //State Variable
    private boolean isRecord = false;
    private boolean isVoiceInput = false;
    private boolean txtClear = false;
    private int recordingDot = 0; // max 2

    private void initUI() {

        // get resolution
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        dpi = dm.densityDpi;

        volView = mView.findViewById(R.id.volume);
        tvRecNOW = mView.findViewById(R.id.tvRecNOW);

        // popup window
        LayoutInflater inflater = LayoutInflater.from(mContext);
        recordingView = inflater.inflate(R.layout.recording_now, null);
        popupWindow = new PopupWindow(recordingView, width/4, height/4, true);
        popupWindow.setTouchable(true);
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        popupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));

        // loading page
        loadingPage = new ProgressDialog(mContext);
        loadingPage.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingPage.setTitle("載入資料中");
        loadingPage.setMessage("請稍候");

        lvResult = mView.findViewById(R.id.lvResult);
        spMyLabel = mView.findViewById(R.id.pronouceSpinner);
        lvWords = mView.findViewById(R.id.lvWords);
        txtMsg = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);
        btnTalk = mView.findViewById(R.id.btnTalk);
        btnClear = mView.findViewById(R.id.btnClear);

        // For Button
        btnRec.setOnClickListener(this);
        btnTalk.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        // For EditText or TextView
        txtMsg.setOnCursorChangedListener(this);
        txtMsg.addTextChangedListener(textWatcher);

        // For ListView or Spinner
        recognitionList.add("-");
        recognitionList.add("ㄧ");
        recognitionList.add("ㄨㄛ");
        recognitionList.add("ㄋㄧ");

        ad2 = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, wordList){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textview = view.findViewById(android.R.id.text1);

                //Set your Font Size Here.
                textview.setTextSize(30);
                textview.setGravity(Gravity.CENTER);

                return view;
            }
        };
        lvWords.setAdapter(ad2);
        lvWords.setOnItemClickListener(this);

        ad3 = new RadioItemViewAdapter(mContext, recognitionList);
        lvResult.setAdapter(ad3);
        lvResult.setOnItemClickListener(this);
        clickItem(lvResult, 0);

        displayLabelList.add("-");
        ad4 = new ArrayAdapter<>(mContext, R.layout.myspinner, displayLabelList);
        spMyLabel.setAdapter(ad4);
        ad4.setDropDownViewResource(R.layout.myspinner);
        spMyLabel.setOnItemSelectedListener(this);
        spMyLabel.setSelection(0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate : " + Thread.currentThread().getId());
        mContext = getActivity();
        eventReceiver.setOnListener(this);
        try {
            JSONObject tables = readTables(mContext);
            czTable = tables.getJSONObject("czTable");
            zcTable = tables.getJSONObject("zcTable");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.fragment_recognition, container, false);
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
        try {
            JSONObject tables = new JSONObject()
                                .put("zcTable", zcTable)
                                .put("czTable", czTable);
            storeTable(tables);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // update word frequency
    private void updateFrequency() throws JSONException {
        String msg = txtMsg.getText().toString();
        for (int i = 0; i < myLabelList.size(); i++) {
            // start to update czTable
            String word = msg.substring(i, i + 1);
            String myLabel = myLabelList.get(i);
            JSONObject item = czTable.getJSONObject(word);
            int count = item.getInt("count") + 1;
            item.put("count", count);
            item = updateOAO(word, myLabel, item, "pronounces");
            czTable.put(word, item);
            // end of update czTable

            // start to update myDic
            String noToneLabel = myLabel.replaceAll("[˙ˊˇˋ]", "");
            zcTable = updateOAO(noToneLabel, myLabel, zcTable, noToneLabel);
            // end of update myDic
        }
    }


    private void talk() {
        JSONObject packet = new JSONObject();
        ArrayList<File> movedFiles = new ArrayList<>();
        for (int i = 0; i < waveFiles.size(); i++) {
            String sourcePath = waveFiles.get(i);
            if (sourcePath.length() > 0) {
                File file = new File(sourcePath);
                String myLabel = myLabelList.get(i);
                String noToneLabel = myLabel.replaceAll("[˙ˊˇˋ]", "");
                String tone = String.valueOf(getTone(myLabel)) + '-';
                String newPath = Environment.getExternalStoragePublicDirectory("MyRecorder")
                        + "/" + noToneLabel + "/" + tone + file.getName();
                if (MyFile.moveFile(sourcePath, newPath)) {
                    movedFiles.add(new File(sourcePath));
                }
                MediaScannerConnection.scanFile(mContext, new String[] {sourcePath, newPath}, null, null);
                waveFiles.set(i, "");
                JSONObject item = new JSONObject();
                try {
                    item.put("label", noToneLabel);
                    item.put("filename", file.getName());
                    item.put("tone", tone);
                    packet.put(String.valueOf(i), item);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        //// TODO: 2018/4/21 add control for local recognition
        if (movedFiles.size() > 0) {
            new Updater(mContext, packet, movedFiles).start();
        }

        try {
            updateFrequency();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    // software to trigger ListView onItemClick event
    private void clickItem(ListView listView, int pos) {
        listView.performItemClick(listView.getAdapter().getView(pos, null, null), pos, pos);
        if (listView == lvResult)
            ad3.setSelectPosition(pos);
    }

    // For Clear
    private void clear() {
        displayLabelList.clear();
        waveFiles.clear();
        wordList.clear();
        recognitionList.clear();
        myLabelList.clear();
        noToneLabelList.clear();
        recognitionList.add("-");
        displayLabelList.add("-");
        ad2.notifyDataSetChanged();
        ad3.notifyDataSetChanged();
        ad4.notifyDataSetChanged();
        spMyLabel.setSelection(0);
        clickItem(lvResult, 0);
        txtClear = false;
    }

    //====================UI Listener Start====================
    // ###STEP 6###
    TextWatcher textWatcher = new TextWatcher() {
        int cursor;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            cursor = txtMsg.getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            boolean insertMode = aftercount > beforecount;
            start = cursor;
            int endPos = insertMode ? start + aftercount - beforecount : start + beforecount - aftercount;
            if (insertMode && !isVoiceInput) { // insert mode
                String addedText = s.toString().substring(start, endPos);
                for (int i = start; i < endPos; i++) {
                    String ch = addedText.substring(i - start, i - start + 1);
                    waveFiles.add(i, "");
                    // avoid no mapping in czTable
                    noToneLabelList.add(i, "unknown");
                    myLabelList.add(i, "unknown");
                    try {
                        ArrayList<String> candidate = lookTable(czTable, ch, "pronounces");
                        if (candidate.size() > 0) {
                            String myLabel = candidate.get(0);
                            noToneLabelList.set(i, myLabel.replaceAll("[˙ˊˇˋ]$", ""));
                            myLabelList.set(i, myLabel);
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "no Mapping In czTable");
                    }
                }
            } else if (!insertMode) { // delete
                if (!txtClear) {
                    for (int i = start; i < endPos; i++) {
                        waveFiles.remove(i);
                        myLabelList.remove(i);
                        noToneLabelList.remove(i);
                    }
                } else {
                    clear();
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    // detect the pronounce of the word in front of the cursor
    @Override
    public void onCursorChanged(View view) {
        switch (view.getId()) {
            case R.id.txtMsg: // ###STEP 7###
                int position = txtMsg.getSelectionEnd();
                String msg = txtMsg.getText().toString();
                String character = (position - 1 < 0) ? "" : msg.substring(position - 1, position);
                displayLabelList.clear();
                displayLabelList.add("-");
                int selectedIndex = 0;
                if (position > 0) {
                    Log.d(TAG, character);
                    try {
                        ArrayList<String> candidate = lookTable(czTable, character, "pronounces");
                        String myLabel = (position > myLabelList.size()) ? noToneLabelList.get(position - 1)
                                                                            : myLabelList.get(position - 1);
                        for (int i = 0; i < candidate.size(); i++) {
                            String label = candidate.get(i);
                            displayLabelList.add(label);
                            if (label.startsWith(myLabel) && selectedIndex == 0) {
                                selectedIndex = i + 1;
                            }
                        }
                        if (candidate.size() > 0 && selectedIndex == 0)
                            selectedIndex = 1;
                    } catch (JSONException e) {
                        Log.w(TAG, "onCursorChanged");
                    }
                }

                ad4.notifyDataSetChanged();
                spMyLabel.setSelection(selectedIndex, true);
                break;
        }
    }

    // For Spinner onItemSelected event handler
    @Override
     public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
         String select = ((TextView) view).getText().toString();

         switch (adapterView.getId()) {

             case R.id.pronouceSpinner: // ###STEP 8###
                 if (position == 0) // ###STEP 8-1###
                     break;
                 int msgPos = txtMsg.getSelectionEnd();
                 // ###STEP 8-1###
                 if (msgPos >= myLabelList.size()) {
                     myLabelList.add(select);
                 } else if (isVoiceInput) {
                     myLabelList.add(msgPos, select);
                 } else {
                     myLabelList.set(msgPos - 1, select);
                 }

                 onFinishAllStep();
                 break;
         }
     }

     @Override
     public void onNothingSelected(AdapterView<?> adapterView) {

     }

     // For ListView onItemClick event handler
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        String select;
        switch (adapterView.getId()) {
            case R.id.lvResult: // ###STEP 4###
                wordList.clear();
                ad3.setSelectPosition(position);
                if (position == 0) { // ###STEP 4-1###
                    ad2.notifyDataSetChanged();
                    break;
                }
                try {
                    select = ((ViewHolder) view.getTag()).name.getText().toString();
                    JSONArray jsonArray = sortJSONArrayByCount(zcTable.getJSONArray(select), false);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String key = jsonArray.getJSONObject(i).keys().next();
                        wordList.add(key);
                    }
                    ad2.notifyDataSetChanged();
                    if (wordList.size() > 0) {
                        clickItem(lvWords, 0); // ###STEP 4-1###
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.lvWords: // ###STEP 5###
                int msgPos = txtMsg.getSelectionEnd();
                String noToneLabel = recognitionList.get(ad3.getSelectPosition());
                if (msgPos >= noToneLabelList.size()) {
                    noToneLabelList.add(noToneLabel);
                } else {
                    noToneLabelList.set(msgPos, noToneLabel);
                }
                select = ((TextView) view).getText().toString();
                lvWordsItemClick(select, msgPos == txtMsg.length());
                break;
        }
    }

    private void lvWordsItemClick(String word, boolean changeCursor) {
        int msgPos = txtMsg.getSelectionEnd();
        String msg = txtMsg.getText().toString();
        String part1 = msg.substring(0, msgPos);
        String part2 = (msgPos + 1 > msg.length()) ? "" : msg.substring(msgPos + 1, msg.length());
        if (changeCursor) // insert word by list
            part2 = msg.substring(msgPos, msg.length());
        String text = part1 + word + part2; // modify the word behind cursor
        txtMsg.setText(text); // will trigger STEP 6 TextWatcher
        if (isVoiceInput || changeCursor)
            txtMsg.setSelection(part1.length() + word.length()); // trigger onCursorChangedevent
        else
            txtMsg.setSelection(part1.length());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRec:
                // ###STEP 1###
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                String path = "MyRecorder/tmp/" + df.format(new Date()) + ".wav";
                WAVRecorder recorder = new WAVRecorder(mContext, path, 2500, mUIHandler);
                if (!isRecord) {
                    circle = new VolumeCircle(mContext, 0, dpi);
                    volView.addView(circle);
                    isRecord = true;
                    Log.d(TAG, "Start Recording");
                    recorder.startRecording(); // ###STEP 1-1###
                }
                break;

            case R.id.btnTalk:
                //talk();
                debug();
                break;

            case R.id.btnClear:
                txtClear = true;
                txtMsg.setText("");
                break;
        }
    }

    //=====================UI Listener End=====================

    //====================Speech Recognition Listener====================
    // ###STEP 2###
    @Override
    public void onFinishRecord(String path) {
        isRecord = false;
        Log.d(TAG, "Stop Recording");
        // notify UI finish recording
        tvRecNOW.setText("");
        volView.removeView(circle);
        circle = null;
        // non UI
        isVoiceInput = true;
        waveFiles.add(txtMsg.getSelectionEnd(), path);
        new Recognition(mContext, path, mUIHandler).start(); // ###STEP 2-1###
    }

    // ###STEP 3###
    @Override
    public void onFinishRecognition(String result, String filepath/*not used here*/) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject response = jsonObject.getJSONObject("response");
            boolean success = response.getBoolean("success");
            recognitionList.clear();
            recognitionList.add("-");

            if (success) {
                JSONArray results = response.getJSONArray("result");
                for (int i = 0; i < results.length(); i++) {
                    String pronounce = results.getString(i);
                    if (!pronounce.startsWith("_")) {
                        recognitionList.add(pronounce);
                    }
                }
            }
            ad3.notifyDataSetChanged();
            if (recognitionList.size() > 1) {
                clickItem(lvResult, 1); // ###STEP 3-1###
            } else {
                clickItem(lvResult, 1); // ###STEP 3-1###
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ###STEP 9###
    private void onFinishAllStep() {
        if (isVoiceInput) {
            isVoiceInput = false;
        }
    }

    private void debug() {
        for (int i = 0; i < waveFiles.size(); i++) {
            String msg = i + " : " + waveFiles.get(i) + " : " + noToneLabelList.get(i) + " : "
                    + myLabelList.get(i) + "\n";
            Log.d("## state", msg);
        }
    }
}
