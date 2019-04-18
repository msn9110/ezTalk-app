package com.hhs.waverecorder.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.core.Speaker;
import com.hhs.waverecorder.core.WAVRecorder;
import com.hhs.waverecorder.listener.OnCursorChangedListener;
import com.hhs.waverecorder.listener.VoiceInputListener;
import com.hhs.waverecorder.receiver.VoiceInputEventReceiver;
import com.hhs.waverecorder.widget.MyText;
import com.hhs.waverecorder.widget.VolumeCircle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.hhs.waverecorder.AppValue.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.AppValue.RECORD_FINISHED_ACTION;
import static com.hhs.waverecorder.AppValue.UPDATE_VOLUME_CIRCLE;
import static com.hhs.waverecorder.utils.Utils.readTables;

@SuppressWarnings("all")
public class RecoFragment extends Fragment implements VoiceInputListener,
        View.OnClickListener, View.OnLongClickListener,
        OnCursorChangedListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener{
    private final String TAG = "## " + getClass().getSimpleName();
    //Fragment Variable
    Context mContext;
    View mView;

    //Important Variable
    // The EventReceiver To trigger onFinishRecord and onFinishRecognition
    VoiceInputEventReceiver eventReceiver = new VoiceInputEventReceiver();
    // callback for another thread access UI
    @SuppressLint("HandlerLeak")
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
    MyText txtMsg;
    ImageButton btnRec;
    Button btnTalk, btnClear, btnBack, btnMoveCursor;
    Button[] btnf = new Button[8];
    FrameLayout volView;
    VolumeCircle circle = null;
    TextView tvRecNOW;
    ListView lvResults;
    ArrayList<String> recognitionList = new ArrayList<>();
    ArrayAdapter<String> ad2;

    int width, height, dpi; // device resolution in pixels used for UI

    //Global Variable
    WAVRecorder recorder = null;
    Speaker speaker;

    private void initUI() {
        // get resolution
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
        dpi = dm.densityDpi;

        lvResults = mView.findViewById(R.id.lvResults);
        txtMsg = mView.findViewById(R.id.txtMsg);
        btnRec = mView.findViewById(R.id.btnRec);
        btnTalk = mView.findViewById(R.id.btnTalk);
        btnClear = mView.findViewById(R.id.btnClear);
        btnBack = mView.findViewById(R.id.btnBack);
        btnMoveCursor = mView.findViewById(R.id.btnMoveCursor);
        int[] id = new int[] {R.id.btnf1, R.id.btnf2, R.id.btnf3, R.id.btnf4,
                R.id.btnf5, R.id.btnf6, R.id.btnf7, R.id.btnf8};
        for (int i = 0; i < 8; i++) {
            btnf[i] = mView.findViewById(id[i]);
            btnf[i].setOnClickListener(this);
            btnf[i].setText(String.valueOf(i + 1));
            btnf[i].setTextSize((float) 20.0);
        }

        // For Button
        btnRec.setOnClickListener(this);
        btnTalk.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnBack.setVisibility(View.GONE);
        btnMoveCursor.setOnClickListener(this);
        btnMoveCursor.setOnLongClickListener(this);

        // For ListView or Spinner
        recognitionList.clear();
        recognitionList.add("-");
        recognitionList.add("畫面測試");

        ad2 = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, recognitionList){
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

        lvResults.setAdapter(ad2);
        lvResults.setOnItemClickListener(this);
        lvResults.setOnItemLongClickListener(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate : " + Thread.currentThread().getId());
        mContext = getActivity();
        speaker = new Speaker(mContext);
        eventReceiver.setOnListener(this); // callback setting

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        mView = inflater.inflate(R.layout.fragment_recognition2, container, false);
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

    @Override
    public void onFinishRecord(String path) {

    }

    @Override
    public void onFinishRecognition(String result, String filepath) {

    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public boolean onLongClick(View view) {
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        return false;
    }

    @Override
    public void onCursorChanged(View view) {

    }
}
