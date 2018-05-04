package com.hhs.waverecorder.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.listener.CursorChangedListener;
import com.hhs.waverecorder.listener.MyListener;
import com.hhs.waverecorder.receiver.MyReceiver;
import com.hhs.waverecorder.widget.MyText;
import com.hhs.waverecorder.widget.VolumeCircle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.hhs.waverecorder.receiver.MyReceiver.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.receiver.MyReceiver.RECORD_FINISHED_ACTION;
import static com.hhs.waverecorder.utils.Utils.readJSONStream;

@SuppressWarnings("all")
public class VoiceCollectFragment extends Fragment implements
        View.OnClickListener, AdapterView.OnItemSelectedListener,
        CursorChangedListener, MyListener {

    private final String TAG = "## " + getClass().getName();
    private final static String CZTABLE = "czTable.json";

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
    ImageButton btnRec;
    Button btnDel;
    FrameLayout volView;
    Spinner spMyLabel, spTone;
    MyText txtWord;
    TextView txtRecording;

    //Global Data
    JSONObject czTable/*chineseToZhuyin*/;
    int width, height, dpi; // device resolution in pixels used for UI

    //State Variable
    private boolean isRecord = false;

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

        txtWord.setOnCursorChangedListener(this);
        spMyLabel.setOnItemSelectedListener(this);
        spTone.setOnItemSelectedListener(this);
        btnRec.setOnClickListener(this);
        btnDel.setOnClickListener(this);
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
        mView = inflater.inflate(R.layout.fragment_voice_collect, container, false);
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
        try {
            czTable = readJSONStream(mContext.openFileInput(CZTABLE));
            double duration = (double) (System.currentTimeMillis() - start) / 1000;
            Toast.makeText(mContext, "Loading Time : " + String.valueOf(duration) + " sec",
                    Toast.LENGTH_SHORT).show();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(eventReceiver);
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onCursorChanged(View view) {

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onFinishRecord(String path) {

    }

    @Override
    public void onFinishRecognition(String result, String correctLabel) {

    }
}
