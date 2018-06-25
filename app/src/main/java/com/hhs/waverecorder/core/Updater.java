package com.hhs.waverecorder.core;


import android.content.Context;
import android.media.MediaScannerConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

@SuppressWarnings("all")
public class Updater extends Thread {
    private Context mContext;
    private JSONObject mUpdateData;
    //private final ArrayList<File> mFiles;

    public Updater(Context context, JSONObject data) {
        mContext = context;
        mUpdateData = data;
        //mFiles = files;
    }

    @Override
    public void run() {
        super.run();
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/updates";
        String url = "http://" + host + port + apiName;

        try {
            StringEntity entity = new StringEntity(mUpdateData.toString(), "UTF-8");
            HttpClient httpClient = new DefaultHttpClient();
            HttpPut httpPut = new HttpPut(url);
            httpPut.setHeader("Accept", "application/json");
            httpPut.setHeader("Content-type", "application/json");
            httpPut.setEntity(entity);
            HttpResponse httpResponse = httpClient.execute(httpPut);
            HttpEntity httpEntity = httpResponse.getEntity();

            String myResult = Recognition.getJSONString(httpEntity);
            JSONObject response = new JSONObject(myResult);
            boolean success = response.getBoolean("success");
            JSONObject movedFilesState = response.getJSONObject("movedFilesState");
            if (success) {
                /*
                for (File f:mFiles) {
                    String originPath = f.getAbsolutePath();
                    String newName = "uploaded-" + f.getName();
                    File dest = new File(f.getParentFile(), newName);
                    if (movedFilesState.getBoolean(f.getName()) && f.renameTo(dest)) {
                        MediaScannerConnection.scanFile(mContext, new String[] {dest.getAbsolutePath(), originPath},
                                                        null, null);
                    }
                }
                */
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
