package com.beeva.travelassistan;


import android.app.Application;
import android.content.Intent;
import android.support.multidex.MultiDexApplication;

import com.kontakt.sdk.android.common.KontaktSDK;

public class App extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}