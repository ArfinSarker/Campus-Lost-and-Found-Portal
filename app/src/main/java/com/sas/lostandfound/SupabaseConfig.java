package com.sas.lostandfound;

public class SupabaseConfig {
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;
    public static final String BUCKET_NAME = "images";

    public static final String FIREBASE_API_KEY = BuildConfig.FIREBASE_API_KEY;
    public static final String FIREBASE_APP_ID = BuildConfig.FIREBASE_APP_ID;
    public static final String FIREBASE_SENDER_ID = BuildConfig.FIREBASE_SENDER_ID;
    public static final String FIREBASE_PROJECT_ID = BuildConfig.FIREBASE_PROJECT_ID;

    public static String getDebugStatus() {
        return "URL: " + (SUPABASE_URL != null && !SUPABASE_URL.isEmpty() ? "Configured" : "MISSING") +
               ", Key: " + (SUPABASE_KEY != null && !SUPABASE_KEY.isEmpty() ? "Configured" : "MISSING");
    }
}
