package com.hhs.waverecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.hhs.wavrecorder.R;
import com.hhs.waverecorder.fragment.RecognitionFragment;
import com.hhs.waverecorder.fragment.VoiceCollectFragment;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.hhs.waverecorder.utils.Utils.readJSONStream;
import static com.hhs.waverecorder.AppValue.*;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final String TAG = "## " + getClass().getSimpleName();

    private Context mContext;

    private String czTable, zcTable;

    NavigationView navigationView;
    DrawerLayout drawer;

    Fragment currentFragment = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate : " + Thread.currentThread().getId());
        setContentView(R.layout.activity_main);
        mContext = this;
        requestPermission();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerClosed(View drawerView) {
                super .onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super .onDrawerOpened(drawerView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        long start = System.currentTimeMillis();
        readTable();
        double duration = (double) (System.currentTimeMillis() - start) / 1000;
        Toast.makeText(mContext, "Loading Time : " + String.valueOf(duration) + " sec",
                Toast.LENGTH_SHORT).show();

        if (currentFragment == null)
            replaceFragment(RecognitionFragment.newInstance(czTable, zcTable));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{android.Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "onRequestPermissionsResult");
        } else {
            this.finish();
        }
    }

    private void replaceFragment(Fragment mFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, mFragment);
        transaction.commit();
        currentFragment = mFragment;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        long start;
        Fragment mFragment = null;
        switch (id) {
            case R.id.nav_fragment_data_collect:
                mFragment = VoiceCollectFragment.newInstance(czTable);
                break;

            case R.id.nav_fragment_recognition:
                mFragment = RecognitionFragment.newInstance(czTable, zcTable);
                break;
        }

        start = System.currentTimeMillis();
        if (mFragment != null)  replaceFragment(mFragment);
        Log.d(TAG, String.valueOf(System.currentTimeMillis() - start));

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // read two tables
    private void readTable() {
        try {
            File pronounceToWord = new File(mContext.getFilesDir(), ZCTABLE);
            InputStream dictStream;
            if (pronounceToWord.exists())
                dictStream = mContext.openFileInput(ZCTABLE);
            else
                dictStream = mContext.getAssets().open(ZCTABLE);
            zcTable = readJSONStream(dictStream).toString();
            File myDic = new File(mContext.getFilesDir(), CZTABLE);
            if (myDic.exists())
                dictStream = mContext.openFileInput(CZTABLE);
            else
                dictStream = mContext.getAssets().open(CZTABLE);
            czTable = readJSONStream(dictStream).toString();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

}
