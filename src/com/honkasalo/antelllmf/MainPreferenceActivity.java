package com.honkasalo.antelllmf;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class MainPreferenceActivity extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.main_preferences);
		ListPreference dataPref = (ListPreference) findPreference("foodMenuLanguage");
		dataPref.setSummary(dataPref.getEntry());
        dataPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setSummary(o.toString());
                return true;
			};
        });
		
	}
}
