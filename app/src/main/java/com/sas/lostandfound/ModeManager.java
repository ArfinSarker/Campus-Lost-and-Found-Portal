package com.sas.lostandfound;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to manage the active operational mode of the application.
 * Allows Admins to switch between behaving as a regular user and a system administrator.
 */
public class ModeManager {
    
    private static final String PREF_NAME = "MyApp";
    private static final String KEY_ACTIVE_MODE = "activeMode";
    
    public static final String MODE_USER = "user";
    public static final String MODE_ADMIN = "admin";

    /**
     * Sets the active mode for the application.
     */
    public static void setMode(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_MODE, mode).apply();
    }

    /**
     * Gets the current active mode. Defaults to MODE_USER.
     */
    public static String getMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACTIVE_MODE, MODE_USER);
    }

    /**
     * Helper method to check if the app is currently in Admin Mode.
     * Requires the user to actually BE an admin AND have Admin Mode active.
     */
    public static boolean isAdminMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isAdminAccount = prefs.getBoolean("isAdminLoggedIn", false);
        String currentMode = prefs.getString(KEY_ACTIVE_MODE, MODE_USER);
        
        return isAdminAccount && MODE_ADMIN.equals(currentMode);
    }
}
