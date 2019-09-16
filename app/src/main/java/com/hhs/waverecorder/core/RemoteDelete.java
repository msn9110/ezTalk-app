package com.hhs.waverecorder.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteDelete {
    private File mFile;
    public RemoteDelete(String path) {
        mFile = new File(path);
    }
    
    public void executeRemoteDelete() {
        final String filename = mFile.getName();
        if (mFile.getAbsolutePath().contains("upload")){
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    String name = filename.replaceAll("uploaded-", "");
                    String label = mFile.getParentFile().getName();
                    JSONObject data = new JSONObject();
                    HttpURLConnection conn = null;
                    try {
                        data.put("filename", name);
                        data.put("label", label);
                        String url = "http://120.126.151.155:5000/remove";

                        StringEntity s = new StringEntity(data.toString(), "UTF-8");
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpPut httpPut = new HttpPut(url);
                        httpPut.setHeader("Accept", "application/json");
                        httpPut.setHeader("Content-type", "application/json");
                        httpPut.setEntity(s);
                        httpClient.execute(httpPut);
                        /*
                        URL u = new URL(url);
                        conn = (HttpURLConnection) u.openConnection();
                        conn.setRequestMethod("PUT");
                        conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setUseCaches(false);

                        OutputStream os = conn.getOutputStream();
                        DataOutputStream writer = new DataOutputStream(os);
                        writer.writeBytes(data.toString());
                        os.flush();
                        os.close();
                        */
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (conn != null)
                            conn.disconnect();
                    }
                }

            });
            worker.start();
        }
        
    }
}
