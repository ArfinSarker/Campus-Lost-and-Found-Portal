package com.sas.lostandfound;

import android.app.Application;
import android.content.Context;

public class LostAndFoundApplication extends Application {
    private static Context instance;

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = getApplicationContext();

        // Register SnackbarManager to track activity lifecycle
        registerActivityLifecycleCallbacks(SnackbarManager.getInstance());

        // Initialize Database Helper
        SupabaseDatabaseHelper.init(this);
    }
}
