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
    private static Boolean cachedDarkMode = null;

    /**
     * Resolves the current theme setting from preferences and applies it.
     */
    public static void applyTheme(Context context) {
        if (context == null) return;
        boolean isDark = isDarkModeEnabled(context);
        applyThemeMode(isDark);
    }

    /**
     * Applies the given theme mode using AppCompatDelegate.
     */
    public static void applyThemeMode(boolean isDark) {
        int targetMode = isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    /**
     * Retrieves the persisted theme preference. Defaults to false (Light Mode).
     */
    public static boolean isDarkModeEnabled(Context context) {
        if (cachedDarkMode != null) {
            return cachedDarkMode;
        }
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cachedDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        return cachedDarkMode;
    }

    /**
     * Persists the selected theme preference and applies it immediately.
     */
    public static void setDarkModeEnabled(Context context, boolean enabled) {
        if (context == null) return;
        cachedDarkMode = enabled;
        int targetMode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() == targetMode) {
            return;
        }

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
