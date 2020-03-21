package com.hhs.waverecorder.core;


import android.content.Context;
import android.media.MediaScannerConnection;

import com.hhs.waverecorder.Settings;
import com.hhs.waverecorder.utils.MyFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.hhs.waverecorder.Settings.hashed_password;
import static com.hhs.waverecorder.Settings.user_id;
import static com.hhs.waverecorder.utils.Utils.getJSONString;

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
        String apiName = "/updates";
        String url = Settings.URL + apiName;
        HttpURLConnection conn = null;

        try {
            String extraData = "}";
            if (extra != null) {
                extraData += ", \"extraData\":" + extra.toString() + "}";
            }
            String account = ", \"account\":{\"user_id\":"
                    + "\"" + user_id + "\", \"password\":"
                    + "\"" + hashed_password + "\", \"sign_up\":"
                    + "true}";
            String data = mUpdateData.toString();//.replaceFirst("}$", "") + extraData;
            String json = data.toString().replaceFirst("\\}\\s*$", "")
                    + account + "}";
            System.out.println(json);

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
            writer.write(json.getBytes("UTF-8"));
            os.flush();
            os.close();

            String myResult = getJSONString(conn.getInputStream());

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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
