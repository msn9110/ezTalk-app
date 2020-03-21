package com.hhs.waverecorder;

import com.hhs.waverecorder.utils.SHA256;

public final class Settings {

    public static String HOST = "120.126.151.155";
    public static String PORT = ":56312";
    public static String URL = "http://" + HOST + PORT;
    public static String user_id = "msn9110";
    public static String hashed_password = SHA256.getSHA256NTimes(user_id, 2);

    private Settings() {}
}
