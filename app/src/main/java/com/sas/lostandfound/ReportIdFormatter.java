package com.sas.lostandfound;

/**
 * Utility class to format and parse report IDs consistently across the app.
 */
public class ReportIdFormatter {
    /**
     * Formats a report display ID to always include the '#' prefix.
     * @param displayId The raw display ID (e.g., "L1", "F1", "N/A")
     * @return Formatted ID (e.g., "#L1", "#F1") or empty string if invalid.
     */
    public static String format(String displayId) {
        if (displayId == null || displayId.trim().isEmpty() || "N/A".equalsIgnoreCase(displayId)) {
            return "";
        }
        String trimmed = displayId.trim();
        if (trimmed.startsWith("#")) {
            return trimmed;
        }
        return "#" + trimmed;
    }

    /**
     * Strips the '#' prefix if present and returns the clean ID (e.g., "L1", "F1", "R1").
     */
    public static String getRawId(String displayId) {
        if (displayId == null) return "";
        String raw = displayId.trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1).trim();
        }
        return raw;
    }

    /**
     * Extracts the single letter prefix (L, F, R) from the display ID, regardless of '#' presence.
     */
    public static String getPrefix(String displayId) {
        String raw = getRawId(displayId);
        if (raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase();
    }
}
