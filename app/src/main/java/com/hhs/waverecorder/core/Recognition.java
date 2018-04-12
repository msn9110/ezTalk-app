package com.hhs.waverecorder.core;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static com.hhs.waverecorder.receiver.MyReceiver.RECOGNITION_FINISHED_ACTION;


public class Recognition extends Thread {

    private File mFile;
    private Context mContext;
    private Handler mHandler;

    public Recognition(Context context, String path, Handler uiHandler) {
        mContext = context;
        mFile = new File(path);
        mHandler = uiHandler;
    }

    @Override
    public void run() {
        super.run();
        String result = "辨識完成";
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/speech_recognition";
        String url = "http://" + host + port + apiName;
        try {
            byte[] raws = new byte[(int)mFile.length()];
            FileInputStream in = new FileInputStream(mFile);
            in.read(raws);
            int[] raw = new int[raws.length];
            for (int i = 0; i < raws.length; i++)
                raw[i] = 0xff & raws[i];
            JSONArray rawData = new JSONArray(Arrays.asList(raw));
            String json = "{\"data\":{\"filename\":\"" + mFile.getName() + "\", \"raw\":"
                            + rawData.getJSONArray(0).toString() + "}}";
            StringEntity s = new StringEntity(json, "UTF-8");
            //s.setContentEncoding("UTF-8");
            //s.setContentType("application/json");

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(s);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();

            String myResult = getJSONString(httpEntity);
            Intent intent = new Intent(RECOGNITION_FINISHED_ACTION);
            intent.putExtra("response", myResult);
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

        onPostExecute(result);
    }

    private void onPostExecute(final String s) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
            }
        });

    }

    private String getJSONString(HttpEntity httpEntity) throws IOException {
        InputStream is = httpEntity.getContent();

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = bufReader.readLine()) != null) {
            builder.append(line + "\n");
        }
        is.close();
        return builder.toString();
    }

}
