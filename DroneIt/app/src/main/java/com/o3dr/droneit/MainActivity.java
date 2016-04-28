package com.o3dr.droneit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_SETTINGS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i("MainActivity", "onCreate");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();

        ImageView imageView = (ImageView)findViewById(R.id.droneImageView);
        ((AnimationDrawable) imageView.getBackground()).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                ParseUser.getCurrentUser().logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        startActivity(new Intent(MainActivity.this, DispatchActivity.class));
                    }
                });
                break;

            case R.id.menu_settings:
                Intent intent = new Intent(this, UserSettingActivity.class);
                intent.putExtra(UserSettingActivity.from, UserSettingActivity.mainActivity);
                startActivityForResult(intent, RESULT_SETTINGS);
                break;
        }

        return true;
    }

    public void onPlayClicked(View view) {
        if(isOnline()) {
            Intent intent = new Intent(this, LevelsActivity.class);
            startActivity(intent);
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        boolean isOnline = netInfo != null && netInfo.isConnectedOrConnecting();

        if(!isOnline) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Turn on internet connection")
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.dialog_try_again), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            isOnline();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        return isOnline;
    }
}
