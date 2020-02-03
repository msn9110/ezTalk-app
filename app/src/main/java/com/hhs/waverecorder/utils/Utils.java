package com.hhs.waverecorder.utils;

import android.content.Context;
import android.media.MediaScannerConnection;
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

    // look zcTable or czTable
    public static ArrayList<String> lookTable(JSONObject table, String value, String key) throws JSONException {
        ArrayList<String> candidate = new ArrayList<>();
        JSONArray jsonArray;
        if (key.length() > 0) {
            JSONObject item = table.getJSONObject(value);
            jsonArray = sortJSONArrayByCount(item.getJSONArray(key), false);
        } else
            jsonArray = sortJSONArrayByCount(table.getJSONArray(value), false);
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


    // update the count of an object in array of an object
    public static JSONObject updateOAO(String key1, String key2, JSONObject item, String strIndex) throws JSONException {
        int count;
        JSONArray itemJSONArray = item.getJSONArray(strIndex);
        JSONObject changedItem = new JSONObject("{\"" + key2 + "\" : 1}"); // default value of array
        int index = itemJSONArray.length(); // default index of array
        for (int j = 0; j < index; j++) {
            JSONObject find = itemJSONArray.getJSONObject(j);
            if (key2.contentEquals(find.keys().next())) {
                count = find.getInt(key2) + 1;
                changedItem = find.put(key2, count);
                index = j;
                break;
            }
        }
        itemJSONArray.put(index, changedItem);
        return item.put(key1, itemJSONArray);
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

    // store ZCTABLE CZTABLE
    public static void storeTable(JSONObject table, Context context) {
        JSONObject zcTable = null, czTable = null;

        try {
            zcTable = table.getJSONObject("zcTable");
            czTable = table.getJSONObject("czTable");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (zcTable != null) {
            String outStr = zcTable.toString().replaceAll("(\\],)", "$0\n\n");
            File f = new File(Environment.getExternalStoragePublicDirectory("tables"), ZCTABLE);
            MyFile.writeStringToFile(outStr, f);
            MediaScannerConnection.scanFile(context, new String[]{f.getAbsolutePath()}, null, null);
        }

        if (czTable != null) {
            String outStr = czTable.toString().replaceAll("(\\]\\},)", "$0\n\n");
            File f = new File(Environment.getExternalStoragePublicDirectory("tables"), CZTABLE);
            MyFile.writeStringToFile(outStr, f);
            MediaScannerConnection.scanFile(context, new String[]{f.getAbsolutePath()}, null, null);
        }
    }

    // to get pronounce tone
    public static int getTone(String pronounce) {
        String toneChar = pronounce.substring(pronounce.length() - 1, pronounce.length());
        int tone;
        switch (toneChar) {
            case "˙":
                tone = 0;
                break;
            case "ˊ":
                tone = 2;
                break;
            case "ˇ":
                tone = 3;
                break;
            case "ˋ":
                tone = 4;
                break;
            default:
                tone = 1;
                break;
        }
        return tone;
    }

}
