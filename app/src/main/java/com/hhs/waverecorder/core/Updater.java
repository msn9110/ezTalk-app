package com.hhs.waverecorder.core;


import android.content.Context;
import android.media.MediaScannerConnection;

import com.hhs.waverecorder.utils.MyFile;

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
    private JSONObject extra;
    private final ArrayList<String> mFiles;

    public Updater(Context context, JSONObject data, ArrayList<String> files, JSONObject extra) {
        mContext = context;
        mUpdateData = data;
        this.extra = extra;
        mFiles = files;
    }

    @Override
    public void run() {
        super.run();
        String host = "120.126.145.113";
        String port = ":5000";
        String apiName = "/updates";
        String url = "http://" + host + port + apiName;

        try {
            String extraData = "}";
            if (extra != null) {
                extraData += ", \"extraData\":" + extra.toString() + "}";
            }
            String data = mUpdateData.toString();//.replaceFirst("}$", "") + extraData;
            StringEntity entity = new StringEntity(data, "UTF-8");
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
                if (mFiles.size() > 0) {
                    for (int i = 0; i < mFiles.size(); i+=2) {
                        String originalPath = mFiles.get(i);
                        String newPath = mFiles.get(i + 1);
                        String name = new File(originalPath).getName();
                        if (movedFilesState.getBoolean(name)) {
                            if (MyFile.moveFile(originalPath, newPath)) {
                                MediaScannerConnection.scanFile(mContext, new String[]{originalPath, newPath},
                                        null, null);
                            }
                        }
                    }
                }
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
