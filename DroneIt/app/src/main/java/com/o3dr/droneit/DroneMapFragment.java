package com.o3dr.droneit;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;
import java.util.List;

public class DroneMapFragment extends Fragment implements OnMapReadyCallback {

    private OnFragmentInteractionListener mListener;

    private int maxZoomIn = 20;
    private int maxZoomOut = 15;
    private final float factorDistance = 0.0001f;
    private final float startDelta = 0.0002f;

    private MapFragment googleMap;
    private LatLng droneLocation;
    private CameraPosition savedCameraPosition;
    private boolean hasDroneLocation = false;
    private LatLng defaultLocation = new LatLng(-35.362307, 149.165064);
    private Marker droneMarker;
    private List<LatLngBounds> pathBoundsList;
    private LatLngBounds startBounds;
    private LatLngBounds finishBounds;
    private ArrayList<LatLng> points;
    private Polyline pathDone;
    private ArrayList<Marker> pointsMarkers;
    private ImageView arrow;
    private int pointIndex = 0;

    private boolean isPassStart;
    private boolean isPassFinish;
    private boolean pointsIsInit = false;

    public DroneMapFragment() {
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }

    @Override
    public void onAttach(Activity context) {
        super.onAttach(context);
        Log.i("MapFragment", "onAttach - MapFragment");

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MapFragment", "onCreate - MapFragment");

        pointsMarkers = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_drone_map, container, false);

        GoogleMapOptions mapOptions = new GoogleMapOptions();
        mapOptions.zoomControlsEnabled(true).compassEnabled(true);

        if (savedCameraPosition != null) {
            mapOptions.camera(savedCameraPosition);
        }

        arrow = (ImageView) rootView.findViewById(R.id.arrow);
        googleMap = MapFragment.newInstance(mapOptions);
        FragmentManager childFragMan = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = childFragMan.beginTransaction();
        fragmentTransaction.add(R.id.container, googleMap);
        fragmentTransaction.commit();

