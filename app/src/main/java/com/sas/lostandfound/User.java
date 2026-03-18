package com.sas.lostandfound;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class User {
    private String userId; // Primary Key (University ID)
    private String authId; // Firebase Auth UID
    private String name;
    private String fullName;
    private String universityId;
    private String email;
    private String password;
    private String phone;
    private String phoneNumber;
    private String department;
    private String batch;
    private String levelTerm;
    private String section;
    private String profileImageUrl;
    private List<String> profileImageUrls;
    private String gender;
    private String userType;
    private String designation;
    private String role;
    private String requestStatus;
    private long registeredAt;
    private String created_at; // Changed to String for human-readable time and date
    private boolean isAdmin;

    public User() {
        this.profileImageUrls = new ArrayList<>();
    }

    private String getCurrentFormattedDate() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // Constructor for Student
    public User(String universityId, String authId, String name, String email, String password, String phone,
                String department, String batch, String levelTerm, String section, String profileImageUrl, String gender) {
        this.userId = universityId;
        this.universityId = universityId;
        this.authId = authId;
        this.name = name;
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.phoneNumber = phone;
        this.department = department;
        this.batch = batch;
        this.levelTerm = levelTerm;
        this.section = section;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.userType = "Student";
        this.registeredAt = System.currentTimeMillis();
        this.created_at = getCurrentFormattedDate();
        this.profileImageUrls = new ArrayList<>();
        this.isAdmin = false;
        this.requestStatus = "approved";
        this.role = "user";
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            this.profileImageUrls.add(profileImageUrl);
        }
    }

    // Constructor for Staff
    public User(String universityId, String authId, String name, String email, String password, String phone,
                String designation, String profileImageUrl, String gender, String userType) {
        this.userId = universityId;
        this.universityId = universityId;
        this.authId = authId;
        this.name = name;
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.phoneNumber = phone;
        this.designation = designation;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.userType = userType;
        this.registeredAt = System.currentTimeMillis();
        this.created_at = getCurrentFormattedDate();
        this.profileImageUrls = new ArrayList<>();
        this.isAdmin = "Admin".equalsIgnoreCase(userType);
        this.requestStatus = "approved";
        this.role = this.isAdmin ? "admin" : "user";
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            this.profileImageUrls.add(profileImageUrl);
        }
    }

    // Full constructor
    public User(String universityId, String authId, String name, String email, String password, String phone,
                String department, String batch, String levelTerm, String section, String profileImageUrl,
                String gender, String userType, String designation) {
        this.userId = universityId;
        this.universityId = universityId;
        this.authId = authId;
        this.name = name;
        this.fullName = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.phoneNumber = phone;
        this.department = department;
        this.batch = batch;
        this.levelTerm = levelTerm;
        this.section = section;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.userType = userType;
        this.designation = designation;
        this.registeredAt = System.currentTimeMillis();
        this.created_at = getCurrentFormattedDate();
        this.profileImageUrls = new ArrayList<>();
        this.isAdmin = "Admin".equalsIgnoreCase(userType);
        this.requestStatus = "approved";
        this.role = this.isAdmin ? "admin" : "user";
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            this.profileImageUrls.add(profileImageUrl);
        }
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAuthId() { return authId; }
    public void setAuthId(String authId) { this.authId = authId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.fullName = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; this.name = fullName; }
    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; this.phoneNumber = phone; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; this.phone = phoneNumber; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }
    public String getLevelTerm() { return levelTerm; }
    public void setLevelTerm(String levelTerm) { this.levelTerm = levelTerm; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public List<String> getProfileImageUrls() { return profileImageUrls; }
    public void setProfileImageUrls(List<String> profileImageUrls) { this.profileImageUrls = profileImageUrls; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}
