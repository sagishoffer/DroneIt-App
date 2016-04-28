package com.o3dr.droneit;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.parse.ParseObject;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.victor.loading.newton.NewtonCradleLoading;

import java.util.ArrayList;

import bolts.Continuation;
import bolts.Task;

public class GameActivity extends AppCompatActivity implements DroneMapFragment.OnFragmentInteractionListener, ControlService.GameListener{

    private final double SECONDS_FOR_WAIT = 2;
    private final double SHIMMER_WAIT = 1;
    private long lastPress;

    public static final String gameBundleKey = "gameBundle";
    public static final String levelKey = "level";

    private Handler timer = new Handler();
    private boolean timerStarted;
    private int secondsPassed = 0;
    private TextView timerLabel;
    private TextView distanceValueTextView;
    private TextView waypoints;

    private Shimmer shimmer;
    private ShimmerTextView stv;
    private DialogFragment dialog;
    private boolean isDialogOn;

    private NewtonCradleLoading mask;
    private RelativeLayout maskLayout;
    private RelativeLayout gameLayout;

    private int lastRightJoyStickDirection = JoyStickClass.STICK_NONE;
    private float lastAngle = 0;
    private long lastRightJoyStickClicked;

    private RelativeLayout layout_joystickLeft, layout_joystickRight;
    private JoyStickClass jsLeft, jsRight;

    private DroneMapFragment map;

    private boolean isGameStarted = false;
    private int waypointIndex = 0;
    private int waypointLength;
    private int level = 0;
    private boolean isBound;
    private ControlService.LocalBinder mBinder;

