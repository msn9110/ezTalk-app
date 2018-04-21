package com.hhs.waverecorder.utils;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MyFile {


    public static void mkdirs(File dir) {
        //判斷文件夾是否存在,如果不存在則建立文件夾
        if (!dir.exists()) {
            if(!dir.mkdirs())
                System.out.println("MakeDir : Fail");
        }
    }

    public static boolean moveFile(File source, File target) {
        return copyFile(source, target) && source.delete();
    }

    public static boolean moveFile(String source, String target) {
        return moveFile(new File(source), new File(target));
    }

    public static boolean copyFile(File source, File target) {

        InputStream in;
        OutputStream out;
        try {

            //create output directory if it doesn't exist
            File dir = new File (target.getParent());
            mkdirs(dir);

            in = new FileInputStream(source.getPath());
            out = new FileOutputStream(target.getPath());

            return copyFile(in, out);

        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
        return false;
    }

    public static boolean copyFile(InputStream in, OutputStream out) {

        try {

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file
            out.flush();
            out.close();
            return true;

        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
        return false;
    }
    
    public static void writeStringToFile(String content, File file) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            BufferedWriter myWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            myWriter.write(content);
            myWriter.flush();
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //no construct
    private MyFile(){

    }
}
