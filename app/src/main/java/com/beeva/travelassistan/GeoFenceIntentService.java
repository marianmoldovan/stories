package com.beeva.travelassistan;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.sousoum.libgeofencehelper.StorableGeofence;
import com.sousoum.libgeofencehelper.StorableGeofenceManager;

import java.util.HashMap;
import java.util.List;


public class GeoFenceIntentService extends IntentService {


    public GeoFenceIntentService() {
        super("GeoFenceIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String notificationText = "Not a geo event";
        GeofencingEvent geoEvent = GeofencingEvent.fromIntent(intent);
        if (geoEvent != null) {
            if (geoEvent.hasError()) {
                notificationText = "Error : " + geoEvent.getErrorCode();
            } else {
                int transition = geoEvent.getGeofenceTransition();
                String transitionStr;
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        transitionStr = "Enter-";
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        transitionStr = "Exit-";
                        break;
                    case Geofence.GEOFENCE_TRANSITION_DWELL:
                        transitionStr = "Dwell-";
                        break;
                    default:
                        transitionStr = "Unknown-";
                }

                StorableGeofenceManager manager = new StorableGeofenceManager(this);

                List<Geofence> triggeringGeo = geoEvent.getTriggeringGeofences();

                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(transitionStr);
                for (int i = 0; i < triggeringGeo.size(); i++) {
                    Geofence geo = triggeringGeo.get(i);
                    StorableGeofence storableGeofence = manager.getGeofence(geo.getRequestId());
                    strBuilder.append(geo.getRequestId());
                    if (storableGeofence != null && storableGeofence.getAdditionalData() != null) {
                        HashMap<String, Object> additionalData = storableGeofence.getAdditionalData();
                        sendNotification(additionalData);
                    }
                }
            }
        }

    }

    private void sendNotification(HashMap<String, Object> story) {
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Bitmap bitmap = BitmapFactory.decodeResource( getResources(), R.drawable.large_icon);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.ic_stat_notnot)
                .setLargeIcon(bitmap)
                .setContentTitle("You just found a new story")
                .setContentText("From " + (String)story.get("author"))
                .setDefaults(Notification.DEFAULT_ALL);

        Intent intent = new Intent(this, StoryActivity.class);
        intent.putExtra("text", (String)story.get("text"));
        intent.putExtra("author", (String)story.get("author"));
        intent.putExtra("lat", Double.parseDouble((String)story.get("lat")));
        intent.putExtra("lon", Double.parseDouble((String)story.get("lon")));

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
}