    private DBHelper db;
    private ParseObject levelObj;
    private MediaPlayer mpSound, mpWaypoint, mpWin, mpLose;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (ControlService.LocalBinder)service;
            mBinder.registerListener(GameActivity.this);
            isBound = true;
            Log.i("GameAcivity", "ServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.i("GameAcivity", "ServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("GameActivity", "onCreate - GameActivity");

        setContentView(R.layout.activity_game);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        db = new DBHelper();

        maskLayout = (RelativeLayout) findViewById(R.id.maskLayout);
        gameLayout = (RelativeLayout) findViewById(R.id.gameLayout);
        mask = (NewtonCradleLoading) findViewById(R.id.newton_cradle_loading);
        timerLabel = (TextView) findViewById(R.id.timerTextView);
        distanceValueTextView = (TextView) findViewById(R.id.distanceValueTextView);
        waypoints = (TextView) findViewById(R.id.waypoints);
        stv = (ShimmerTextView) findViewById(R.id.status_tv);
        stv.bringToFront();

        shimmer = new Shimmer();

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra(GameActivity.gameBundleKey);
        level = bundle.getInt(GameActivity.levelKey);

        createLeftJoyStick();
        createRightJoyStick();

        mpSound = MediaPlayer.create(this, R.raw.wind_louder);
        mpSound.setLooping(true);

        map = new DroneMapFragment();
        Task<ParseObject> levelObjectTask = db.getLevelObject(level);
        levelObjectTask.onSuccess(new Continuation<ParseObject, Void>() {
            public Void then(Task<ParseObject> task) throws Exception {
                // Everything is done!
                levelObj = task.getResult();
                ArrayList<LatLng> points = db.getPath(levelObj);
                if (points != null) {
                    map.setPoints(points);
                    final double pathLength = SphericalUtil.computeLength(points);
                    waypointLength = points.size();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            distanceValueTextView.setText((int) pathLength + "m");
                            waypoints.setText(waypointIndex + "/" + waypointLength);

                            FragmentTransaction transaction = getFragmentManager().beginTransaction();
                            transaction.add(R.id.mapContainer, map);
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    });
                }
                return null;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i("GameActivity", "OnStart - GameActivity");

        doBindService();

        startMask();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(isGameStarted) {
            startTime();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTime();
        stopBackroundSound();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("GameActivity", "onStop - GameActivity");

        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mpSound != null)
            mpSound.release();

        if(mpWaypoint != null)
            mpWaypoint.release();

        if(mpWin != null)
            mpWin.release();

        if(mpLose != null)
            mpLose.release();
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.

        if(!isBound) {
            Intent serviceIntent = new Intent(this, ControlService.class);
            bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
            Log.i("GameAcivity", "doBindService.");
        }
    }

    void doUnbindService() {
        if (isBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            isBound = false;
            Log.i("GameAcivity", "Unbinding.");
        }
    }

    // UI Events
    // ==========================================================

    public void onArmButtonTap(View view) {
        mBinder.armButtonPressed();
    }

    public void createLeftJoyStick() {
        layout_joystickLeft = (RelativeLayout) findViewById(R.id.layout_joystickLeft);

        jsLeft = new JoyStickClass(getApplicationContext(), layout_joystickLeft, R.drawable.image_button);
        jsLeft.setStickSize(150, 150);
        jsLeft.setLayoutSize(400, 400);
        jsLeft.setLayoutAlpha(150);
        jsLeft.setStickAlpha(100);
        jsLeft.setOffset(90);
        jsLeft.setMinimumDistance(50);

        layout_joystickLeft.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {

                jsLeft.drawStick(arg1);
                if (arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
//                    textView1.setText("X : " + String.valueOf(js.getX()));
//                    textView2.setText("Y : " + String.valueOf(js.getY()));
//                    textView3.setText("Angle : " + String.valueOf(js.getAngle()));
//                    textView4.setText("Distance : " + String.valueOf(js.getDistance()));

                    int direction = jsLeft.get8Direction();
                    if (direction == JoyStickClass.STICK_UP) {
//                        textView5.setText("Direction : Up");
                        mBinder.goUp();
                    } else if (direction == JoyStickClass.STICK_UPRIGHT) {
//                        textView5.setText("Direction : Up Right");
                    } else if (direction == JoyStickClass.STICK_RIGHT) {
//                        textView5.setText("Direction : Right");

                        mBinder.onYawRightClick();
                    } else if (direction == JoyStickClass.STICK_DOWNRIGHT) {
//                        textView5.setText("Direction : Down Right");
                    } else if (direction == JoyStickClass.STICK_DOWN) {
//                        textView5.setText("Direction : Down");
                        mBinder.goDown();
                    } else if (direction == JoyStickClass.STICK_DOWNLEFT) {
//                        textView5.setText("Direction : Down Left");
                    } else if (direction == JoyStickClass.STICK_LEFT) {
//                        textView5.setText("Direction : Left");
                        mBinder.onYawLeftClick();
                    } else if (direction == JoyStickClass.STICK_UPLEFT) {
//                        textView5.setText("Direction : Up Left");
                    } else if (direction == JoyStickClass.STICK_NONE) {
//                        textView5.setText("Direction : Center");
                    }
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                }

                return true;
            }
        });
    }

    public void createRightJoyStick() {
        layout_joystickRight = (RelativeLayout) findViewById(R.id.layout_joystickRight);

        jsRight = new JoyStickClass(getApplicationContext(), layout_joystickRight, R.drawable.image_button);
        jsRight.setStickSize(150, 150);
        jsRight.setLayoutSize(400, 400);
        jsRight.setLayoutAlpha(150);
        jsRight.setStickAlpha(100);
        jsRight.setOffset(90);
        jsRight.setMinimumDistance(50);

        layout_joystickRight.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {

                jsRight.drawStick(arg1);
                if (arg1.getAction() == MotionEvent.ACTION_DOWN || arg1.getAction() == MotionEvent.ACTION_MOVE) {
//                    textView1.setText("X : " + String.valueOf(js.getX()));
//                    textView2.setText("Y : " + String.valueOf(js.getY()));
//                    textView3.setText("Angle : " + String.valueOf(js.getAngle()));
//                    textView4.setText("Distance : " + String.valueOf(js.getDistance()));
                    int delta = 5;
                    if (jsRight.getTouchState()) {
                        double distance = jsRight.getDistance();
                        float angle = jsRight.getAngle() + 90;
                        double radius = jsRight.getJoyStickRadius();
                        double minDistance = jsRight.getMinimumDistance();

                        Log.i("Right JoyStick", "Distance: " + distance);
                        Log.i("Right JoyStick", "Angle: " + angle);
                        Log.i("Right JoyStick", "Radius: " + radius);

                        if (distance > radius)
                            distance = radius;
                        if (distance < minDistance)
                            distance = minDistance;

                        int direction = jsRight.get8Direction();
                        Log.i("Right JoyStick", "direction: " + direction);
                        Log.i("Right JoyStick", "lastDirection: " + lastRightJoyStickDirection);

                        long currentTime = System.currentTimeMillis();
//                            if (direction != lastRightJoyStickDirection || currentTime - lastRightJoyStickClicked > 1000) {
                        if (Math.abs(angle - lastAngle) > delta || currentTime - lastRightJoyStickClicked > 2000) {
                            mBinder.goTo(angle, distance / radius);
                            lastRightJoyStickDirection = direction;
                            lastRightJoyStickClicked = currentTime;
                            lastAngle = angle;
                        }
                    }
                } else {
                    mBinder.pauseAtCurrentLocation();
                }

                return true;
            }
        });
    }

    // UI updating
    // ==========================================================

    public void setJoystickVisibility(boolean visibility) {
        int visibleType = visibility ? View.VISIBLE : View.INVISIBLE;
        layout_joystickLeft.setVisibility(visibleType);
        layout_joystickRight.setVisibility(visibleType);
    }

    @Override
    public void changeToFlying() {
        setArmButtonText("LAND");
    }

    @Override
    public void changeToArmed() {
        //stopMask();
        setArmButtonText("TAKE OFF");
    }

    @Override
    public void droneConected() {
        //stopMask();
        setArmButtonText("ARM");
    }

    public void setArmButtonVisibility(boolean visible) {
        Log.i("MainActivity", "setArmButtonVisibility = " + visible);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!visible) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }
    }

    public void setArmButtonText(String txt) {
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);
        armButton.setText(txt);
    }

    public void updateAltitude(Altitude droneAltitude) {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    public void updateSpeed(Speed droneSpeed) {
        TextView speedVrTextView = (TextView) findViewById(R.id.speedVrTextView);
        speedVrTextView.setText(String.format("vr: %3.1f", droneSpeed.getVerticalSpeed()) + "m/s");

        TextView speedHrTextView = (TextView) findViewById(R.id.speedHrTextView);
        speedHrTextView.setText(String.format("hr: %3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    public void updateDroneLocation(LatLong vehiclePosition, double yaw, double droneAltitude) {
        if(!isDialogOn) {
            map.updateDroneLocation(vehiclePosition, yaw, droneAltitude);
        }
    }

    public void startTime() {
        if (!timerStarted) {
            timer.postDelayed(updateTimeElasped, 1000);
            timerStarted = true;
        }
    }

    public void stopTime() {
        timer.removeCallbacks(updateTimeElasped);
        timerStarted = false;
    }

    public void playBackroundSound() {
        mpSound.start();
    }

    public void stopBackroundSound() {
        if(mpSound.isPlaying())
            mpSound.pause();
    }

    public void playWaypointSound() {
        mpWaypoint = MediaPlayer.create(this, R.raw.waypoint);
        mpWaypoint.start();
    }

    public void playWinSound() {
        mpWin = MediaPlayer.create(this, R.raw.win);
        mpWin.start();
    }

    public void playLoseSound() {
        mpLose = MediaPlayer.create(this, R.raw.lose);
        mpLose.start();
    }

    public void startMask() {
        maskLayout.bringToFront();
        maskLayout.setVisibility(View.VISIBLE);
        //gameLayout.setVisibility(View.INVISIBLE);
        mask.start();
    }

    public void stopMask() {
        gameLayout.bringToFront();
        maskLayout.setVisibility(View.INVISIBLE);
        //gameLayout.setVisibility(View.VISIBLE);
        mask.stop();
    }

    public void setShimerVisibility(boolean visible) {
        if(visible) {
            stv.setVisibility(ShimmerTextView.VISIBLE);
            shimmer.start(stv);
        }
        else {
            shimmer.cancel();
            stv.setVisibility(ShimmerTextView.INVISIBLE);
        }
    }

    public void showShimmer(String msg) {
        stv.setText(msg);
        stv.setVisibility(ShimmerTextView.VISIBLE);
        shimmer.start(stv);

        new CountDownTimer((int) (SECONDS_FOR_WAIT * 1000), 1000) {

            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                shimmer.cancel();
                stv.setVisibility(ShimmerTextView.INVISIBLE);
            }
        }.start();
    }

    private void updateWaypointsCounter() {
        waypoints.setText(++waypointIndex + "/" + waypointLength);
    }

    // Game methods
    // ==========================================================

    public void startGame() {
        if(!isDialogOn) {
            isGameStarted = true;
            showShimmer(getString(R.string.start_line));
            updateWaypointsCounter();
//            stv.setText(R.string.start_line);
//            setShimerVisibility(true);
            startTime();
            playWaypointSound();
        }
    }

    public void waypointReceived() {
        if(!isDialogOn) {
            showShimmer(getString(R.string.waypoint_received));
            updateWaypointsCounter();
            playWaypointSound();
        }
    }

    public void finishGame() {
        if(!isDialogOn) {
            showShimmer(getString(R.string.finish_line));
            updateWaypointsCounter();
            stopTime();
            playWinSound();

            db.insertScore(secondsPassed, levelObj.getObjectId());

            new CountDownTimer((int) (SECONDS_FOR_WAIT * 1000), 1000) {

                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    dialog = DialogFragment.newInstance(4, 4, false, false, secondsPassed, true, level);
                    dialog.show(getFragmentManager(), "blur_sample");
                }
            }.start();
        }
    }

    public void gameOver() {
        if(!isDialogOn) {
            stv.setText(R.string.game_over);
            setShimerVisibility(true);
            stopTime();
            playLoseSound();

            new CountDownTimer((int) (SECONDS_FOR_WAIT * 1000), 1000) {

                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    dialog = DialogFragment.newInstance(4, 4, false, false, secondsPassed, false, level);
                    dialog.show(getFragmentManager(), "game_dialog");
                }
            }.start();

            isDialogOn = true;
        }
    }

//    public void updateDoneLength(double all, double part) {
//        Log.i("updateDoneLength", "all - " + all);
//        Log.i("updateDoneLength", "part - " + part);
//        int precent = (int)(part/all*100);
//        distanceValueTextView.setText(precent +  "%");
//    }


    // Helper methods
    // ==========================================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            long currentTime = System.currentTimeMillis();

            if(currentTime - lastPress > 5000){
                alertUser(R.string.game_press_back_alert);
                lastPress = currentTime;
            }
            else {
                gotoMainActivity();
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void gotoMainActivity() {
        Intent intent = new Intent(this, LevelsActivity.class);
        startActivity(intent);
    }

    protected void alertUser(int resId) {
        alertUser(getString(resId));
    }
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // timer call back when timer is ticked
    private Runnable updateTimeElasped = new Runnable()
    {
        public void run()
        {
            ++secondsPassed;
            timerLabel.setText(secondsPassed + "s");

            long currentMilliseconds = System.currentTimeMillis();
            timer.postAtTime(this, currentMilliseconds);
            timer.postDelayed(updateTimeElasped, 1000);
        }
    };
}

