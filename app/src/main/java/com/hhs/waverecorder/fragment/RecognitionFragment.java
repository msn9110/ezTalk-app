package com.hhs.waverecorder.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.adapter.RadioItemViewAdapter;
import com.hhs.waverecorder.adapter.ViewHolder;
import com.hhs.waverecorder.core.RecognitionTask;
import com.hhs.waverecorder.core.Speaker;
import com.hhs.waverecorder.core.Updater;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.OnCursorChangedListener;
import com.hhs.waverecorder.listener.VoiceInputListener;
import com.hhs.waverecorder.receiver.VoiceInputEventReceiver;
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
import static com.hhs.waverecorder.MainActivity.showSoftKeyboard;
import static com.hhs.waverecorder.utils.Utils.*;

@SuppressWarnings("all")
public class RecognitionFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        View.OnClickListener, View.OnLongClickListener,
        OnCursorChangedListener, VoiceInputListener {

    // The method creating fragment pass some parameters
    // Not used in this app
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
    // The EventReceiver To trigger onFinishRecord and onFinishRecognition
    VoiceInputEventReceiver eventReceiver = new VoiceInputEventReceiver();
    // callback for another thread access UI
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
    Spinner spMyLabel;
    ListView lvWords, lvResults;
    MyText txtMsg;
    ImageButton btnRec;
    Button btnTalk, btnClear, btnBack, btnMoveCursor;
    FrameLayout volView;
    VolumeCircle circle = null;
    TextView tvRecNOW;
    ArrayList<String> recognitionList = new ArrayList<>(); // to store recognition zhuyin without tone
    ArrayList<String> wordsList = new ArrayList<>(); // to show all possible chinese word according to selected no tone zhuyin
    ArrayList<String> displayLabelList = new ArrayList<>(); // to show all zhuyin with tone of the first chinese word behind cursor
    ArrayList<String> myLabelList = new ArrayList<>(); // store the choice for displayLabelList
    ArrayList<String> noToneLabelList = new ArrayList<>(); // store choice for recognitionList
    ArrayList<String> waveFiles = new ArrayList<>(); // store path of recording wave file if input is not by recording will store ""
    ArrayList<ArrayList<String>> results = new ArrayList<>();
    ArrayList<Integer> resultsPos = new ArrayList<>();
    ArrayAdapter<String> ad2, ad4;
    RadioItemViewAdapter ad3;

    //Global Data
    JSONObject zcTable, czTable/*chineseToZhuyin*/;

    //Global Variable
    WAVRecorder recorder = null;
    Speaker speaker;
    ArrayList<String> rawFiles = new ArrayList<>();
    ArrayList<String> originalSentences = new ArrayList<>();

    //State Variable
    private boolean isVoiceInput = false;
    private boolean isInputByWordsList = false;
    private boolean isClear = false;
    private boolean longClick = false; // to insert new word
    private boolean modifiedByKeyboard = false;

    private void initUI() {
        volView = mView.findViewById(R.id.volume);
        tvRecNOW = mView.findViewById(R.id.tvRecNOW);

        lvResults = mView.findViewById(R.id.lvResults);
        spMyLabel = mView.findViewById(R.id.pronounceSpinner);
        lvWords = mView.findViewById(R.id.lvWords);
        txtMsg = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);
        btnTalk = mView.findViewById(R.id.btnTalk);
        btnClear = mView.findViewById(R.id.btnClear);
        btnBack = mView.findViewById(R.id.btnBack);
        btnMoveCursor = mView.findViewById(R.id.btnMoveCursor);

        // For Button
        btnRec.setOnClickListener(this);
        btnTalk.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnBack.setVisibility(View.GONE);
        btnMoveCursor.setOnClickListener(this);
        btnMoveCursor.setOnLongClickListener(this);

        // For EditText or TextView
        txtMsg.setOnCursorChangedListener(this);
        txtMsg.addTextChangedListener(textWatcher);
        txtMsg.setShowSoftInputOnFocus(false);

        // For ListView or Spinner
        recognitionList.clear();
        recognitionList.add("-");
        recognitionList.add("ㄧ");
        recognitionList.add("ㄨㄛ");
        recognitionList.add("ㄋㄧ");

        ad2 = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, wordsList){
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
        lvWords.setOnItemLongClickListener(this);

        ad3 = new RadioItemViewAdapter(mContext, recognitionList);
        lvResults.setAdapter(ad3);
        lvResults.setOnItemClickListener(this);
        lvResults.setOnItemLongClickListener(this);

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
        speaker = new Speaker(mContext);
        eventReceiver.setOnListener(this); // callback setting
        try {
            // read two dictionary
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
        speaker.stop();
        mContext.unregisterReceiver(eventReceiver);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speaker.shutdown();
    }

    // update word frequency
    private void updateFrequency() throws JSONException {
        String msg = txtMsg.getText().toString().replaceAll("[^\u4e00-\u9fa5]", "");
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
            zcTable = updateOAO(noToneLabel, word, zcTable, noToneLabel);
            // end of update myDic

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject tables = new JSONObject()
                                .put("zcTable", zcTable)
                                .put("czTable", czTable);
                        storeTable(tables, mContext);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }


    private void feedback() throws JSONException {
        JSONObject packet = new JSONObject();
        boolean update_files = false;
        JSONArray filesNeedToMove = new JSONArray();
        String msg = txtMsg.getText().toString();
        ArrayList<String> files = new ArrayList<>();
        packet.put("sentence", msg);
        JSONArray zhuyin = new JSONArray();
        for (int i = 0; i < msg.length(); i++) {
            zhuyin.put(myLabelList.get(i));
        }
        packet.put("zhuyin", zhuyin);
        for (int i = 0; i < rawFiles.size(); i++) {
            String modified = "";
            String mappedFilename = rawFiles.get(i);
            for (int j = 0; j < waveFiles.size(); j++) {
                if (waveFiles.get(j).endsWith(mappedFilename)) {
                    modified += msg.charAt(j);
                }
            }
            if (modified.length() > 0) {
                update_files = true;
                String original = this.originalSentences.get(i);
                JSONObject itemContent = new JSONObject();
                itemContent.put("original", original);
                itemContent.put("modified", modified);
                JSONObject item = new JSONObject();
                item.put(mappedFilename, itemContent);
                filesNeedToMove.put(item);
                String originalPath = Environment.getExternalStoragePublicDirectory("MyRecorder")
                                      + "/tmp/" + mappedFilename;
                String newPath = Environment.getExternalStoragePublicDirectory("MyRecorder")
                                 + "/uploaded/sentence/" + modified + "/" + mappedFilename;
                files.add(originalPath);
                files.add(newPath);

            }
        }
        rawFiles.clear();
        originalSentences.clear();
        packet.put("streamFilesMove", filesNeedToMove);

        JSONArray syllableFilesMove = new JSONArray();
        for (int i = 0; i < waveFiles.size(); i++) {
            String sourcePath = waveFiles.get(i);
            if (sourcePath.length() > 0) {
                File file = new File(sourcePath);
                String myLabel = myLabelList.get(i);
                String noToneLabel = myLabel.replaceAll("[˙ˊˇˋ]", "");
                String tone = String.valueOf(getTone(myLabel));
                waveFiles.set(i, "");
                JSONObject itemContent = new JSONObject();
                itemContent.put("label", noToneLabel);
                itemContent.put("tone", tone);
                JSONObject item = new JSONObject();
                item.put(file.getName(), itemContent);
                syllableFilesMove.put(item);
            }
        }
        packet.put("syllableFilesMove", syllableFilesMove);
        packet.put("update_files", update_files);
        //// TODO: 2018/4/21 add control for local recognition

        new Updater(mContext, packet, files, null).start();

        updateFrequency();
    }

    private void talk() {
        debug();
        Thread feedbacker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    feedback();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        feedbacker.start();
        speaker.addSpeak(txtMsg.getText().toString());
    }

    // software to trigger ListView onItemClick event
    private void clickItem(ListView listView, int pos) {
        listView.performItemClick(listView.getAdapter().getView(pos, null, null), pos, pos);
    }

    // For Clear
    private void clear() {
        displayLabelList.clear();
        waveFiles.clear();
        recognitionList.clear();
        myLabelList.clear();
        noToneLabelList.clear();
        results.clear();
        resultsPos.clear();
        rawFiles.clear();
        originalSentences.clear();
        recognitionList.add("-");
        displayLabelList.add("-");
        ad3.notifyDataSetChanged();
        ad4.notifyDataSetChanged();
        spMyLabel.setSelection(0);
        clickItem(lvResults, 0); // will affect wordsList and ad2
        isClear = false;
    }

    private void loadRecord(int position) {
        recognitionList.clear();
        recognitionList.addAll(results.get(position - 1));
        int selected = resultsPos.get(position - 1);
        ad3.setSelectPosition(selected);
        String noToneLabel = recognitionList.get(selected);
        wordsList.clear();
        try {
            wordsList.addAll(lookTable(zcTable, noToneLabel, ""));
        } catch (JSONException e) {
            Log.w(TAG, "onCursorChanged1");
        }
        ad2.notifyDataSetChanged();
    }

    private void onFinishModifiedByKeyboard(int position, String character) {
        try {
            int recognitionListChosen = 0;
            ArrayList<String> candidate = lookTable(czTable, character, "pronounces");
            for (String label:candidate) {
                String noToneLabel = label.replaceAll("[˙ˊˇˋ]", "");
                if (recognitionList.contains(noToneLabel) && recognitionListChosen == 0) {
                    recognitionListChosen = recognitionList.indexOf(noToneLabel);
                    noToneLabelList.set(position - 1, noToneLabel);
                    myLabelList.set(position - 1, label);
                }
                if (!recognitionList.contains(noToneLabel)) {
                    recognitionList.add(noToneLabel);
                    if (recognitionListChosen == 0) {
                        recognitionListChosen = recognitionList.size() - 1;
                        noToneLabelList.set(position - 1, noToneLabel);
                        myLabelList.set(position - 1, label);
                    }

                }
            }
            ad3.notifyDataSetChanged();
            ad3.setSelectPosition(recognitionListChosen);
            resultsPos.set(position - 1, recognitionListChosen);
            ArrayList<String> clone = new ArrayList<>(recognitionList);
            results.set(position - 1, clone);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayMyLabel() {
        int position = txtMsg.getSelectionEnd();
        String msg = txtMsg.getText().toString();
        String character = (position - 1 < 0) ? "" : msg.substring(position - 1, position);
        displayLabelList.clear();
        displayLabelList.add("-");
        int selectedIndex = 0;

        if (position > 0) {
            Log.d(TAG, "cursorPosition : " + position);
            if (!isInputByWordsList && !modifiedByKeyboard) {
                loadRecord(position);
            }

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
                Log.w(TAG, "onCursorChanged2");
            }
        }
        ad4.notifyDataSetChanged();
        spMyLabel.setSelection(selectedIndex, true);
    }

    //====================UI Listener Start====================
    // ###STEP 6###
    TextWatcher textWatcher = new TextWatcher() {
        int cursor;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            Log.d(TAG, "Trigger TextWatcher");
            cursor = txtMsg.getSelectionEnd();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            boolean insertMode = aftercount > beforecount;
            Log.d(TAG, "TextWatcher : " + s.toString());
            start = cursor;
            if (modifiedByKeyboard) {
                String ch = txtMsg.getText().toString().substring(start - 1, start);
                Log.d(TAG, "modify to : " + ch);
                onFinishModifiedByKeyboard(start, ch);
                Log.d(TAG, "finish modify");
                modifiedByKeyboard = false;
                displayMyLabel();
            }

            int endPos = insertMode ? start + aftercount - beforecount : start + beforecount - aftercount;
            if (insertMode && !isInputByWordsList) { // insert mode
                String addedText = s.toString().substring(start, endPos);
                for (int i = start; i < endPos; i++) {
                    String ch = addedText.substring(i - start, i - start + 1);
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add("-");
                    waveFiles.add(i, "");
                    // avoid no mapping in czTable
                    noToneLabelList.add(i, "unknown");
                    myLabelList.add(i, "unknown");
                    int selection = 0;
                    try {
                        ArrayList<String> candidate = lookTable(czTable, ch, "pronounces");
                        if (candidate.size() > 0) {
                            String myLabel = candidate.get(0);
                            String noToneLabel = myLabel.replaceAll("[˙ˊˇˋ]$", "");
                            noToneLabelList.set(i, noToneLabel);
                            myLabelList.set(i, myLabel);
                            for (String label:candidate) {
                                noToneLabel = label.replaceAll("[˙ˊˇˋ]$", "");
                                if (!tmp.contains(noToneLabel))
                                    tmp.add(noToneLabel);
                            }
                            selection = 1;
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "no Mapping In czTable");
                    }
                    results.add(i, tmp);
                    resultsPos.add(selection);
                }
            } else if (!insertMode) { // delete
                if (!isClear) {
                    for (int i = start - 1; i < endPos - 1; i++) {
                        waveFiles.remove(i);
                        myLabelList.remove(i);
                        noToneLabelList.remove(i);
                        results.remove(i);
                        resultsPos.remove(i);
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
                displayMyLabel();
                break;
        }
    }

    // For Spinner onItemSelected event handler
    @Override
     public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
         String select = ((TextView) view).getText().toString();

         switch (adapterView.getId()) {

             case R.id.pronounceSpinner: // ###STEP 8###
                 Log.d(TAG, "spinner : " + position);
                 if (position > 0 && !isVoiceInput) {
                     int msgPos = txtMsg.getSelectionEnd();
                     boolean insertMode = isInputByWordsList;
                     // ###STEP 8-1###
                     if (insertMode) {
                         Log.d(TAG, "spinner insert my label");
                         if (msgPos >= myLabelList.size())
                             myLabelList.add(select);
                         else
                            myLabelList.add(msgPos - 1, select);
                     } else {
                         myLabelList.set(msgPos - 1, select);
                     }
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
            case R.id.lvResults: // ###STEP 4###
                select = ((ViewHolder) view.getTag()).name.getText().toString();
                lvResultsItemClick(select, position, false);
                break;

            case R.id.lvWords: // ###STEP 5###
                int msgPos = txtMsg.getSelectionEnd();
                select = ((TextView) view).getText().toString();
                // longClick or msgPos == 0 enter insert mode
                boolean insertMode = isVoiceInput || longClick || msgPos == 0;
                lvWordsItemClick(select, insertMode);
                break;
        }
    }

    // longClick indicates insert
    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        String select;
        switch (adapterView.getId()) {
            case R.id.lvResults:
                select = ((ViewHolder) view.getTag()).name.getText().toString();
                lvResultsItemClick(select, position, true);
                break;

            case R.id.lvWords:
                select = ((TextView) view).getText().toString();
                lvWordsItemClick(select, true);
                break;
        }
        return false;
    }

    private void lvResultsItemClick(String select, int position, boolean longClick) {
        wordsList.clear();
        ad3.setSelectPosition(position);
        if (position == 0) {
            if (txtMsg.getSelectionEnd() > 0) {
                if (longClick) {
                    int cursor = txtMsg.getSelectionEnd();
                    modifiedByKeyboard = true;
                    txtMsg.setSelection(cursor - 1, cursor);
                    showSoftKeyboard(txtMsg, mContext);
                } else {
                    final EditText editText = new EditText(mContext);
                    int pos = txtMsg.getSelectionEnd();
                    String clip_name = waveFiles.get(pos - 1);
                    if (clip_name.length() > 0) {
                        String suffix = clip_name.split("-")[2];
                        String stn = "";
                        String msg = txtMsg.getText().toString();
                        for (int i = 0; i < msg.length(); i++) {
                            String file = waveFiles.get(i);
                            if (file.endsWith(suffix)) {
                                stn += msg.substring(i, i + 1);
                            }
                        }
                    }
                }
            }
            ad2.notifyDataSetChanged();
            return;
        }

        if (!modifiedByKeyboard) {
            try {
                wordsList.addAll(lookTable(zcTable, select, ""));
                ad2.notifyDataSetChanged();
                if (wordsList.size() > 0) {
                    this.longClick = longClick;
                    clickItem(lvWords, 0);
                    this.longClick = false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private void lvWordsItemClick(String word, boolean insertMode) {
        isInputByWordsList = insertMode;
        int msgPos = txtMsg.getSelectionEnd();
        String noToneLabel = recognitionList.get(ad3.getSelectPosition());
        if (insertMode) {
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.addAll(recognitionList);
            results.add(msgPos, tmp);
            resultsPos.add(msgPos, ad3.getSelectPosition());
            noToneLabelList.add(msgPos, noToneLabel);
        } else {
            Log.d(TAG, "## lvWordsClick : " + msgPos);
            resultsPos.set(msgPos - 1, ad3.getSelectPosition());
            noToneLabelList.set(msgPos - 1, noToneLabel);
            myLabelList.set(msgPos -1, noToneLabel);
        }
        if (!isVoiceInput && insertMode) {
            // insert by wordlist and not voice input
            waveFiles.add(msgPos, "");
        }

        String msg = txtMsg.getText().toString();
        String part1 = (msgPos == 0) ? "" : msg.substring(0, msgPos - 1);
        String part2 = msg.substring(msgPos, msg.length());
        if (insertMode) // insert word by list
            part1 = msg.substring(0, msgPos);
        String text = part1 + word + part2; // modify the word in front of cursor
        Log.d(TAG, part1 + " + " + word + " + " + part2 + " : " + text);
        txtMsg.setText(text); // will trigger STEP 6 TextWatcher
        if (insertMode)
            txtMsg.setSelection(part1.length() + word.length()); // trigger onCursorChangedevent
        else
            txtMsg.setSelection(msgPos);
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.btnMoveCursor:
                if (txtMsg.length() > 0)
                    txtMsg.setSelection(1);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRec:
                // ###STEP 1###
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
                String path = "MyRecorder/tmp/" + df.format(new Date()) + ".wav";
                if (recorder == null) {
                    recorder = new WAVRecorder(mContext, path, -1, mUIHandler);
                    circle = new VolumeCircle(mContext, 0);
                    volView.addView(circle);
                    Log.d(TAG, "Start Recording");
                    recorder.startRecording(); // ###STEP 1-1###
                } else if (recorder != null && recorder.isRecordNow()) {
                    recorder.stopRecording();
                }
                break;

            case R.id.btnTalk:
                talk();
                //debug();
                break;

            case R.id.btnClear:
                isClear = true;
                txtMsg.setText("");
                break;

            case R.id.btnMoveCursor:
                txtMsg.setSelection((txtMsg.getSelectionEnd() + 1) % (txtMsg.length() + 1));
                break;

            case R.id.btnBack:
                int cursorPos = txtMsg.getSelectionEnd();
                if (cursorPos > 0) {
                    String msg = txtMsg.getText().toString();
                    String part1 = msg.substring(0, cursorPos - 1);
                    String part2 = msg.substring(cursorPos);
                    txtMsg.setText(part1 + part2); // trigger STEP 6
                    txtMsg.setSelection(cursorPos - 1); // trigger STEP 7
                }
                break;
        }
    }

    //=====================UI Listener End=====================

    //====================Speech Recognition Listener====================
    // ###STEP 2###
    @Override
    public void onFinishRecord(String path) {
        recorder = null;
        Log.d(TAG, "Stop Recording");
        // notify UI finish recording
        tvRecNOW.setText("");
        volView.removeView(circle);
        circle = null;
        // non UI
        isVoiceInput = true;
        if (new File(path).exists()) {

            RecognitionTask task = new RecognitionTask(mContext);
            task.execute(new File(path));
        }
    }

    // ###STEP 3###
    @Override
    public void onFinishRecognition(String result, String filepath) {
        try {
            File file = new File(filepath);
            JSONObject jsonObject = new JSONObject(result);
            JSONObject response = jsonObject.getJSONObject("response");
            int numOfWord = response.getInt("success");
            int cursor = txtMsg.getSelectionEnd();

            if (numOfWord > 0) {
                JSONArray lists = response.getJSONArray("result_lists");
                JSONArray usedIndexes = response.getJSONArray("usedIndexes");
                String sentence = response.getString("sentence");
                originalSentences.add(sentence);
                rawFiles.add(file.getName());
                Log.i(TAG, sentence);
                for (int i = 0; i < numOfWord; i++) {
                    int index = usedIndexes.getInt(i);
                    recognitionList.clear();
                    recognitionList.add("-");
                    waveFiles.add(txtMsg.getSelectionEnd(), "clip-stream" + String.valueOf(i + 1)
                    + "-" + file.getName());
                    JSONArray results = lists.getJSONArray(i);
                    for (int j = 0; j < results.length(); j++) {
                        String pronounce = results.getString(j);
                        if (!pronounce.startsWith("_")) {
                            recognitionList.add(pronounce);
                        }
                    }
                    ad3.notifyDataSetChanged();
                    if (recognitionList.size() > 1) {
                        clickItem(lvResults, index); // ###STEP 3-1###
                    } else {
                        clickItem(lvResults, 0); // ###STEP 3-1###
                    }
                    // post processing replace word according to sentence
                    String msg = txtMsg.getText().toString();
                    String text1 = msg.substring(0, cursor + i);
                    String text2 = msg.substring(cursor + i + 1);
                    String replacement = sentence.substring(i, i + 1);
                    msg = text1 + replacement + text2;
                    int msgPos = cursor + i;
                    String noToneLabel = noToneLabelList.get(cursor + i);
                    ArrayList<String> candidate = lookTable(czTable, replacement, "pronounces");
                    for (int j = 0; j < candidate.size(); j++) {
                        String label = candidate.get(j);
                        if (label.startsWith(noToneLabel)) {
                            if (msgPos >= myLabelList.size())
                                myLabelList.add(label);
                            else
                                myLabelList.add(msgPos, label);
                            break;
                        }
                    }
                    isInputByWordsList = false;
                    txtMsg.setText(msg);
                    txtMsg.setSelection(cursor + i + 1);
                    Log.i(TAG, "replace word:" + String.valueOf(msgPos + 1));
                }
                txtMsg.setSelection(cursor + 1);
                txtMsg.setSelection(txtMsg.getText().length());
                isVoiceInput = false;

                debug();
                Log.i(TAG, "Finish Recognition");
            } else {
                file.delete();
                MediaScannerConnection.scanFile(mContext, new String[]{filepath}, null, null);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ###STEP 9###
    private void onFinishAllStep() {
        isInputByWordsList = false;
    }

    private void debug() {
        Log.d(TAG, String.valueOf(waveFiles.size()));
        Log.d(TAG, String.valueOf(myLabelList.size()));
        Log.d(TAG, String.valueOf(noToneLabelList.size()));
        for (int i = 0; i < waveFiles.size(); i++) {
            String msg = i + " : " + waveFiles.get(i) + " : " + noToneLabelList.get(i) + " : "
                    + myLabelList.get(i) + "\n";
            Log.d("## state", msg);
        }
    }
}
