package com.example.hhs.wavrecorder;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

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

import static com.example.hhs.wavrecorder.MyReceiver.RECOGNITION_FINISHED_ACTION;
import static com.example.hhs.wavrecorder.MyReceiver.RECORD_FINISHED_ACTION;


public class RecognitionFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        AdapterView.OnItemClickListener, MyListener {

    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    MyReceiver eventReceiver = new MyReceiver();

    //UI Variable
    Spinner resultSpinner;
    ListView lvWords, lvFunction;
    EditText txtMsg;
    ArrayList<String> pronounceList = new ArrayList<>();
    ArrayList<String> wordList = new ArrayList<>();

    //Global Data
    JSONObject myDict;

    //State Variable
    private boolean isRecord = false;

    private void initUI() {

        resultSpinner = mView.findViewById(R.id.resultSpinner);
        lvWords = mView.findViewById(R.id.wordsList);
        lvFunction = mView.findViewById(R.id.fuctionList);
        txtMsg = mView.findViewById(R.id.txtMsg);

        txtMsg.setText("我");
        pronounceList.addAll(Arrays.asList("ㄨㄛ", "ㄋㄧ", "ㄊㄚ", "ㄨ", "ㄧ"));
        ArrayList<String> fList = new ArrayList<>(Arrays.asList("錄音", "talk", "刪除"));

        ArrayAdapter<String> ad1 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, fList);
        lvFunction.setAdapter(ad1);

        ArrayAdapter<String> ad2 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, wordList);
        lvWords.setAdapter(ad2);

        ArrayAdapter<String> ad3 = new ArrayAdapter<>(mContext, R.layout.myspinner, pronounceList);
        resultSpinner.setAdapter(ad3);
        ad3.setDropDownViewResource(R.layout.myspinner);
        resultSpinner.setOnItemSelectedListener(this);
        //resultSpinner.setSelection(0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        eventReceiver.setOnListener(this);
        try {
            InputStream dictStream = mContext.getAssets().open("pronounces_to_words.json");
            myDict = readDictionary(dictStream);
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

    private JSONObject readDictionary(InputStream inputStream) throws IOException, JSONException {
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
     public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
         String select = ((TextView) view).getText().toString();
         switch (adapterView.getId()) {
             case R.id.resultSpinner:
                 wordList.clear();
                 try {
                     JSONArray jsonArray = myDict.getJSONArray(select);
                     for (int i = 0; i < jsonArray.length(); i++) {
                         wordList.add(jsonArray.getJSONObject(i).keys().next());
                     }
                     ArrayAdapter<String> ad2 = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, wordList);
                     lvWords.setAdapter(ad2);
                 } catch (JSONException e) {
                     e.printStackTrace();
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

                        break;
                }
                break;
        }
    }

    @Override
    public void onFinishRecord(String path) {

    }

    @Override
    public void onFinishRecognition(String result, String label) {

    }
}
