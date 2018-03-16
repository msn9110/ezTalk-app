package com.example.hhs.wavrecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;


public class FTPManager extends AsyncTask<Void, Void, String> {

    private Context mContext;
    private File mFile;
    private String mContent;
    private FTPClient ftpClient;

    public FTPManager(Context context, File file, String content) {
        this.mContext = context;
        this.mFile = file;
        this.mContent = content;
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
                System.out.println(8888);
            }

            String label = remoteDir;
            String filename = mFile.getName();
            String json = "{\"data\":{\"label\":\"" + label +"\", \"filename\":\"" + filename + "\"}}";
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

            /*
            String url = "http://1.34.132.90/recordings/insert.php";
            String file_id = mFile.getName().replace(".mp4","");
            ArrayList<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("file_id", file_id));
            params.add(new BasicNameValuePair("content", mContent));

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            InputStream is = httpEntity.getContent();

            BufferedReader bufReader = new BufferedReader(new InputStreamReader(is, "utf-8"), 8);
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = bufReader.readLine()) != null) {
                builder.append(line + "\n");
            }
            inputStream.close();
            JSONObject jsonData = new JSONObject(builder.toString());
            int success = Integer.parseInt(jsonData.getString("success"));
            if (success == 0)
                result = "insert failed";
            */
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