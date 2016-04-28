package com.o3dr.droneit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;


public class SignUpActivity extends Activity {

    private EditText usernameView;
    private EditText passwordView;
    private EditText passwordAgainView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_activity);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set up the signup form.
        usernameView = (EditText) findViewById(R.id.username);
        passwordView = (EditText) findViewById(R.id.password);
        passwordAgainView = (EditText) findViewById(R.id.passwordAgain);

        // Set up the submit button click handler
        findViewById(R.id.action_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                logOut();
            }
        });
    }

    private void logOut() {
        if(isOnline()) {
            // Validate the sign up data
            boolean validationError = false;
            StringBuilder validationErrorMessage = new StringBuilder(getResources().getString(R.string.error_intro));
            if (isEmpty(usernameView)) {
                validationError = true;
                validationErrorMessage.append(getResources().getString(R.string.error_blank_username));
            }
            if (isEmpty(passwordView)) {
                if (validationError) {
                    validationErrorMessage.append(getResources().getString(R.string.error_join));
                }
                validationError = true;
                validationErrorMessage.append(getResources().getString(R.string.error_blank_password));
            }
            if (!isMatching(passwordView, passwordAgainView)) {
                if (validationError) {
                    validationErrorMessage.append(getResources().getString(R.string.error_join));
                }
                validationError = true;
                validationErrorMessage.append(getResources().getString(
                        R.string.error_mismatched_passwords));
            }
            validationErrorMessage.append(getResources().getString(R.string.error_end));

            // If there is a validation error, display the error
            if (validationError) {
                Toast.makeText(SignUpActivity.this, validationErrorMessage.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            // Set up a progress dialog
            final ProgressDialog dlg = new ProgressDialog(SignUpActivity.this);
            dlg.setTitle("Please wait.");
            dlg.setMessage("Signing up.  Please wait.");
            dlg.show();

            // Set up a new Parse user
            ParseUser user = new ParseUser();
            user.setUsername(usernameView.getText().toString());
            user.setPassword(passwordView.getText().toString());
            // Call the Parse signup method
            user.signUpInBackground(new SignUpCallback() {

                @Override
                public void done(ParseException e) {
                    dlg.dismiss();
                    if (e != null) {
                        // Show the error message
                        Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        // Start an intent for the dispatch activity
                        Intent intent = new Intent(SignUpActivity.this, DispatchActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            });
        }
    }

    private boolean isEmpty(EditText etText) {
        if (etText.getText().toString().trim().length() > 0) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isMatching(EditText etText1, EditText etText2) {
        if (etText1.getText().toString().equals(etText2.getText().toString())) {
            return true;
        } else {
            return false;
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
