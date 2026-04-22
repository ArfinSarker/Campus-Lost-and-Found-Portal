package com.sas.lostandfound;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class LostAndFoundApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Enable Firebase Database persistence for better performance and offline support.
        // This allows the app to load data from the local cache immediately while fetching updates.
        // This MUST be called before any other usage of FirebaseDatabase.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // In case it was already initialized elsewhere
            e.printStackTrace();
        }
    }
}
