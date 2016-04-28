package com.o3dr.droneit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.ListHolder;
import com.orhanobut.dialogplus.OnBackPressListener;
import com.orhanobut.dialogplus.OnCancelListener;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.OnItemClickListener;
import com.orhanobut.dialogplus.ViewHolder;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.victor.loading.newton.NewtonCradleLoading;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import bolts.Continuation;
import bolts.Task;

public class LevelsActivity extends AppCompatActivity {

    public static final String mainBundleKey = "mainBundle";
    public static final String levelKey = "level";
    public static final String statusKey = "status";
    public static final String timeKey = "time";

    private static final int RESULT_SETTINGS = 1;

    private GridLayout levelGrid;
    private DBHelper db;
    private Set<Integer> userLevels;
    private Set<Integer> allLevels;

    private DialogPlus levelInfoDialog;
    private DialogPlus levelHighscoreDialog;

    private NewtonCradleLoading mask;
    private RelativeLayout maskLayout;
    private ScrollView mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_levels);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.i("LevelsActivity", "onCreate");

        maskLayout = (RelativeLayout) findViewById(R.id.maskLayout);
        mask = (NewtonCradleLoading) findViewById(R.id.newton_cradle_loading);
        mainLayout = (ScrollView) findViewById(R.id.scrollView);

        levelGrid = (GridLayout) findViewById(R.id.levels_layout);
        db = new DBHelper();

        startMask();

        db.getUserLevelsTask().onSuccessTask(new Continuation<List<ParseObject>, Task<List<ParseObject>>>() {
            public Task<List<ParseObject>> then(Task<List<ParseObject>> task) throws Exception {
                List<ParseObject> records = task.getResult();
                userLevels = db.getUserLevels(records);
                return db.getDroneLevelsTask();
            }
        }).onSuccess(new Continuation<List<ParseObject>, Void>() {
            public Void then(Task<List<ParseObject>> task) throws Exception {
                List<ParseObject> records = task.getResult();
                allLevels = db.getDroneLevels(records);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        drawLevelsButtons(userLevels, allLevels);
                        stopMask();
                    }
                });
                return null;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                ParseUser.getCurrentUser().logOutInBackground(new LogOutCallback() {
                    @Override
                    public void done(ParseException e) {
                        startActivity(new Intent(LevelsActivity.this, DispatchActivity.class));
                    }
                });
                break;

            case R.id.menu_settings:
                Intent intent = new Intent(this, UserSettingActivity.class);
                intent.putExtra(UserSettingActivity.from, UserSettingActivity.levelsActivity);
                startActivityForResult(intent, RESULT_SETTINGS);
                break;
        }

        return true;
    }

    private void drawLevelsButtons(Set<Integer> userLevels, Set<Integer> allLevels) {
        int numOfcolumns = 3;
        int numOfrows = 5;

        levelGrid.removeAllViews();
        levelGrid.setColumnCount(numOfcolumns);
        levelGrid.setRowCount(numOfrows);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        int widthMargin = (int)getResources().getDimension(R.dimen.activity_horizontal_margin);
        int levelMargin = (int)getResources().getDimension(R.dimen.activity_levels_margin);
        int screenWidth = size.x - widthMargin*2 - levelMargin*2 - (numOfcolumns*levelMargin)*2;
        int buttonSize = screenWidth/numOfcolumns;

        for (int row = 0; row < numOfrows; row++) {
            for (int column = 0; column < numOfcolumns; column++) {

                final int levelNum = row * numOfcolumns + (column);

                GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(column));

                cellParams.width = buttonSize;
                cellParams.height = buttonSize;
                cellParams.setMargins(levelMargin, levelMargin, levelMargin, levelMargin);

                Button bt = new Button(this);
                bt.setLayoutParams(cellParams);
                bt.setTextColor(Color.WHITE);
                bt.setText(getString(R.string.level) + " " + levelNum);

                Drawable img;
                if(allLevels.contains(levelNum)) {
                    if(levelNum == 0) {
                        bt.setBackgroundResource(R.drawable.circle_button);
                        img = getResources().getDrawable(R.drawable.ic_dialog_map);
                        bt.setText(getString(R.string.free_flight));
                    }
                    else if (levelNum == 1 || userLevels.contains(levelNum) || userLevels.contains(levelNum - 1)) {
                        bt.setBackgroundResource(R.drawable.circle_button);
                        img = getResources().getDrawable(R.drawable.ic_dialog_map);
                    } else {
                        bt.setBackgroundResource(R.drawable.circle_button_disabled);
                        bt.setEnabled(false);
                        img = getResources().getDrawable(R.drawable.ic_lock_idle_lock);
                    }
                }
                else {
                    bt.setText(getString(R.string.coming_soon));
                    bt.setBackgroundResource(R.drawable.circle_button_disabled);
                    bt.setEnabled(false);
                    img = getResources().getDrawable(R.drawable.ic_lock_idle_lock);
                }

                img.setBounds(0, 30, img.getIntrinsicWidth(), img.getIntrinsicHeight() + 30);
                bt.setCompoundDrawables(null, img, null, null);
                bt.setTypeface(null, Typeface.BOLD);

                levelGrid.addView(bt, cellParams);
                bt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onLevelPressed(levelNum);
                    }
                });
            }
        }
    }

    public void onLevelPressed(final int levelNum) {
        ViewHolder viewHolder;
        TextView valueTV = new TextView(LevelsActivity.this);
        valueTV.setBackgroundResource(R.color.colorBackround3);
        valueTV.setTextColor(Color.WHITE);
        valueTV.setTextSize(30f);
        valueTV.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        if(levelNum != 0) {
            valueTV.setText(getString(R.string.level) + " " + levelNum);
            viewHolder = new ViewHolder(R.layout.level_info);
        }
        else {
            valueTV.setText(getString(R.string.free_flight));
            viewHolder = new ViewHolder(R.layout.level_info_freeflight);
        }

        levelInfoDialog = DialogPlus.newDialog(LevelsActivity.this)
                .setContentHolder(viewHolder)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        switch (view.getId()) {
                            case R.id.playBT:
                                if (isOnline()) {
                                    Intent intentGame = new Intent(LevelsActivity.this, GameActivity.class);

                                    Bundle bundleGame = new Bundle();
                                    bundleGame.putInt(GameActivity.levelKey, levelNum);

                                    intentGame.putExtra(GameActivity.gameBundleKey, bundleGame);
                                    startActivity(intentGame);
                                }
                                break;
                            case R.id.highScoresBT:
                                if (isOnline()) {
                                    dialog.dismiss();

                                    db.getLevelHighScoresTask(levelNum).onSuccess(new Continuation<List<ParseObject>, Void>() {
                                        public Void then(Task<List<ParseObject>> task) throws Exception {
                                            List<ParseObject> records = task.getResult();
                                            final ArrayList<Score> arr = db.getLevelHighScores(records);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    new CountDownTimer((int) (1 * 1000), 1000) {
                                                        public void onTick(long millisUntilFinished) {
                                                        }

                                                        public void onFinish() {
                                                            levelHighscoreDialog = DialogPlus.newDialog(LevelsActivity.this)
                                                                    .setContentHolder(new ListHolder())
                                                                    .setAdapter(new ScoreAdapter(LevelsActivity.this, arr))
                                                                    .setHeader(R.layout.activity_high_score_title)
                                                                    .create();
                                                            levelHighscoreDialog.show();
                                                        }
                                                    }.start();
                                                }
                                            });
                                            return null;
                                        }
                                    });
                                }

                                break;
                        }
                        dialog.dismiss();
                    }
                })
                .setHeader(valueTV)
                .create();
        levelInfoDialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            if(levelInfoDialog != null && levelInfoDialog.isShowing()) {
                levelInfoDialog.dismiss();
            }
            else if(levelHighscoreDialog != null && levelHighscoreDialog.isShowing()) {
                levelHighscoreDialog.dismiss();
            }
            else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void startMask() {
        maskLayout.bringToFront();
        maskLayout.setVisibility(View.VISIBLE);
        //gameLayout.setVisibility(View.INVISIBLE);
        mask.start();
    }

    public void stopMask() {
        mainLayout.bringToFront();
        maskLayout.setVisibility(View.INVISIBLE);
        //gameLayout.setVisibility(View.VISIBLE);
        mask.stop();
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

    ////////////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                //showUserSettings();
                break;
        }
    }

//    private void showUserSettings() {
//        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//        StringBuilder builder = new StringBuilder();
//
//        builder.append("\n Sync Frequency: " + sharedPrefs.getString("prefConnectionType", "1"));
//
//        TextView settingsTextView = (TextView) findViewById(R.id.textUserSettings);
//
//        settingsTextView.setText(builder.toString());
//    }
}
