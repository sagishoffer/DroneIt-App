package com.o3dr.droneit;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

public class ControlService extends Service implements DroneListener, TowerListener {
    private LocalBinder mBinder = new LocalBinder();
    private GameListener mGameListener;

    private Drone drone;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private int droneType = Type.TYPE_UNKNOWN;
    private final int DEFAULT_UDP_PORT = 14550;
    private final int DEFAULT_USB_BAUD_RATE = 57600;

    private final float DRONE_STEP = 0.0003f;
    private final float DRONE_ANGLE_STEP = 5;
    private final float DRONE_ALT_STEP = 1;

    private boolean isArmed;

    public ControlService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Service", "onCreate - Service");

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.controlTower.connect(this);
        Log.i("Service", "onBind - Service");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("Service", "onUnbind - Service");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Service", "onDestroy - Service");

        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                Log.i("Service", "Drone Connected");
                mGameListener.setArmButtonVisibility(true);
                checkState();
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED);
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                Log.i("Service", "Drone Disconnected");
                mGameListener.setArmButtonVisibility(false);
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                checkState();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    //updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                //updateVehicleMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeedStatus();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitudeStatus();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;

            case AttributeEvent.GPS_POSITION:
                Log.i("Service", "GPS_POSITION");
                Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
                LatLong vehiclePosition = droneGps.getPosition();

                Attitude at = this.drone.getAttribute(AttributeType.ATTITUDE);
                double yaw = at.getYaw();

                Altitude alt = this.drone.getAttribute(AttributeType.ALTITUDE);
                double droneAltitude = alt.getAltitude();

                mGameListener.updateDroneLocation(vehiclePosition, yaw, droneAltitude);

                break;

            default:
