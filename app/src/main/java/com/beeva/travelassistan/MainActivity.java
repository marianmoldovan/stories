package com.beeva.travelassistan;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.snapshot.DoubleNode;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements StorableGeofenceManager.StorableGeofenceManagerListener {

    public final static String TAG = "Main";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private DatabaseReference stories;
    private FirebaseDatabase database;

    private MapView mapView;
    private Map<Long, Story> mapOfStories;

    private StorableGeofenceManager mGeofenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuth StateChanged:signed_in:" + user.getUid());

                    database = FirebaseDatabase.getInstance();
                    stories = database.getReference("stories");
                    stories.addValueEventListener(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(final DataSnapshot snapshot) {
                                    Log.e("Count " ,""+snapshot.getChildrenCount());
                                    setGeofences(snapshot);

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

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                                }
                            });
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        mAuth.signInAnonymously()
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "signInAnonymously", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditStoryActivity.class));
            }
        });

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        final double lat = 52.5119475d;
        final double lon = 13.4228874d;

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(lat, lon)) // Sets the new camera position
                        .zoom(8) // Sets the zoom
                        .bearing(180) // Rotate the camera
                        .tilt(30) // Set the camera tilt
                        .build(); // Creates a CameraPosition from the builder
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 1000);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.custom_actionbar);

        mGeofenceManager = new StorableGeofenceManager(this);
        mGeofenceManager.setListener(this);


    }

    private void setGeofences(DataSnapshot snapshot) {
        for (DataSnapshot postSnapshot: snapshot.getChildren()) {
            Story post = postSnapshot.getValue(Story.class);

            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("author", post.getAuthor());
            map.put("text", post.getText());
            map.put("lat", "" + post.getLat());
            map.put("lon", "" + post.getLon());
            StorableGeofence storableGeofence = new StorableGeofence(
                    Double.toString(post.getLat() + post.getLon()),
                    GeoFenceIntentService.class.getName(),
                    post.getLat(),
                    post.getLon(),
                    200,
                    Geofence.NEVER_EXPIRE,
                    Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT,
                    map);
            mGeofenceManager.addGeofence(storableGeofence);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 27) {

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
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
}