package com.sas.lostandfound;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String name;
    private String universityId;
    private String email;
    private String phone;
    private String department;
    private String batch;
    private String levelTerm;
    private String section;
    private String profileImageUrl;
    private List<String> profileImageUrls;
    private String gender;
    private long registeredAt; // timestamp

    // Required empty constructor for Firebase
    public User() {
        this.profileImageUrls = new ArrayList<>();
    }

    // Constructor with all fields
    public User(String userId, String name, String universityId, String email, String phone,
                String department, String batch, String levelTerm, String section, String profileImageUrl, String gender) {
        this.userId = userId;
        this.name = name;
        this.universityId = universityId;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.batch = batch;
        this.levelTerm = levelTerm;
        this.section = section;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.registeredAt = System.currentTimeMillis();
        this.profileImageUrls = new ArrayList<>();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            this.profileImageUrls.add(profileImageUrl);
        }
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUniversityId() { return universityId; }
    public void setUniversityId(String universityId) { this.universityId = universityId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

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

    public long getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(long registeredAt) { this.registeredAt = registeredAt; }
}
