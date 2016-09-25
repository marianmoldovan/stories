package com.beeva.travelassistan;

import android.*;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapzen.android.lost.api.Geofence;
import com.sousoum.libgeofencehelper.StorableGeofence;
import com.sousoum.libgeofencehelper.StorableGeofenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

import static android.R.attr.button;

public class MainActivity extends AppCompatActivity implements StorableGeofenceManager.StorableGeofenceManagerListener, FirebaseAuth.AuthStateListener {

    public final static String TAG = "Main";
    private static final int ASK_FOR_GPS_PERMISSION = 27;
    private boolean permissionGranted = false;

    private FirebaseAuth mAuth;

    private DatabaseReference stories;
    private FirebaseDatabase database;

    private MapView mapView;
    private Map<Long, Story> mapOfStories;

    private StorableGeofenceManager mGeofenceManager;

    private DataSnapshot cachedDataSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.custom_actionbar);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditStoryActivity.class));
            }
        });

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(this);
        mAuth.signInAnonymously();

        mGeofenceManager = new StorableGeofenceManager(this);

        askPermission();
    }


    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void geofenceAddStatus(StorableGeofence storableGeofence, Status status) {
        Log.d("Geo", status.toString());
    }

    @Override
    public void geofenceRemoveStatus(String s, Status status) {
        Log.d(s, status.toString());

    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && cachedDataSnapshot == null) {
            // User is signed in
            Log.d(TAG, "onAuth StateChanged:signed_in:" + user.getUid());

            database = FirebaseDatabase.getInstance();
            stories = database.getReference("stories");
            stories.addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot snapshot) {
                        Log.e("Count " ,""+snapshot.getChildrenCount());
                        cachedDataSnapshot = snapshot;
                        setStoriesOnMap(snapshot);
                        if(permissionGranted)
                            setGeofences();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
            firebaseAuth.removeAuthStateListener(this);
        } else {
            // User is signed out
            Log.d(TAG, "onAuthStateChanged:signed_out");
        }
    }

    private void setStoriesOnMap(final DataSnapshot snapshot){
        mapOfStories = new HashMap<Long, Story>();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                    Story post = postSnapshot.getValue(Story.class);

                    Log.e("Get Data", post.toString());

                    IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                    Drawable iconDrawable = ContextCompat.getDrawable(MainActivity.this, R.drawable.marker_2);
                    Icon icon = iconFactory.fromDrawable(iconDrawable);

                    Marker marker = mapboxMap.addMarker(new MarkerOptions()
                            .position(new LatLng(post.getLat(), post.getLon()))
                            .title("Story from " + post.getAuthor())
                            .snippet(post.getText()));
                    mapOfStories.put(marker.getId(), post);

                    mapboxMap.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(@NonNull Marker marker) {
                            Log.d("TAG", marker.getSnippet());
                            Story story = mapOfStories.get(marker.getId());
                            Intent intent = new Intent(MainActivity.this, StoryActivity.class);
                            intent.putExtra("text", story.getText());
                            intent.putExtra("author", story.getAuthor());
                            intent.putExtra("lat", story.getLat());
                            intent.putExtra("lon", story.getLon());
                            startActivity(intent);
                            return true;
                        }
                    });
                }
            }
        });
    }

    private void setGeofences() {
        for (DataSnapshot postSnapshot: cachedDataSnapshot.getChildren()) {
            Story post = postSnapshot.getValue(Story.class);
            String geofenceId = Double.toString(post.getLat() + post.getLon());

            if(mGeofenceManager.getGeofence(geofenceId) != null) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("author", post.getAuthor());
                map.put("text", post.getText());
                map.put("lat", "" + post.getLat());
                map.put("lon", "" + post.getLon());
                StorableGeofence storableGeofence = new StorableGeofence(
                        Double.toString(post.getLat() + post.getLon()),
                        GeoFenceIntentService.class.getName(),
                        post.getLat(), post.getLon(), 250,
                        Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_ENTER, map);

                mGeofenceManager.addGeofence(storableGeofence);
            }
        }
    }

    private void centerMapOnLocation() {
        SmartLocation.with(this).location()
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(final Location location) {
                        mapView.getMapAsync(new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(final MapboxMap mapboxMap) {
                                CameraPosition position = new CameraPosition.Builder()
                                        .target(new LatLng(location.getLatitude(), location.getLongitude())) // Sets the new camera position
                                        .zoom(8) // Sets the zoom
                                        .bearing(0) // Rotate the camera
                                        .tilt(30) // Set the camera tilt
                                        .build(); // Creates a CameraPosition from the builder
                                mapboxMap.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(position), 1000);
                            }
                        });
                    }
                });
    }

    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ASK_FOR_GPS_PERMISSION);
            }
        }
        else {
            permissionGranted = true;
            if(cachedDataSnapshot != null)
                setGeofences();
            centerMapOnLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ASK_FOR_GPS_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    if(cachedDataSnapshot != null)
                        setGeofences();
                    centerMapOnLocation();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

        }
    }


}