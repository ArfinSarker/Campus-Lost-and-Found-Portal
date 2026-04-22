package com.sas.lostandfound;

import android.text.TextUtils;

/**
 * Dedicated utility for formatting Lost/Found report locations.
 * This is the single source of truth for location display.
 */
public class ReportLocationDisplay {

    /**
     * Formats the full location string based on predefined location, manual location,
     * and additional location details.
     *
     * @param location                  The predefined location from dropdown.
     * @param manualLocation           The manually entered location when "Other" is selected.
     * @param additionalLocationDetails Free text for extra details.
     * @return A formatted string for display.
     */
    public static String formatFullLocation(String location, String manualLocation, String additionalLocationDetails) {
        // Must handle null/empty safely
        if (TextUtils.isEmpty(location) && TextUtils.isEmpty(manualLocation) && TextUtils.isEmpty(additionalLocationDetails)) {
            return "Location not specified";
        }

        // Logic: IF location != "Other"
        if (location != null && !"Other".equalsIgnoreCase(location) && !location.isEmpty()) {
            if (!TextUtils.isEmpty(additionalLocationDetails)) {
                return location + " - " + additionalLocationDetails;
            } else {
                return location;
            }
        }

        // Logic: IF location == "Other" (or null/empty)
        if (location == null || "Other".equalsIgnoreCase(location) || location.isEmpty()) {
            if (!TextUtils.isEmpty(manualLocation) && !TextUtils.isEmpty(additionalLocationDetails)) {
                return manualLocation + " - " + additionalLocationDetails;
            } else if (!TextUtils.isEmpty(manualLocation)) {
                return manualLocation;
            } else if (!TextUtils.isEmpty(additionalLocationDetails)) {
                return additionalLocationDetails;
            }
        }

        // Must NEVER return broken/undefined values
        return "Location not specified";
    }
}