//                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onTowerConnected() {
        Log.i("Service", "onTowerConnected - Service");
        //alertUser("3DR Services Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
        connectToDrone();
    }

    @Override
    public void onTowerDisconnected() {

    }

    private void connectToDrone() {
        Bundle extraParams = new Bundle();
        int selectedConnectionType = ConnectionType.TYPE_USB;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String settingType = sharedPrefs.getString("prefConnectionType", "1");
        switch (settingType) {
            case "0":
                selectedConnectionType = ConnectionType.TYPE_USB;
                extraParams.putInt(ConnectionType.EXTRA_USB_BAUD_RATE, DEFAULT_USB_BAUD_RATE); // Set default baud rate to 57600
                break;

            case "1":
                selectedConnectionType = ConnectionType.TYPE_UDP;
                extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, DEFAULT_UDP_PORT); // Set default baud rate to 14550
                break;
        }

        ConnectionParameter connectionParams = new ConnectionParameter(selectedConnectionType, extraParams, null);
        this.drone.connect(connectionParams);
    }

    public void checkState() {
        State vehicleState = drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying()) {
            // Land
            isArmed = true;
            mGameListener.setArmButtonVisibility(true);
            mGameListener.changeToFlying();
            Log.i("Service", "isFlying");
        } else if (vehicleState.isArmed()) {
            // Take off
            //mGameListener.setJoystickVisibility(true);
            isArmed = true;
            mGameListener.setArmButtonVisibility(true);
            mGameListener.changeToArmed();
        }
        else if (vehicleState.isConnected()) {
            // Connected but not Armed
            isArmed = false;
            mGameListener.setArmButtonVisibility(true);
            mGameListener.droneConected();
            //mGameListener.setJoystickVisibility(false);
        }
    }

    private void updateAltitudeStatus() {
        Altitude droneAltitude = drone.getAttribute(AttributeType.ALTITUDE);
        mGameListener.updateAltitude(droneAltitude);
    }

    private void updateSpeedStatus() {
        Speed droneSpeed = drone.getAttribute(AttributeType.SPEED);
        mGameListener.updateSpeed(droneSpeed);
    }

    private void updateDistanceFromHome() {
        Altitude droneAltitude = drone.getAttribute(AttributeType.ALTITUDE);
        Gps droneGps = drone.getAttribute(AttributeType.GPS);

        double vehicleAltitude = droneAltitude.getAltitude();
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }
    }

    private double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public class LocalBinder extends Binder {

        void registerListener(GameListener listener) {
            mGameListener = listener;
            checkState();
        }

        void armButtonPressed() {
            State vehicleState = drone.getAttribute(AttributeType.STATE);
            if (vehicleState.isFlying()) {
                // Land
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LAND);
            } else if (vehicleState.isArmed()) {
                // Take off
                ControlApi.getApi(drone).takeoff(10, null);
            } else if (!vehicleState.isConnected()) {
                // Connect
                //alertUser("Connect to a drone first");
            } else {
                // Connected but not Armed
                VehicleApi.getApi(drone).arm(true);
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED);
            }
        }

        // angle 0-360 ,speed 0-1
        public void goTo(float angle, double speed) {
            if(drone != null && drone.isConnected() && drone.isStarted() && isArmed) {
                Attitude at = drone.getAttribute(AttributeType.ATTITUDE);
                Gps droneGps = drone.getAttribute(AttributeType.GPS);

                if(at != null && droneGps != null) {
                    double yaw = at.getYaw();
                    double alpha = Math.toRadians(yaw + angle);
                    double x = DRONE_STEP * Math.sin(alpha);
                    double y = DRONE_STEP * Math.cos(alpha);
                    Log.i("GameActivity", "Yaw = " + yaw);
                    Log.i("GameActivity", "speed = " + speed);
                    Log.i("GameActivity", "x = " + x);
                    Log.i("GameActivity", "y = " + y);

                    LatLong vehiclePosition = droneGps.getPosition();

                    if (vehiclePosition != null) {
                        LatLong latLong = new LatLong(vehiclePosition.getLatitude() + y, vehiclePosition.getLongitude() + x);
                        ControlApi.getApi(drone).goTo(latLong, false, null);
                        ControlApi.getApi(drone).turnTo((float) yaw, 4, true, false, null);
                    }

                }
            }
        }

        public void onForwardClick(View view) {
            goTo(0, 1);
        }

        public void onBackwardClick(View view) {
            goTo(180, 1);
        }

        public void onRightClick(View view) {
            goTo(90, 1);
        }

        public void onLeftClick(View view) {
            goTo(-90, 1);
        }

        public void pauseAtCurrentLocation() {
            if(drone.isConnected() && drone.isStarted() && isArmed) {
                ControlApi.getApi(drone).pauseAtCurrentLocation(null);
            }
        }

        public void onYawLeftClick() {
            if(drone.isConnected() && drone.isStarted() && isArmed) {
                Attitude at = drone.getAttribute(AttributeType.ATTITUDE);
                double yaw = at.getYaw() - DRONE_ANGLE_STEP;
                if (yaw < 0)
                    yaw += 360;
                ControlApi.getApi(drone).turnTo((float) yaw, 4, true, false, null);
            }
        }

        public void onYawRightClick() {
            if(drone.isConnected() && drone.isStarted() && isArmed) {
                Attitude at = drone.getAttribute(AttributeType.ATTITUDE);
                double yaw = at.getYaw();
                ControlApi.getApi(drone).turnTo(DRONE_ANGLE_STEP, 4, true, true, null);
            }
        }

        void goUp() {
            if(drone.isConnected() && drone.isStarted() && isArmed) {
                Altitude alt = drone.getAttribute(AttributeType.ALTITUDE);
                ControlApi.getApi(drone).climbTo(alt.getAltitude() + DRONE_ALT_STEP);
            }
        }

        void goDown() {
            if(drone.isConnected() && drone.isStarted() && isArmed) {
                Altitude alt = drone.getAttribute(AttributeType.ALTITUDE);
                ControlApi.getApi(drone).climbTo(alt.getAltitude() - DRONE_ALT_STEP);
            }
        }
    }

    public interface GameListener {
        void setArmButtonVisibility(boolean visible);
        void updateSpeed(Speed droneSpeed);
        void updateAltitude(Altitude droneAltitude);
        void updateDroneLocation(LatLong vehiclePosition, double yaw, double droneAltitude);
        void setArmButtonText(String txt);
        void changeToFlying();
        void changeToArmed();
        void droneConected();
    }

}
