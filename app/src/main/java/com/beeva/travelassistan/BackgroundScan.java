package com.beeva.travelassistan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ForceScanConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.configuration.scan.EddystoneScanContext;
import com.kontakt.sdk.android.ble.configuration.scan.IBeaconScanContext;
import com.kontakt.sdk.android.ble.configuration.scan.ScanContext;
import com.kontakt.sdk.android.ble.configuration.scan.ScanMode;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.discovery.BluetoothDeviceEvent;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerContract;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.DeviceProfile;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;
import com.kontakt.sdk.android.common.util.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BackgroundScan extends Service {

    private static final String TAG = BackgroundScan.class.getSimpleName();
    private ProximityManager beaconManager;
    private ProximityManagerContract proximityManager;
    private ScanContext scanContext;

    private DatabaseReference stories;
    private FirebaseDatabase database;
    private List<Story> storiesList;

    @Override
    public void onCreate() {
        super.onCreate();
        beaconManager = new ProximityManager(this);
        beaconManager.setIBeaconListener(new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.d("TAG", iBeacon.getProximity().name() + ", " + iBeacon.getUniqueId());
                if(storiesList != null && iBeacon.getDistance() < 10){
                    for (Story story : storiesList) {
                        if(story.getBeaconId() != null &&  story.getBeaconId().equals(iBeacon.getUniqueId()))
                            launchNotification(story);
                    }
                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {

            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {

            }
        });
        beaconManager.configuration().monitoringEnabled(true).scanMode(ScanMode.BALANCED);
        beaconManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                beaconManager.startScanning();

            }
        });

        database = FirebaseDatabase.getInstance();
        stories = database.getReference("stories");
        stories.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                storiesList = new ArrayList<Story>();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    Story post = postSnapshot.getValue(Story.class);
                    storiesList.add(post);
                    Log.d("TAG", post.getAuthor() + "," + post.getBeaconId());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void launchNotification(Story story) {
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Bitmap bitmap = BitmapFactory.decodeResource( getResources(), R.drawable.large_icon);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.ic_stat_notnot)
                .setLargeIcon(bitmap)
                .setContentTitle("You just found a new story [B]")
                .setContentText("From " + story.getAuthor())
                .setDefaults(Notification.DEFAULT_ALL);

        Intent intent = new Intent(this, StoryActivity.class);
        intent.putExtra("text", story.getText());
        intent.putExtra("author", story.getAuthor());
        intent.putExtra("lat", story.getLat());
        intent.putExtra("lon", story.getLon());

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        builder.setContentIntent(resultPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "service started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "service destroyed");
        beaconManager.stopScanning();
        beaconManager.disconnect();
        beaconManager = null;
        super.onDestroy();
    }



}