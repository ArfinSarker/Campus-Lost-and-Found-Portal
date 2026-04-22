package com.sas.lostandfound;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class LostAndFoundApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate(savedInstanceState);
        
        // Enable Firebase Database persistence for better performance and offline support
        // This should be done before any other usage of the database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        // Ensure that data is synced even when the app is in the background (optional, but good for real-time)
        // mDatabase.child("LostItems").keepSynced(true);
        // mDatabase.child("FoundItems").keepSynced(true);
    }
}
