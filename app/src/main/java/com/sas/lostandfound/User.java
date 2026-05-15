package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class User {
    @SerializedName("university_id")
    private String universityId;
    
    @SerializedName("auth_id")
    private String authId;
    
    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;
    
    @SerializedName("phone_number")
    private String phoneNumber;
    
    @SerializedName("department")
    private String department;
    
    @SerializedName("batch")
    private String batch;
    
    @SerializedName("level_term")
    private String levelTerm;
    
    @SerializedName("profile_image_url")
    private String profileImageUrl;
    
    @SerializedName("user_type")
    private String userType;
    
    @SerializedName("designation")
    private String designation;
    
    @SerializedName("role")
    private String role;
    
    @SerializedName("gender")
    private String gender;
    
    @SerializedName("section")
    private String section;

    @SerializedName("request_status")
    private String requestStatus;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("password")
    private String password;

    // Derived or UI-specific fields (not in DB but kept for compatibility or temporary use)
    private transient String name;
    private transient String phone;
    private transient boolean isAdmin;

    public User() {
    }

    // Constructor for Student with gender and section
    public User(String universityId, String authId, String fullName, String email, String password, String phone,
                String department, String batch, String levelTerm, String gender, String profileImageUrl, String section) {
        this.universityId = universityId;
        this.authId = authId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phone;
        this.department = department;
        this.batch = batch;
        this.levelTerm = levelTerm;
        this.gender = gender;
        this.profileImageUrl = profileImageUrl;
        this.section = section;
        this.userType = "Student";
        this.role = "user";
        this.isAdmin = false;
        this.displayName = fullName;
    }

    // Constructor for Staff/Admin with gender and department
    public User(String universityId, String authId, String fullName, String email, String password, String phone,
                String designation, String department, String profileImageUrl, String gender, String userType) {
        this.universityId = universityId;
        this.authId = authId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phone;
        this.designation = designation;
        this.department = department;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.userType = userType;
        this.isAdmin = "Admin".equalsIgnoreCase(userType);
        this.role = this.isAdmin ? "admin" : "user";
        this.displayName = fullName;
    }

    // Getters and Setters with backward compatibility logic
    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }
    
    public String getUserId() { return universityId; } // Compatibility
    public void setUserId(String userId) { this.universityId = userId; }

    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { 
        this.fullName = fullName; 
        this.name = fullName;
        this.displayName = fullName;
    }

    public String getName() { 
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        if (name != null && !name.trim().isEmpty()) return name;
        return null;
    }
    public void setName(String name) { this.fullName = name; this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; this.phone = phoneNumber; }

    public String getPhone() { 
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) return phoneNumber;
        if (phone != null && !phone.trim().isEmpty()) return phone;
        return null;
    }
    public void setPhone(String phone) { this.phoneNumber = phone; this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getLevelTerm() { return levelTerm; }
    public void setLevelTerm(String levelTerm) { this.levelTerm = levelTerm; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { 
        this.userType = userType; 
        this.isAdmin = "Admin".equalsIgnoreCase(userType);
        this.role = this.isAdmin ? "admin" : "user";
    }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }

    public String getCreated_at() { return createdAt; }
    public void setCreated_at(String createdAt) { this.createdAt = createdAt; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isAdmin() { return "admin".equalsIgnoreCase(role) || isAdmin; }
    public void setAdmin(boolean admin) { 
        isAdmin = admin; 
        this.role = admin ? "admin" : "user";
    }
}
