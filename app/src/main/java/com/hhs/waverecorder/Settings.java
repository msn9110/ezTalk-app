package com.hhs.waverecorder;

import android.content.Context;
import android.content.SharedPreferences;

import com.hhs.waverecorder.utils.SHA256;

import static android.content.Context.MODE_PRIVATE;

public final class Settings {

    public static String host;
    public static int port;
    public static String URL;
    public static String user_id;
    public static String password;
    public static String hashed_password;

    public static void setAPPSettings(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("preferences", MODE_PRIVATE);
        host = preferences.getString("pref_host", "example.com");
        port = preferences.getInt("pref_port", 56312);
        URL = "http://" + host + ":" + String.valueOf(port);
        user_id = preferences.getString("pref_user", "user");
        password = preferences.getString("pref_password", "password");
        hashed_password = SHA256.getSHA256NTimes(password, 2);
    }

    private Settings() {}
}
