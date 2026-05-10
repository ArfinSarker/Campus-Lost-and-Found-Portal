package com.sas.lostandfound;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * Reusable utility class for input validation across the application.
 */
public class ValidationUtils {

    /**
     * Validates if the email follows a standard format.
     */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validates if the phone number consists only of digits and is within the 10-14 digits range.
     */
    public static boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) return false;
        // Check length 10-14
        if (phone.length() < 11 || phone.length() > 11) return false;
        // Check if all are digits
        return phone.matches("\\d+");
    }

    /**
     * Validates if the password is at least 8 characters long and contains at least 
     * one uppercase letter, one lowercase letter, and one digit.
     */
    public static boolean isValidPassword(String password) {
        if (TextUtils.isEmpty(password) || password.length() < 8) return false;
        
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasLowercase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        
        return hasUppercase && hasLowercase && hasDigit;
    }
    
    /**
     * Returns the requirements for a valid password.
     */
    public static String getPasswordRequirements() {
        return "Password must be at least 8 characters and include uppercase, lowercase, and a number.";
    }
}
