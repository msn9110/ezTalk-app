package com.hhs.waverecorder.utils;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.hhs.waverecorder.AppValue.CZTABLE;
import static com.hhs.waverecorder.AppValue.ZCTABLE;

public final class Utils {
    public static JSONArray sortJSONArrayByCount(JSONArray jsonArray, final boolean ascending) throws JSONException {
        List<JSONObject> array = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++)
            array.add(jsonArray.getJSONObject(i));
        Collections.sort(array, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String keyA = a.keys().next(), keyB = b.keys().next();
                try {
                    Integer valA = a.getInt(keyA);
                    Integer valB = b.getInt(keyB);
                    if (ascending)
                        return valA.compareTo(valB);
                    else
                        return -valA.compareTo(valB);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
        String jsonArrStr = "[";
        for (JSONObject json:array) {
            jsonArrStr += json.toString() + ",";
        }
        jsonArrStr = jsonArrStr.replaceAll(",$", "]");
        return new JSONArray(jsonArrStr);
    }

    // read JSON FileStream
    public static JSONObject readJSONStream(InputStream inputStream) throws IOException, JSONException {
        BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = myReader.readLine()) != null) {
            sb.append(line);
        }
        String jsonStr = sb.toString();
        return new JSONObject(jsonStr);
    }

    // find pronounce of chinese word
    public static ArrayList<String> lookTable(JSONObject table, String word, String key) throws JSONException {
        JSONObject item = table.getJSONObject(word);
        ArrayList<String> candidate = new ArrayList<>();
        JSONArray jsonArray = sortJSONArrayByCount(item.getJSONArray(key), false);
        for (int i = 0; i < jsonArray.length(); i++) {
            candidate.add(jsonArray.getJSONObject(i).keys().next());
        }
        return candidate;
    }

    // read two tables
    public static JSONObject readTables(Context context) {
        JSONObject tables = new JSONObject();
        try {
            File pronounceToWord = new File(Environment.getExternalStoragePublicDirectory("tables"), ZCTABLE);
            InputStream dictStream;
            if (pronounceToWord.exists())
                dictStream = new FileInputStream(pronounceToWord);
            else
                dictStream = context.getAssets().open(ZCTABLE);
            JSONObject zcTable = readJSONStream(dictStream);
            tables.put("zcTable", zcTable);
            File myDic = new File(Environment.getExternalStoragePublicDirectory("tables"), CZTABLE);
            if (myDic.exists())
                dictStream = new FileInputStream(myDic);
            else
                dictStream = context.getAssets().open(CZTABLE);
            JSONObject czTable = readJSONStream(dictStream);
            tables.put("czTable", czTable);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return tables;
    }

}
