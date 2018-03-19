package com.example.hhs.wavrecorder;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static com.example.hhs.wavrecorder.MyReceiver.RECOGNITION_FINISHED_ACTION;


public class Recogntion extends AsyncTask<Void, Void, String> {

    private String mFilename;
    private String mLabel;
    private Context mContext;

    public Recogntion(Context context, String filename, String label) {
        mContext = context;
        mFilename = filename;
        mLabel = label;
    }
    @Override
    protected String doInBackground(Void... voids) {
        String result = "";
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/recognize";
        String url = "http://" + host + port + apiName;

        String json = "{\"data\":{\"label\":\"" + mLabel +"\", \"filename\":\"" + mFilename + "\"}}";
        try {
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
            intent.putExtra("label", mLabel);
            mContext.sendBroadcast(intent);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            result = "unsupportedEncoding";
        } catch (IOException e) {
            e.printStackTrace();
            result = "POST Error";
        }

        return result;
    }
    @Override
    protected void onPostExecute(String s) {
    if (s.length() > 0)   Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
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
