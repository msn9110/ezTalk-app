package com.hhs.waverecorder.utils;


import java.util.regex.Pattern;

public class Check {


    // 根据UnicodeBlock方法判断中文标点符号
    static private boolean isChinesePunctuation(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
                /*|| ub == Character.UnicodeBlock.VERTICAL_FORMS*/);
    }

    // 使用Unicode编码范围来判断汉字；这个方法不准确,因为还有很多汉字不在这个范围之内
    static private boolean isChineseByRange(String str) {
        if (str == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("[\\u4E00-\\u9FCC]+");
        return pattern.matcher(str.trim()).find();
    }


    static public int checkChar(char ch){
        if(ch >= 32 && ch <= 64 || ch >= 91 && ch <= 96 || ch >= 123 && ch <= 126 || isChinesePunctuation(ch))
            return -1;
        if(isChineseByRange(String.valueOf(ch)))
            return 1;
        return 0;
    }
}
