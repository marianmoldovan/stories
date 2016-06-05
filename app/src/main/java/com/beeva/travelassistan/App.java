package com.beeva.travelassistan;


import android.app.Application;
import android.content.Intent;

import com.kontakt.sdk.android.common.KontaktSDK;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KontaktSDK.initialize(this);

        Intent intent = new Intent(this, BackgroundScan.class);
        startService(intent);
    }
}