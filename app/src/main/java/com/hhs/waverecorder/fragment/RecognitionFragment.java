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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.Recognition;
import com.hhs.waverecorder.core.Updater;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.CursorChangedListener;
import com.hhs.waverecorder.listener.MyListener;
import com.hhs.waverecorder.receiver.MyReceiver;
import com.hhs.waverecorder.utils.MyFile;
import com.hhs.waverecorder.widget.VolumeCircle;
import com.hhs.waverecorder.widget.MyText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.hhs.waverecorder.receiver.MyReceiver.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.receiver.MyReceiver.RECORD_FINISHED_ACTION;

@SuppressWarnings("all")
public class RecognitionFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener, View.OnClickListener,
        MyListener, CursorChangedListener {

    private final String TAG = "## " + getClass().getName();
    private final static String CZTABLE = "czTable.json";
    private final static String ZCTABLE = "zcTable_noTone.json";
    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    MyReceiver eventReceiver = new MyReceiver();
    Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    volView.removeAllViews();
                    int level = msg.arg1;
                    VolumeCircle circle = new VolumeCircle(mContext, level, dpi);
                    volView.addView(circle);
            }
        }
    };

    //UI Variable
    Spinner spRecognition, spMyLabel;
    ListView lvWords, lvFunction;
    MyText txtMsg;
    ImageButton btnRec;
    FrameLayout volView;
    View recordingView;
    PopupWindow popupWindow;
    ProgressDialog loadingPage;
    ArrayList<String> recognitionList = new ArrayList<>(); // to store recognition zhuyin without tone
    ArrayList<String> wordList = new ArrayList<>(); // to show all possible chinese word according to selected no tone zhuyin
    ArrayList<String> displayLabelList = new ArrayList<>(); // to show all zhuyin with tone of the first chinese word behind cursor
    ArrayList<String> myLabelList = new ArrayList<>(); // store the choice for displayLabelList
    ArrayList<String> noToneLabelList = new ArrayList<>(); // store choice for recognitionList
    ArrayList<String> waveFiles = new ArrayList<>(); // store path of recording wave file if input is not by recording will store ""
    ArrayAdapter<String> ad2, ad3, ad4;

    //Global Data
    JSONObject zcTable, czTable/*chineseToZhuyin*/;
    int width, height, dpi; // device resolution in pixels used for UI

    //State Variable
    private boolean isRecord = false;
    private boolean isVoiceInput = false;
    private boolean txtClear = false;

    private void initUI() {

        // get resolution
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        dpi = dm.densityDpi;

        volView = mView.findViewById(R.id.volume);

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

        spRecognition = mView.findViewById(R.id.resultSpinner);
        spMyLabel = mView.findViewById(R.id.pronouceSpinner);
        lvWords = mView.findViewById(R.id.wordsList);
        lvFunction = mView.findViewById(R.id.fuctionList);
        txtMsg = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);

        // For Button
        btnRec.setOnClickListener(this);

        // For EditText or TextView
        txtMsg.setText("");
        txtMsg.setOnCursorChangedListener(this);
        txtMsg.addTextChangedListener(textWatcher);

        // For ListView or Spinner
        recognitionList.add("-");
        recognitionList.add("ㄧ");
        recognitionList.add("ㄨㄛ");
        recognitionList.add("ㄋㄧ");
        ArrayList<String> fList = new ArrayList<>(Arrays.asList("talk", "刪除"));

        ArrayAdapter<String> ad1 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, fList);
        lvFunction.setAdapter(ad1);
        lvFunction.setOnItemClickListener(this);

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

        ad3 = new ArrayAdapter<>(mContext, R.layout.myspinner, recognitionList);
        spRecognition.setAdapter(ad3);
        ad3.setDropDownViewResource(R.layout.myspinner);
        spRecognition.setOnItemSelectedListener(this);
        spRecognition.setSelection(0);

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
        mContext = getActivity();
        eventReceiver.setOnListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_recognition, container, false);
        initUI();
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RECORD_FINISHED_ACTION);
        intentFilter.addAction(RECOGNITION_FINISHED_ACTION);
        mContext.registerReceiver(eventReceiver, intentFilter);
        long start = System.currentTimeMillis();
        readTable();
        double duration = (double) (System.currentTimeMillis() - start) / 1000;
        Toast.makeText(mContext, "Loading Time : " + String.valueOf(duration) + " sec",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(eventReceiver);
        storeTable();
    }

    // read two tables
    private void readTable() {
        try {
            File pronounceToWord = new File(mContext.getFilesDir(), ZCTABLE);
            InputStream dictStream;
            if (pronounceToWord.exists())
                dictStream = mContext.openFileInput(ZCTABLE);
            else
                dictStream = mContext.getAssets().open(ZCTABLE);
            zcTable = readJSONStream(dictStream);
            File myDic = new File(mContext.getFilesDir(), CZTABLE);
            if (myDic.exists())
                dictStream = mContext.openFileInput(CZTABLE);
            else
                dictStream = mContext.getAssets().open(CZTABLE);
            czTable = readJSONStream(dictStream);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    // store ZCTABLE CZTABLE
    private void storeTable() {
        if (zcTable != null) {
            String outStr = zcTable.toString().replaceAll("(\\},)", "$0\n");
            MyFile.writeStringToFile(outStr, new File(mContext.getFilesDir(), ZCTABLE));
        }

        if (czTable != null) {
            String outStr = czTable.toString().replaceAll("(\\],)", "$0\n");
            MyFile.writeStringToFile(outStr, new File(mContext.getFilesDir(), CZTABLE));
        }
    }

    public static JSONArray sortJSONArrayByCount(JSONArray jsonArray, final boolean ascending) throws JSONException {
        List<JSONObject> array = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++)
            array.add(jsonArray.getJSONObject(i));
        Collections.sort(array, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String keyA = a.keys().next(), keyB = b.keys().next();
                try {
                    Integer valA = a.getInt(keyA);
                    Integer valB = b.getInt(keyB);
                    if (ascending)
                        return valA.compareTo(valB);
                    else
                        return -valA.compareTo(valB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        String jsonArrStr = "[";
        for (JSONObject json:array) {
            jsonArrStr += json.toString() + ",";
        }
        jsonArrStr = jsonArrStr.replaceAll(",$", "]");
        return new JSONArray(jsonArrStr);
    }

    // read JSON FileStream
    private JSONObject readJSONStream(InputStream inputStream) throws IOException, JSONException {
        BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = myReader.readLine()) != null) {
            sb.append(line);
        }
        String jsonStr = sb.toString();
        return new JSONObject(jsonStr);
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

    // update the count of an object in array of an object
    private JSONObject updateOAO(String key1, String key2, JSONObject item, String strIndex) throws JSONException {
        int count;
        JSONArray itemJSONArray = item.getJSONArray(strIndex);
        JSONObject changedItem = new JSONObject("{\"" + key2 + "\" : 1}"); // default value of array
        int index = itemJSONArray.length(); // default index of array
        for (int j = 0; j < index; j++) {
            JSONObject find = itemJSONArray.getJSONObject(j);
            if (key2.contentEquals(find.keys().next())) {
                count = find.getInt(key2) + 1;
                changedItem = find.put(key2, count);
                index = j;
                break;
            }
        }
        itemJSONArray.put(index, changedItem);
        return item.put(key1, itemJSONArray);
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

    // find pronounce of chinese word
    private ArrayList<String> lookCZTable(String word) throws JSONException {
        JSONObject item = czTable.getJSONObject(word);
        ArrayList<String> candidate = new ArrayList<>();
        JSONArray jsonArray = sortJSONArrayByCount(item.getJSONArray("pronounces"), false);
        for (int i = 0; i < jsonArray.length(); i++) {
            candidate.add(jsonArray.getJSONObject(i).keys().next());
        }
        return candidate;
    }
    
    // to get pronounce tone
    private int getTone(String pronounce) {
        String toneChar = pronounce.substring(pronounce.length() - 1, pronounce.length());
        int tone;
        switch (toneChar) {
            case "˙":
                tone = 0;
                break;
            case "ˊ":
                tone = 2;
                break;
            case "ˇ":
                tone = 3;
                break;
            case "ˋ":
                tone = 4;
                break;
            default:
                tone = 1;
                break;
        }
        return tone;
    }

    // software to trigger ListView onItemClick event
    private void clickItem(ListView listView, int pos) {
        listView.performItemClick(listView.getAdapter().getView(pos, null, null), pos, pos);
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
        spRecognition.setSelection(0);
        txtClear = false;
    }

    //====================UI Listener Start====================
    // ###STEP 6###
    TextWatcher textWatcher = new TextWatcher() {
        int cursor;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int beforecount, int aftercount) {
            cursor = txtMsg.getSelectionStart();
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
                        ArrayList<String> candidate = lookCZTable(ch);
                        if (candidate.size() > 0) {
                            String myLabel = candidate.get(0);
                            noToneLabelList.set(i, myLabel.replaceAll("[˙ˊˇˋ]$", ""));
                            myLabelList.set(i, myLabel);
                        }
                    } catch (JSONException e) {
                        Log.w(TAG, "no Mapping In czTable");
                    }
                }
            } else { // delete
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
                int position = txtMsg.getSelectionStart();
                String msg = txtMsg.getText().toString();
                String character = (position - 1 < 0) ? "" : msg.substring(position - 1, position);
                displayLabelList.clear();
                displayLabelList.add("-");
                int selectedIndex = 0;
                if (position > 0) {
                    Log.d(TAG, character);
                    try {
                        ArrayList<String> candidate = lookCZTable(character);
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
             case R.id.resultSpinner: // ###STEP 4###
                 wordList.clear();
                 if (position == 0) { // ###STEP 4-1###
                     ad2.notifyDataSetChanged();
                     break;
                 }
                 try {
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

             case R.id.pronouceSpinner: // ###STEP 8###
                 if (position == 0) // ###STEP 8-1###
                     break;
                 int msgPos = txtMsg.getSelectionStart();
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
        String select = ((TextView) view).getText().toString();
        switch (adapterView.getId()) {
            case R.id.fuctionList:
                switch (select) {
                    case "talk":
                        //talk();
                        debug();
                        break;
                    case "刪除":
                        txtClear = true;
                        txtMsg.setText("");
                        break;
                }
                break;

            case R.id.wordsList: // ###STEP 5###
                int msgPos = txtMsg.getSelectionStart();
                String noToneLabel = ((TextView) spRecognition.getSelectedView()).getText().toString();
                if (msgPos >= noToneLabelList.size()) {
                    noToneLabelList.add(noToneLabel);
                } else {
                    noToneLabelList.set(msgPos, noToneLabel);
                }
                String msg = txtMsg.getText().toString();
                String part1 = msg.substring(0, msgPos);
                String part2 = (msgPos + 1 > msg.length()) ? "" : msg.substring(msgPos + 1, msg.length());
                String text = part1 + select + part2; // modify the word behind cursor
                txtMsg.setText(text); // will trigger STEP 6 TextWatcher
                txtMsg.setSelection(part1.length() + select.length()); // trigger onCursorChanged event
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnRec:
                // ###STEP 1###
                SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                final String path = "MyRecorder/tmp/" + df.format(new Date()) + ".wav";

                Thread recorder = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        WAVRecorder wavRecorder = new WAVRecorder(path, mContext, mUIHandler);
                        Log.d(TAG, "Start Recording");
                        wavRecorder.startRecording();
                        try {
                            isRecord = true;
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            wavRecorder.stopRecording();
                            isRecord = false;
                            Log.d(TAG, "Finish Recording!");
                        }
                    }
                });
                if (!isRecord) {
                    //popupWindow.showAtLocation(mView, Gravity.CENTER, width/8*2, height/8*2);
                    recorder.start(); // ###STEP 1-1###
                }
                break;
        }
    }

    //=====================UI Listener End=====================

    //====================Speech Recognition Listener====================
    // ###STEP 2###
    @Override
    public void onFinishRecord(String path) {
        popupWindow.dismiss();
        volView.removeAllViews();
        isVoiceInput = true;
        waveFiles.add(txtMsg.getSelectionStart(), path);
        new Recognition(mContext, path, mUIHandler).start(); // ###STEP 2-1###
    }

    // ###STEP 3###
    @Override
    public void onFinishRecognition(String result, String correctLabel/*not used here*/) {
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
            ad3 = new ArrayAdapter<>(mContext, R.layout.myspinner, recognitionList);
            spRecognition.setAdapter(ad3);
            if (recognitionList.size() > 1) {
                spRecognition.setSelection(1); // ###STEP 3-1###
            } else {
                spRecognition.setSelection(0); // ###STEP 3-1###
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
