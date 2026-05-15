package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;

public class AdminRequest {
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
    
    @SerializedName("designation")
    private String designation;
    
    @SerializedName("department")
    private String department;
    
    @SerializedName("profile_image_url")
    private String profileImageUrl;
    
    @SerializedName("status")
    private String requestStatus;
    
    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("verification_code")
    private String verificationCode;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("password")
    private String password;

    @SerializedName("user_type")
    private String userType;

    public AdminRequest() {
    }

    public AdminRequest(String universityId, String authId, String fullName, String email, String phoneNumber, String designation, String department, String verificationCode, String password, String profileImageUrl) {
        this.universityId = universityId;
        this.authId = authId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.designation = designation;
        this.department = department;
        this.verificationCode = verificationCode;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.userType = "Admin";
        this.requestStatus = "pending";
        this.displayName = fullName;
    }

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String id) { this.universityId = id; }

    public String getAuthId() { return authId; }
    public void setAuthId(String id) { this.authId = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String name) { 
        this.fullName = name; 
        this.displayName = name;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phone) { this.phoneNumber = phone; }

    public String getDesignation() { return designation; }
    public void setDesignation(String d) { this.designation = d; }

    public String getDepartment() { return department; }
    public void setDepartment(String dept) { this.department = dept; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String code) { this.verificationCode = code; }

    public String getPassword() { return password; }
    public void setPassword(String p) { this.password = p; }

    public String getUserType() { return userType != null ? userType : "Admin"; }
    public void setUserType(String type) { this.userType = type; }

    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String s) { this.requestStatus = s; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String url) { this.profileImageUrl = url; }

    public String getCreated_at() { return createdAt; }
    public void setCreated_at(String date) { this.createdAt = date; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
