package com.hhs.waverecorder.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.example.hhs.wavrecorder.R;

import static android.content.Context.MODE_PRIVATE;
import static com.hhs.waverecorder.Settings.host;
import static com.hhs.waverecorder.Settings.password;
import static com.hhs.waverecorder.Settings.port;
import static com.hhs.waverecorder.Settings.setAPPSettings;
import static com.hhs.waverecorder.Settings.user_id;

public class MyPrefFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener {

    SharedPreferences preferences;
    EditTextPreference pref_host, pref_port, pref_user, pref_password;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.preferences);

        preferences = getContext().getSharedPreferences("preferences", MODE_PRIVATE);

        pref_host = (EditTextPreference) findPreference("pref_host");
        pref_port = (EditTextPreference) findPreference("pref_port");
        pref_user = (EditTextPreference) findPreference("pref_user");
        pref_password = (EditTextPreference) findPreference("pref_password");


        pref_host.setOnPreferenceChangeListener(this);
        pref_port.setOnPreferenceChangeListener(this);
        pref_user.setOnPreferenceChangeListener(this);
        pref_password.setOnPreferenceChangeListener(this);

        showPreferences();
    }

    private void showPreferences() {
        pref_host.setText(host);
        pref_host.setSummary(pref_host.getText());

        pref_port.setText(String.valueOf(port));
        pref_port.setSummary(pref_port.getText());

        pref_user.setText(user_id);
        pref_user.setSummary(pref_user.getText());

        pref_password.setText("");

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        System.out.println(key);
        switch (key) {
            case "pref_host":
            case "pref_user":
            case "pref_password":
                preferences.edit().putString(key, value.toString()).apply();
                break;

            case "pref_port":
                preferences.edit().putInt(key, Integer.parseInt(value.toString())).apply();
                break;
        }
        setAPPSettings(getActivity());
        showPreferences();
        return false;
    }
}
