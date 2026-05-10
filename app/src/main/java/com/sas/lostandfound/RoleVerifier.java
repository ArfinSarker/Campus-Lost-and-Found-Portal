package com.sas.lostandfound;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Utility class to enforce role-based access control throughout the application.
 * Helps prevent unauthorized access to protected activities.
 */
public class RoleVerifier {

    /**
     * Checks if the currently logged-in user has administrator privileges.
     * If not, redirects to the appropriate dashboard or login screen.
     */
    public static void checkAdminAccess(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        String universityId = prefs.getString("universityId", null);
        
        if (universityId == null) {
            redirectToLogin(activity);
            return;
        }

        boolean isAdmin = prefs.getBoolean("isAdminLoggedIn", false);
        
        if (!isAdmin) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Access Denied: Admin privileges required.");
            Intent intent = new Intent(activity, CampusDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        }
    }

    /**
     * Checks if a user is logged in. Redirects to login if not.
     */
    public static void checkUserAccess(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        if (prefs.getString("universityId", null) == null) {
            redirectToLogin(activity);
        }
    }

    private static void redirectToLogin(Activity activity) {
        Intent intent = new Intent(activity, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
