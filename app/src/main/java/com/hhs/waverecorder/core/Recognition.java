package com.hhs.waverecorder.core;

import android.content.Context;
import android.content.Intent;
import android.net.http.HttpsConnection;
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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static com.hhs.waverecorder.AppValue.*;

@SuppressWarnings("all")
public class Recognition extends Thread {

    private File mFile;
    private Context mContext;
    private Handler mHandler;
    private JSONObject extra;
    private HttpPost httpPost = null;

    public Recognition(Context context, String path, Handler uiHandler, JSONObject extra) {
        mContext = context;
        mFile = new File(path);
        mHandler = uiHandler;
        this.extra = extra;
    }

    public void stopRecognition() {
        if (httpPost != null)
            httpPost = null;
    }

    @Override
    public void run() {
        super.run();
        String label = mFile.getParentFile().getName();
        String result = "辨識完成";
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/recognize";
        String url = "http://" + host + port + apiName;
        HttpURLConnection conn = null;
        try {
            byte[] raws = new byte[(int)mFile.length()];
            FileInputStream in = new FileInputStream(mFile);
            in.read(raws);
            int[] raw = new int[raws.length];
            for (int i = 0; i < raws.length; i++)
                raw[i] = 0xff & raws[i];
            JSONArray rawData = new JSONArray(Arrays.asList(raw));
            String extraData = "";
            if (extra != null) {
                extraData += ", \"extraData\":" + extra.toString();
            }
            String json = "{\"data\":{\"label\":\"" + label + "\","
                            + "\"filename\":\"" + mFile.getName() + "\", \"raw\":"
                            + rawData.getJSONArray(0).toString() + "}"
                            + extraData + "}";
            StringEntity s = new StringEntity(json, "UTF-8");
            //s.setContentEncoding("UTF-8");
            //s.setContentType("application/json");
/*
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);

            OutputStream os = conn.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);
            writer.writeBytes(json);
            os.flush();
            os.close();

            String myResult = getJSONString(conn.getInputStream());

            */

            HttpClient httpClient = new DefaultHttpClient();
            httpPost = new HttpPost(url);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(s);
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();

            String myResult = getJSONString(httpEntity.getContent());

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
        } finally {
            if (conn != null)
                conn.disconnect();
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

    public static String getJSONString(InputStream is) throws IOException {

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
