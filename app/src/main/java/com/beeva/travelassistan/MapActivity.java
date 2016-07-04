package com.beeva.travelassistan;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.services.commons.ServicesException;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.directions.v5.DirectionsCriteria;
import com.mapbox.services.directions.v5.MapboxDirections;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;


public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    private MapView mapView;

    private LatLng latLng;
    private Marker marker;

    private String textHtml;
    private DatabaseReference stories;
    private FirebaseDatabase database;
    private IBeaconDevice iBeacon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        textHtml = getIntent().getExtras().getString("text");

        Log.e("TAG", findViewById(R.id.mapview).toString());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Select place for story");
        toolbar.setTitleTextColor(Color.BLACK);
        setSupportActionBar(toolbar);

        final double lat = 52.5119475d;
        final double lon = 13.4228874d;

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
                                        .zoom(12) // Sets the zoom
                                        .bearing(180) // Rotate the camera
                                        .tilt(30) // Set the camera tilt
                                        .build(); // Creates a CameraPosition from the builder
                                mapboxMap.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(position), 1000);

                                mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                                    @Override
                                    public void onMapClick(@NonNull LatLng point) {
                                        if(latLng == null){
                                            marker = mapboxMap.addMarker(new MarkerOptions()
                                                    .position(point));
                                        }
                                        else {
                                            marker.setPosition(point);
                                        }
                                        latLng = point;


                                    }
                                });
                            }
                        });
                    }
                });



        database = FirebaseDatabase.getInstance();
        stories = database.getReference("stories");


        final ProximityManager proximityManager = new ProximityManager(this);

        proximityManager.setIBeaconListener(new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice beacon, IBeaconRegion region) {
                Log.d("Beacon", beacon.toString());
                if(iBeacon == null)
                    iBeacon = beacon;
                else if(beacon.getDistance() <= iBeacon.getDistance())
                    iBeacon = beacon;
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {

            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {

            }
        });
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();

            }
        });
    }


    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                Log.i("ActionBar", "Nuevo!");
                if(latLng == null){
                    Toast.makeText(this, "Hey, select a location!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent intent = this.getIntent();
                    intent.putExtra("lat", latLng.getLatitude());
                    intent.putExtra("lon", latLng.getLongitude());
                    this.setResult(RESULT_OK, intent);

                    Story story = new Story();
                    story.setAuthor("David Hasselhof");
                    story.setText(textHtml);
                    story.setLat(latLng.getLatitude());
                    story.setLon(latLng.getLongitude());
                    if(iBeacon != null)
                        story.setBeaconId(iBeacon.getUniqueId());

                    Map<String, Object> postValues = story.toMap();
                    Task<Void> task = stories.push().setValue(story);
                    task.addOnCompleteListener(MapActivity.this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            finish();
                        }
                    });
                    task.addOnFailureListener(MapActivity.this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            e.printStackTrace();
                        }
                    });


                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
