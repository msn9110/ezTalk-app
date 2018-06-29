package com.hhs.waverecorder.utils;


import java.util.ArrayList;
import java.util.Arrays;

public class Divider {

    static public ArrayList<String> getSentences(String message){
        ArrayList<String> result = new ArrayList<>();
        //message = message.replaceAll("(\\W\\s\\W)", "$1,$3");
        String regex = "(?<=[,.，。])";

        String[] temp = message.split(regex);
        result.addAll(Arrays.asList(temp));
        return result;
    }
}
