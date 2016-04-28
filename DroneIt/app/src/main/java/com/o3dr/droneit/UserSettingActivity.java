package com.o3dr.droneit;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.KeyEvent;


public class UserSettingActivity extends PreferenceActivity {

	public static final String from = "From";
	public static final String levelsActivity = "LevelsActivity";
	public static final String mainActivity = "MainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		addPreferencesFromResource(R.xml.settings);

		Log.i("UserSettingActivity", "onCreate");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

			Intent intent;
			if(getIntent().getStringExtra(from).equals(levelsActivity))
				intent = new Intent(this, LevelsActivity.class);
			else
				intent = new Intent(this, MainActivity.class);

			startActivity(intent);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
