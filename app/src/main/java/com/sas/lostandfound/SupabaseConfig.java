package com.sas.lostandfound;

public class SupabaseConfig {
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    public static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;
    public static final String BUCKET_NAME = "images";

    public static String getDebugStatus() {
        return "URL: " + (SUPABASE_URL != null && !SUPABASE_URL.isEmpty() ? "Configured" : "MISSING") +
               ", Key: " + (SUPABASE_KEY != null && !SUPABASE_KEY.isEmpty() ? "Configured" : "MISSING");
    }
}
