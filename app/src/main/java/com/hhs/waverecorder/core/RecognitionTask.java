package com.hhs.waverecorder.core;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static com.hhs.waverecorder.AppValue.RECOGNITION_FINISHED_ACTION;
import static com.hhs.waverecorder.core.Recognition.getJSONString;

public class RecognitionTask extends AsyncTask<File, Void, String> {

    private Context mContext;
    private ProgressDialog recognitionDialog;
    private HttpPost httpPost = null;
    private long start = 0;

    public RecognitionTask(Context context) {
        this.mContext = context;
        recognitionDialog = new ProgressDialog(mContext);
        recognitionDialog.setTitle("辨識中");
        recognitionDialog.setMessage("請稍候...");
        recognitionDialog.setCanceledOnTouchOutside(false);
        recognitionDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        recognitionDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (httpPost != null) {
                    httpPost.abort();
                }
            }
        });
        recognitionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        recognitionDialog.dismiss();
                    }
                });
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();
        recognitionDialog.show();
    }

    @Override
    protected String doInBackground(File... files) {
        String result = "辨識完成";
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/recognize";
        String url = "http://" + host + port + apiName;
        if (files.length == 1) {
            for (File mFile: files) {
                String label = mFile.getParentFile().getName();
                try {
                    byte[] raws = new byte[(int)mFile.length()];
                    FileInputStream in = new FileInputStream(mFile);
                    in.read(raws);
                    int[] raw = new int[raws.length];
                    for (int i = 0; i < raws.length; i++)
                        raw[i] = 0xff & raws[i];
                    JSONArray rawData = new JSONArray(Arrays.asList(raw));
                    String extraData = "";
                    String json = "{\"data\":{\"label\":\"" + label + "\","
                            + "\"num_of_stn\": 8,"
                            + "\"filename\":\"" + mFile.getName() + "\", \"raw\":"
                            + rawData.getJSONArray(0).toString() + "}"
                            + extraData + "}";
                    StringEntity s = new StringEntity(json, "UTF-8");
                    HttpClient httpClient = new DefaultHttpClient();
                    httpPost = new HttpPost(url);
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");
                    httpPost.setEntity(s);
                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity httpEntity = httpResponse.getEntity();

                    String myResult = getJSONString(httpEntity.getContent());

                    httpPost = null;
                    Intent intent = new Intent(RECOGNITION_FINISHED_ACTION);
                    intent.putExtra("response", myResult);
                    intent.putExtra("filepath", mFile.getAbsolutePath());
                    mContext.sendBroadcast(intent);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    result = "unsupportedEncoding";
                } catch (IOException e) {
                    e.printStackTrace();
                    result = "POST Error";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        recognitionDialog.dismiss();
        if (s != null) {
            String time = ":" + String.valueOf((System.currentTimeMillis() - start) / 1000)
                    + "sec";
            Toast.makeText(mContext, s + time, Toast.LENGTH_SHORT).show();
        }
    }
}
