package com.honkasalo.antelllmf;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class MainAboutActivity extends Activity {
	public TextView version;
	public String versionName;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_antell_lmf);
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		TextView version = (TextView) findViewById(R.id.aboutVersion);
		version.setText("Current version: "+versionName+"\n");
	}
}
