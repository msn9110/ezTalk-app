package com.hhs.waverecorder.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class SHA256 {
    public static String getSHA256NTimes(String msg, int n) {
        if (n <= 0) {
            return msg;
        } else {
            msg = getSHA256(msg);
            return getSHA256NTimes(msg, n - 1);
        }
    }
    /**
     * 利用java原生的摘要實現SHA256加密
     * @param str 加密後的報文
     * @return
     */
    public static String getSHA256(String str){
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes("UTF-8"));
            encodeStr = byte2Hex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodeStr;
    }
    /**
     * 將byte轉為16進位制
     * @param bytes
     * @return
     */
    private static String byte2Hex(byte[] bytes){
        StringBuffer stringBuffer = new StringBuffer();
        String temp = null;
        for (int i=0;i<bytes.length;i++){
            temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length()==1){
//1得到一位的進行補0操作
                stringBuffer.append("0");
            }
            stringBuffer.append(temp);
        }
        return stringBuffer.toString();
    }
}
