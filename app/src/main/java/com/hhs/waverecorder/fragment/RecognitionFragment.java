package com.hhs.waverecorder.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.CursorChangedListener;
import com.hhs.waverecorder.listener.MyListener;
import com.hhs.waverecorder.receiver.MyReceiver;
import com.hhs.waverecorder.widget.MyText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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

    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    MyReceiver eventReceiver = new MyReceiver();
    Handler mUIHandler = new Handler();

    //UI Variable
    Spinner resultSpinner, pronounceSpinner;
    ListView lvWords, lvFunction;
    MyText txtMsg;
    ArrayList<String> pronounceList = new ArrayList<>();
    ArrayList<String> wordList = new ArrayList<>();
    ArrayList<String> labelList = new ArrayList<>();
    ArrayList<String> noToneLabelList = new ArrayList<>();
    ArrayList<String> waveFiles = new ArrayList<>();
    ArrayAdapter<String> ad2, ad3, ad4;

    //Global Data
    JSONObject myDict, czTable/*chineseToZhuyin*/;

    //State Variable
    private boolean isRecord = false;

    private void initUI() {

        resultSpinner = mView.findViewById(R.id.resultSpinner);
        pronounceSpinner = mView.findViewById(R.id.pronouceSpinner);
        lvWords = mView.findViewById(R.id.wordsList);
        lvFunction = mView.findViewById(R.id.fuctionList);
        txtMsg = mView.findViewById(R.id.txtMsg);

        // For EditText or TextView
        txtMsg.setText("");
        txtMsg.setOnCursorChangedListener(this);

        // For ListView or Spinner
        pronounceList.add("-");
        pronounceList.add("ㄨㄛ");
        pronounceList.add("ㄋㄧ");
        ArrayList<String> fList = new ArrayList<>(Arrays.asList("錄音", "talk", "刪除"));

        ArrayAdapter<String> ad1 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, fList);
        lvFunction.setAdapter(ad1);
        lvFunction.setOnItemClickListener(this);

        ad2 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, wordList);
        lvWords.setAdapter(ad2);
        lvWords.setOnItemClickListener(this);

        ad3 = new ArrayAdapter<>(mContext, R.layout.myspinner, pronounceList);
        resultSpinner.setAdapter(ad3);
        ad3.setDropDownViewResource(R.layout.myspinner);
        resultSpinner.setOnItemSelectedListener(this);
        resultSpinner.setSelection(0);

        labelList.add("-");
        ad4 = new ArrayAdapter<>(mContext, R.layout.myspinner, labelList);
        pronounceSpinner.setAdapter(ad4);
        ad4.setDropDownViewResource(R.layout.myspinner);
        pronounceSpinner.setOnItemSelectedListener(this);
        pronounceSpinner.setSelection(0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        eventReceiver.setOnListener(this);
        try {
            InputStream dictStream = mContext.getAssets().open("pronounces_to_words.json");
            myDict = readJSONStream(dictStream);
            czTable = readJSONStream(mContext.getAssets().open("一字多音表.json"));
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

    @Override
    public void onCursorChanged(int position) {
        String character = txtMsg.getText().toString().substring(position, position + 1);
        labelList.clear();
        labelList.add("-");
        int selectedIndex = 0;
        try {
            JSONArray jsonArray = czTable.getJSONArray(character);
            String noToneLabel = noToneLabelList.get(position);
            for (int i = 0; i < jsonArray.length(); i++) {
                String label = jsonArray.getString(i);
                labelList.add(label);
                if (label.startsWith(noToneLabel) && selectedIndex == 0) {
                    selectedIndex = i + 1;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ad4.notifyDataSetChanged();
        pronounceSpinner.setSelection(selectedIndex);
        System.out.println(character);
    }

    @Override
     public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
         String select = ((TextView) view).getText().toString();
        int msgPos = txtMsg.getSelectionStart();
         switch (adapterView.getId()) {
             case R.id.resultSpinner:
                 wordList.clear();
                 if (position == 0){
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
                         lvWords.setSelection(0);
                     }
                 } catch (JSONException e) {
                     e.printStackTrace();
                 }
                 break;
             case R.id.pronouceSpinner:
                 if (position == 0)
                     break;

                 if (msgPos >= labelList.size() - 1) {
                     labelList.add(select);
                 } else {
                     labelList.set(msgPos, select);
                 }
                 break;
         }
     }

     @Override
     public void onNothingSelected(AdapterView<?> adapterView) {

     }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        String select = ((TextView) view).getText().toString();
        switch (adapterView.getId()) {
            case R.id.fuctionList:
                switch (select) {
                    case "錄音":
                        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.getDefault());
                        final String path = "MyRecorder/tmp/" + df.format(new Date()) + ".wav";

                        Thread recorder = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                WAVRecorder wavRecorder = new WAVRecorder(path, mContext);
                                System.out.println("Start Recording");
                                //TODO: a UI to hint recording now

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
                        if (!isRecord) {
                            recorder.start();
                        }

                        break;
                    case "talk":

                        break;
                    case "刪除":
                        labelList.clear();;
                        waveFiles.clear();
                        wordList.clear();
                        pronounceList.clear();
                        noToneLabelList.clear();
                        pronounceList.add("-");
                        labelList.add("-");
                        ad2.notifyDataSetChanged();
                        ad3.notifyDataSetChanged();
                        ad4.notifyDataSetChanged();
                        pronounceSpinner.setSelection(0);
                        resultSpinner.setSelection(0);
                        txtMsg.setText("");
                        break;
                }
                break;
            case R.id.wordsList:
                int msgPos = txtMsg.getSelectionStart();
                String noToneLabel = ((TextView) resultSpinner.getSelectedView()).getText().toString();
                if (msgPos >= labelList.size() - 1) {
                    noToneLabelList.add(noToneLabel);
                } else {
                    noToneLabelList.set(msgPos, noToneLabel);
                }
                String msg = txtMsg.getText().toString();
                String part1 = msg.substring(0, msgPos);
                String part2 = msg.substring(msgPos, msg.length());
                String text = part1 + select + part2;
                txtMsg.setText(text);
                txtMsg.setSelection(part1.length() + select.length());
                break;
        }
    }

    @Override
    public void onFinishRecord(String path) {
        waveFiles.add(txtMsg.getSelectionStart(), path);
    }

    @Override
    public void onFinishRecognition(String result, String correctLabel/*not used here*/) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject response = jsonObject.getJSONObject("response");
            boolean success = response.getBoolean("success");
            pronounceList.clear();
            pronounceList.add("-");

            if (success) {
                JSONArray results = response.getJSONArray("result");
                for (int i = 0; i < results.length(); i++) {
                    String pronounce = results.getString(i);
                    if (!pronounce.startsWith("__")) {
                        pronounceList.add(pronounce);
                    }
                }
            }
            ArrayAdapter<String> ad3 = new ArrayAdapter<>(mContext, R.layout.myspinner, pronounceList);
            resultSpinner.setAdapter(ad3);
            ad3.setDropDownViewResource(R.layout.myspinner);
            if (pronounceList.size() > 1)
                resultSpinner.setSelection(1);
            else
                resultSpinner.setSelection(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}