package com.example.hhs.wavrecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.example.hhs.wavrecorder.MyReceiver.RECOGNITION_FINISHED_ACTION;

public class FTPManager extends AsyncTask<Void, Void, String> {

    private Context mContext;
    private File mFile;
    private FTPClient ftpClient;

    public FTPManager(Context context, File file) {
        this.mContext = context;
        this.mFile = file;
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
                System.out.println(ftpClient.printWorkingDirectory());
                inputStream = new FileInputStream(mFile);
                ftpClient.setBufferSize(1024);

                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.storeFile(mFile.getName(), inputStream);
                inputStream.close();

                String filename = mFile.getName();
                String json = "{\"data\":{\"label\":\"" + remoteDir +"\", \"filename\":\"" + filename + "\"}}";
                StringEntity s = new StringEntity(json, "UTF-8");
                System.out.println(json);
                //s.setContentEncoding("UTF-8");
                //s.setContentType("application/json");

                String url = "http://" + host + ":5000/recognize";
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setEntity(s);
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                String myResult = getJSONString(httpEntity);
                Intent intent = new Intent(RECOGNITION_FINISHED_ACTION);
                intent.putExtra("response", myResult);
                intent.putExtra("label", remoteDir);
                mContext.sendBroadcast(intent);
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

    private String getJSONString(HttpEntity httpEntity) throws IOException {
        InputStream is = httpEntity.getContent();

        BufferedReader bufReader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = bufReader.readLine()) != null) {
            builder.append(line + "\n");
        }
        is.close();
        return builder.toString();
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