package com.sas.lostandfound;

import android.app.Application;

public class LostAndFoundApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Register SnackbarManager to track activity lifecycle
        registerActivityLifecycleCallbacks(SnackbarManager.getInstance());
    }
}
