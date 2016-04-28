package com.o3dr.droneit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

public class SignUpOrLoginActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_or_login);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Log in button click handler
        ((Button) findViewById(R.id.login)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Starts an intent of the log in activity
                if(isOnline())
                    startActivity(new Intent(SignUpOrLoginActivity.this, LoginActivity.class));
            }
        });

        // Sign up button click handler
        ((Button) findViewById(R.id.signup)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Starts an intent for the sign up activity
                if(isOnline())
                    startActivity(new Intent(SignUpOrLoginActivity.this, SignUpActivity.class));
            }
        });

        // Load the ImageView that will host the animation and
        ImageView img = (ImageView)findViewById(R.id.drone_animation);

        // Get the background, which has been compiled to an AnimationDrawable object.
        AnimationDrawable frameAnimation = (AnimationDrawable) img.getBackground();

        // Start the animation (looped playback by default).
        frameAnimation.start();
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
