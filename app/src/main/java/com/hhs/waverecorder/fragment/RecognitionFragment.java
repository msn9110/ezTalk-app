package com.hhs.waverecorder.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.Recognition;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.CursorChangedListener;
import com.hhs.waverecorder.listener.MyListener;
import com.hhs.waverecorder.receiver.MyReceiver;
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
import java.util.Date;
import java.util.Locale;

import static com.hhs.waverecorder.receiver.MyReceiver.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.receiver.MyReceiver.RECORD_FINISHED_ACTION;


public class RecognitionFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener, MyListener, CursorChangedListener {

    private final String TAG = "## " + getClass().getName();
    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    MyReceiver eventReceiver = new MyReceiver();
    Handler mUIHandler = new Handler();

    //UI Variable
    Spinner spRecognition, spMyLabel;
    ListView lvWords, lvFunction;
    MyText txtMsg;
    View recordingView;
    PopupWindow popupWindow;
    ArrayList<String> recognitionList = new ArrayList<>(); // to store recognition zhuyin without tone
    ArrayList<String> wordList = new ArrayList<>(); // to show all possible chinese word according to selected no tone zhuyin
    ArrayList<String> displayLabelList = new ArrayList<>(); // to show all zhuyin with tone of the first chinese word behind cursor
    ArrayList<String> myLabelList = new ArrayList<>(); // store the choice for displayLabelList
    ArrayList<String> noToneLabelList = new ArrayList<>(); // store choice for recognitionList
    ArrayList<String> waveFiles = new ArrayList<>(); // store path of recording wave file if input is not by recording will store ""
    ArrayAdapter<String> ad2, ad3, ad4;

    //Global Data
    JSONObject myDict, czTable/*chineseToZhuyin*/;
    int width, height; // device resolution in pixels used for UI

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

        spRecognition = mView.findViewById(R.id.resultSpinner);
        spMyLabel = mView.findViewById(R.id.pronouceSpinner);
        lvWords = mView.findViewById(R.id.wordsList);
        lvFunction = mView.findViewById(R.id.fuctionList);
        txtMsg = mView.findViewById(R.id.txtMsg);

        // For EditText or TextView
        txtMsg.setText("");
        txtMsg.setOnCursorChangedListener(this);
        txtMsg.addTextChangedListener(textWatcher);

        // For ListView or Spinner
        recognitionList.add("-");
        recognitionList.add("ㄧ");
        recognitionList.add("ㄨㄛ");
        recognitionList.add("ㄋㄧ");
        ArrayList<String> fList = new ArrayList<>(Arrays.asList("錄音", "talk", "刪除"));

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
        try {
            InputStream dictStream = mContext.getAssets().open("pronounces_to_words.json");
            myDict = readJSONStream(dictStream);
            czTable = readJSONStream(mContext.getAssets().open("myDic.json"));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
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
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(eventReceiver);
    }

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

    // find pronounce of chinese word
    private ArrayList<String> lookCZTable(String word) throws JSONException {
        ArrayList<String> candidate = new ArrayList<>();
        JSONArray jsonArray = czTable.getJSONObject(word).getJSONArray("pronounces");
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
        txtMsg.setText("");
        txtClear = true;
    }

    //====================UI Listener Start====================
    // ###STEP 6###
    TextWatcher textWatcher = new TextWatcher() {
        int originLength;
        int pos;
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            originLength = txtMsg.getText().toString().length();
            pos = txtMsg.getSelectionStart();
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            int currentLength = txtMsg.getText().toString().length();
            int currentPos = txtMsg.getSelectionStart();
            if (!txtClear && currentPos == txtMsg.getSelectionEnd()) { // confirm not clear by functionList and not used select to delete
                if (currentLength > originLength) { // insert mode
                    if (!isVoiceInput) {
                        String addedText = txtMsg.getText().toString().substring(pos, currentPos);
                        for (int i = pos; pos < currentPos; i++) {
                            String ch = addedText.substring(i - pos, i - pos + 1);
                            waveFiles.add(i, "");
                            // avoid no mapping in czTable
                            noToneLabelList.add(i, "unknown");
                            myLabelList.add(i, "unknown");
                            try {
                                ArrayList<String> candidate = lookCZTable(ch);
                                if (candidate.size() > 0) {
                                    String myLabel = candidate.get(i);
                                    noToneLabelList.set(i, myLabel.replaceAll("[˙ˊˇˋ]$", ""));
                                    myLabelList.set(i, myLabel);
                                }
                            } catch (JSONException e) {
                                Log.w(TAG, "no Mapping In czTable");
                            }
                        }
                    }
                } else if (currentLength < originLength) { // delete mode
                    for (int i = currentPos - 1; i >= pos; i--) {
                        waveFiles.remove(i);
                        myLabelList.remove(i);
                        noToneLabelList.remove(i);
                    }
                }
            }
            txtClear = false;
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
                spMyLabel.setSelection(selectedIndex);
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
                     JSONArray jsonArray = myDict.getJSONArray(select);
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
                     myLabelList.set(msgPos, select);
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
                    case "錄音":  // ###STEP 1###
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                        final String path = "MyRecorder/tmp/" + df.format(new Date()) + ".wav";

                        Thread recorder = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                WAVRecorder wavRecorder = new WAVRecorder(path, mContext);
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
                            popupWindow.showAtLocation(mView, Gravity.CENTER, width/8*2, height/8*2);
                            recorder.start(); // ###STEP 1-1###
                        }

                        break;
                    case "talk":

                        break;
                    case "刪除":
                        clear();
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
                System.out.println(500+txtMsg.getSelectionStart());

                break;
        }
    }
    //=====================UI Listener End=====================

    //====================Speech Recognition Listener====================
    // ###STEP 2###
    @Override
    public void onFinishRecord(String path) {
        popupWindow.dismiss();
        isVoiceInput = true;
        waveFiles.add(txtMsg.getSelectionStart(), path);
        File file = new File(path);
        MediaScannerConnection.scanFile(mContext, new String[] {file.getAbsolutePath()}, null, null);
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
}
