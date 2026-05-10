package com.sas.lostandfound;

import android.view.View;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Helper class to manage role-based visibility of profile information fields.
 */
public class ProfileRoleHelper {

    /**
     * Applies visibility rules based on the user type.
     *
     * @param userType       The type of the user (Student, Staff, Admin)
     * @param tilDesignation TextInputLayout for Designation
     * @param tilBatch       TextInputLayout for Batch
     * @param tilLevelTerm   TextInputLayout for Level & Term
     * @param tilDepartment  TextInputLayout for Department
     * @param tilSection     TextInputLayout for Section
     */
    public static void applyRoleVisibility(String userType,
                                          TextInputLayout tilDesignation,
                                          TextInputLayout tilBatch,
                                          TextInputLayout tilLevelTerm,
                                          TextInputLayout tilDepartment,
                                          TextInputLayout tilSection) {
        
        if (userType == null) return;

        boolean isStaffOrAdmin = "Staff".equalsIgnoreCase(userType) || "Admin".equalsIgnoreCase(userType);
        boolean isStudent = "Student".equalsIgnoreCase(userType);

        if (isStaffOrAdmin) {
            if (tilDesignation != null) tilDesignation.setVisibility(View.VISIBLE);
            if (tilBatch != null) tilBatch.setVisibility(View.GONE);
            if (tilLevelTerm != null) tilLevelTerm.setVisibility(View.GONE);
            if (tilDepartment != null) tilDepartment.setVisibility(View.VISIBLE);
            if (tilSection != null) tilSection.setVisibility(View.GONE);
        } else if (isStudent) {
            if (tilDesignation != null) tilDesignation.setVisibility(View.GONE);
            if (tilBatch != null) tilBatch.setVisibility(View.VISIBLE);
            if (tilLevelTerm != null) tilLevelTerm.setVisibility(View.VISIBLE);
            if (tilDepartment != null) tilDepartment.setVisibility(View.VISIBLE);
            if (tilSection != null) tilSection.setVisibility(View.VISIBLE);
        }
    }
}
