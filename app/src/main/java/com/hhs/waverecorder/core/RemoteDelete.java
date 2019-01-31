package com.hhs.waverecorder.core;

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

public class RemoteDelete {
    private File mFile;
    public RemoteDelete(String path) {
        mFile = new File(path);
    }
    
    public void executeRemoteDelete() {
        final String filename = mFile.getName();
        if (mFile.getAbsolutePath().replaceAll("upload", "").length() <
                mFile.getAbsolutePath().length()){
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    String name = filename.replaceAll("uploaded-", "");
                    String label = mFile.getParentFile().getName();
                    JSONObject data = new JSONObject();
                    try {
                        data.put("filename", name);
                        data.put("label", label);
                        String url = "http://120.126.145.113:5000/remove";

                        StringEntity entity = new StringEntity(data.toString(), "UTF-8");
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpPut httpPut = new HttpPut(url);
                        httpPut.setHeader("Accept", "application/json");
                        httpPut.setHeader("Content-type", "application/json");
                        httpPut.setEntity(entity);
                        HttpResponse httpResponse = httpClient.execute(httpPut);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });
            worker.start();
        }
        
    }
}
