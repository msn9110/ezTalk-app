package com.hhs.waverecorder.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPManager extends AsyncTask<Void, Void, String> {

    private Context mContext;
    private File mFile;
    private FTPClient ftpClient;
    private Handler mHandler;

    public FTPManager(Context context, File file) {
        this.mContext = context;
        this.mFile = file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mHandler = new Handler();
    }

    @Override
    protected String doInBackground(Void... voids) {
        String result = "上傳成功!!!";
        ftpClient = new FTPClient();
        FileInputStream inputStream = null;

        String host = "120.126.145.113";
        String username = "dmcl";
        String password = "unigrid";
        String remoteDir = mFile.getParentFile().getName();
        try {
            ftpClient.setConnectTimeout(60 * 1000);
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.connect(host);
            ftpClient.login(username, password);
            if (checkDirectoryExists(remoteDir) ||
                    ftpClient.makeDirectory(remoteDir)) {
                ftpClient.changeWorkingDirectory(remoteDir);
                inputStream = new FileInputStream(mFile);
                ftpClient.setBufferSize(1024);

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.storeFile(mFile.getName(), inputStream);
                inputStream.close();

            }
        } catch (Exception ex) {
            result = "連線異常";
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(inputStream);
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                result = "關閉連線異常";
            }
        }
        return result;
    }


    @Override
    protected void onPostExecute(String s) {
        Toast.makeText(mContext, s, Toast.LENGTH_LONG).show();
    }

    private boolean checkDirectoryExists(String dirPath) throws IOException {
        ftpClient.changeWorkingDirectory(dirPath);
        int returnCode = ftpClient.getReplyCode();
        if (returnCode == 550) {
            return false;
        }
        ftpClient.changeToParentDirectory();
        return true;
    }

    private boolean checkFileExists(String filePath) throws IOException {
        InputStream inputStream = ftpClient.retrieveFileStream(filePath);
        int returnCode = ftpClient.getReplyCode();
        if (inputStream == null || returnCode == 550) {
            return false;
        }
        return true;
    }
}