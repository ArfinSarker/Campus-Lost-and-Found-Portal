package com.sas.lostandfound;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Centrally manages the application's appearance and theme mode.
 * Supports toggling strictly between Light Mode (Default) and Dark Mode.
 */
public class ThemeManager {
    private static final String PREFS_NAME = "MyApp";
    private static final String KEY_DARK_MODE = "darkModeEnabled";

    /**
     * Resolves the current theme setting from preferences and applies it.
     */
    public static void applyTheme(Context context) {
        boolean isDark = isDarkModeEnabled(context);
        applyThemeMode(isDark);
    }

    /**
     * Applies the given theme mode using AppCompatDelegate.
     */
    public static void applyThemeMode(boolean isDark) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Retrieves the persisted theme preference. Defaults to false (Light Mode).
     */
    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Persists the selected theme preference and applies it immediately.
     */
    public static void setDarkModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        applyThemeMode(enabled);
    }

    /**
     * Resolves the appropriate DatePicker theme resource based on active theme mode.
     */
    public static int getDatePickerTheme(Context context) {
        return isDarkModeEnabled(context) ? R.style.ThemeOverlay_App_DatePicker_Dark : R.style.ThemeOverlay_App_DatePicker_Light;
    }

    /**
     * Resolves the appropriate TimePicker theme resource based on active theme mode.
     */
    public static int getTimePickerTheme(Context context) {
        return isDarkModeEnabled(context) ? R.style.ThemeOverlay_App_TimePicker_Dark : R.style.ThemeOverlay_App_TimePicker_Light;
    }
}
