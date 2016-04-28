package com.o3dr.droneit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.parse.ParseUser;

public class DispatchActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Log.i("DispatchActivity", "onCreate");
        dispatch();
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
                            dispatch();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        return isOnline;
    }

    private void dispatch() {
        if(isOnline()) {
            // Check if there is current user info
            if (ParseUser.getCurrentUser() != null) {
                // Start an intent for the logged in activity
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // Start and intent for the logged out activity
                startActivity(new Intent(this, SignUpOrLoginActivity.class));
            }
        }
    }
}