        googleMap.getMapAsync(this);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i("MapFragment", "onActivityCreated - MapFragment");
    }

    @Override
    public void onStart() {
        super.onStart();
        hasDroneLocation = false;
        Log.i("MapFragment", "onStart - MapFragment");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("MapFragment", "onResume - MapFragment");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i("MapFragment", "onDestroyView - MapFragment");

        googleMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                savedCameraPosition = googleMap.getCameraPosition();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("MapFragment", "onPause - MapFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i("MapFragment", "onDetach - MapFragment");
        mListener = null;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);

        LatLng location = droneLocation != null ? droneLocation : defaultLocation;
        // Add Marker
        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.quad);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .icon(icon)
                .anchor(0.5f, 0.5f);
        droneMarker = googleMap.addMarker(markerOptions);
    }

    private List<LatLngBounds> initPathBounds(ArrayList<LatLng> points) {
        List<LatLngBounds> boundsList = new ArrayList<>();

        for(int i=0; i<points.size(); i++) {
            boundsList.add(getPointBounds(points.get(i)));
        }

        return boundsList;
    }

    private void initBounds(ArrayList<LatLng> points) {
        pathBoundsList = initPathBounds(points);
        startBounds = pathBoundsList.get(0);
        finishBounds = pathBoundsList.get(points.size() - 1);
    }

    private LatLngBounds getPointBounds(LatLng point) {
        LatLng southwest  = new LatLng(point.latitude - factorDistance, point.longitude - factorDistance);
        LatLng northeast = new LatLng(point.latitude + factorDistance, point.longitude + factorDistance);
        if(southwest.latitude > northeast.latitude)
            return new LatLngBounds(northeast, southwest);
        return new LatLngBounds(southwest, northeast);
    }

    private CameraPosition getCameraPosition(double yaw, double droneAltitude) {

        float alt;
        if (droneAltitude >= 100)
            alt = maxZoomOut;
        else
            alt = maxZoomIn - (float) droneAltitude / 100 * (maxZoomIn - maxZoomOut);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(droneLocation)      // Sets the center of the map to location user
                .zoom(alt)                   // Sets the zoom
                .bearing((int) yaw)          // Sets the orientation of the camera to east
                .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder

        return cameraPosition;
    }

    private void updatePolyLine(LatLng latLng) {
        List<LatLng> points = pathDone.getPoints();
        if(!PolyUtil.isLocationOnPath(latLng, points, false)) {
            points.add(latLng);
            pathDone.setPoints(points);
        }
    }

    public void updateDroneLocation(final LatLong location, final double yaw, final double droneAltitude) {
        if(mListener != null) {
            googleMap.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (location != null) {
                        droneLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        droneMarker.setPosition(droneLocation);

                        CameraPosition cameraPosition = getCameraPosition(yaw, droneAltitude);

                        if (!hasDroneLocation) {
                            hasDroneLocation = true;

                            if (points != null && !pointsIsInit) {
                                pointsIsInit = true;
                                addStartPathLocationToPoints();
                                addDroneLocationToPoints(droneLocation);
                                drawPathOnMap(googleMap, droneLocation);
                                initBounds(points);
                            }

                            Log.i("Service", "hasDroneLocation");
                            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

//                            googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
//                                @Override
//                                public void onMapLoaded() {
                                    mListener.stopMask();
                                    mListener.playBackroundSound();
//                                }
//                            });
                        } else {
                            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }

                        if (points != null) {
                            checkGameStates(droneLocation);
                            drawDoneLinePath();
                            updateArrow(yaw);
                        }
                    }
                }
            });
        }
    }

    private void drawDoneLinePath() {
        if (pointIndex > 0 && pointIndex < pathBoundsList.size() && !pathBoundsList.get(pointIndex - 1).contains(droneLocation)) {
            double angle = Math.toRadians(getAngleBetween2Point(droneLocation, points.get(pointIndex - 1)));

            double distance = SphericalUtil.computeDistanceBetween(droneLocation, points.get(pointIndex - 1));
            double lineAngle = getAngleBetween2Point(points.get(pointIndex - 1), points.get(pointIndex));
            LatLng pos = SphericalUtil.computeOffset(points.get(pointIndex - 1), /*Math.sin(angle)**/distance, -lineAngle);

            updatePolyLine(pos);
        }
    }

    private void updateArrow(double yaw) {
        if(pointIndex < pathBoundsList.size()) {
            arrow.bringToFront();
            double angle = getAngleBetween2Point(droneLocation, points.get(pointIndex));
            arrow.setRotation((float) (-angle - yaw));
        }
        else {
            arrow.setVisibility(View.INVISIBLE);
        }
    }

    private void checkGameStates(LatLng droneLocation) {

        // check if drone recived to waypoint
        if (pointIndex < pathBoundsList.size() && pathBoundsList.get(pointIndex).contains(droneLocation)) {

//            List<LatLng> temp = new ArrayList<>();
//            for(int i=0; i<=pointIndex ; i++) {
//                temp.add(points.get(i));
//            }
//            pathDone.setPoints(temp);

            if(pointIndex != 0 && pointIndex != pointsMarkers.size()-1) {
                pointsMarkers.get(pointIndex).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                mListener.waypointReceived();
            }

            updatePolyLine(pointsMarkers.get(pointIndex).getPosition());
            ++pointIndex;
        }

        // Check for start/win/lose
        boolean isContains = PolyUtil.isLocationOnPath(droneLocation, points, false, 8);
        boolean isOnStartLine = startBounds.contains(droneLocation);
        boolean isOnFinishLine = finishBounds.contains(droneLocation);

        if (isOnStartLine && !isPassStart) {
            isPassStart = true;
            mListener.startGame();
        }
        else if (isOnFinishLine && !isPassFinish) {
            isPassFinish = true;
            mListener.finishGame();
        }
        else if (!isContains && isPassStart && !isPassFinish && !isOnStartLine && !isOnFinishLine) {
            mListener.gameOver();
        }
        else if (!isOnFinishLine && !isOnStartLine && isContains) {
            //mListener.setShimerVisibility(false);
        }
    }

    private void addStartPathLocationToPoints() {
        if(points != null) {
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                LatLng newPoint = new LatLng(point.latitude + startDelta, point.longitude + startDelta);

                points.set(i, newPoint);
            }
        }
    }

    private void addDroneLocationToPoints(LatLng droneLocation) {
        if(points != null) {
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                LatLng newPoint = new LatLng(point.latitude + droneLocation.latitude, point.longitude + droneLocation.longitude);

                points.set(i, newPoint);
            }
        }
    }

    private void drawPathOnMap(GoogleMap googleMap, LatLng droneLocation) {
        if(points != null) {
            Polyline path = googleMap.addPolyline(new PolylineOptions()
                    .width(5)
                    .color(Color.RED));
            path.setPoints(points);

            double alpha;
            alpha = getAngleBetween2Point(points.get(0), points.get(1));
            MarkerOptions startMarkerOption = initImageMarker(points.get(0), alpha, "start_flag");
            pointsMarkers.add(googleMap.addMarker(startMarkerOption));

            for (int i = 1; i < points.size() - 1; i++) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(points.get(i));

                pointsMarkers.add(googleMap.addMarker(markerOptions));
            }

            alpha = getAngleBetween2Point(points.get(points.size() - 1), points.get(points.size() - 2));
            MarkerOptions finishMarkerOption = initImageMarker(points.get(points.size() - 1), alpha, "finish_flag");
            pointsMarkers.add(googleMap.addMarker(finishMarkerOption));

            pathDone = googleMap.addPolyline(new PolylineOptions()
                    .width(5)
                    .color(Color.BLUE));
        }
    }

    private double getAngleBetween2Point(LatLng point1, LatLng point2) {
        double lat1=point1.latitude;
        double lat2=point2.latitude;
        double long1=point1.longitude;
        double long2=point2.longitude;
        double deltaLong=long2-long1;
        double angle = Math.atan2(Math.sin(deltaLong)*Math.cos(lat2), Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(deltaLong));

        return Math.toDegrees(angle);
    }

    public Bitmap resizeMapIcons(String iconName, float scale){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(iconName, "drawable", getActivity().getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, (int)(imageBitmap.getWidth()*scale), (int)(imageBitmap.getHeight()*scale), false);
        return resizedBitmap;
    }

    private MarkerOptions initImageMarker(LatLng location, double alpha, String source) {

        //BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(source);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(source, 0.3f)))
                .anchor(0.5f, 0.5f)
                .rotation((float)alpha + 180)
                .flat(true);

        return markerOptions;
    }

    public interface OnFragmentInteractionListener {
        void startTime();
        void stopTime();
        void playBackroundSound();
        void startMask();
        void stopMask();
        void startGame();
        void finishGame();
        void gameOver();
        void waypointReceived();
    }
}
