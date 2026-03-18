package com.sas.lostandfound;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminRequest {
    private String universityId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String designation;
    private String verificationCode;
    private String password;
    private String userType;
    private String requestStatus;
    private String profileImageUrl;
    private long timestamp;
    private String created_at; // Changed to String for human-readable time and date

    public AdminRequest() {
    }

    private String getCurrentFormattedDate() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public AdminRequest(String universityId, String fullName, String email, String phoneNumber, String designation, String verificationCode, String password, String profileImageUrl) {
        this.universityId = universityId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.designation = designation;
        this.verificationCode = verificationCode;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.userType = "Admin";
        this.requestStatus = "pending";
        this.timestamp = System.currentTimeMillis();
        this.created_at = getCurrentFormattedDate();
    }

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}
