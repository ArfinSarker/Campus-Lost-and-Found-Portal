package com.sas.lostandfound;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Item {
    private String id;
    private String name;
    private String category;
    private String description;
    private String location;
    private String date;
    private String time;
    private String additionalLocationDetails;
    private String imageUrl; // Kept for backward compatibility
    private List<String> imageUrls;
    private String status; // "lost" or "found"
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userUniversityId;
    private String userDepartment;
    
    // Lost Item Specific
    private String proofOfOwnershipUrl;
    private List<String> proofOfOwnershipUrls;
    private String proofOfOwnershipDetail; // Textual proof
    private String confidentialIdentificationDetail; // Hidden field
    
    // Found Item Specific
    private String itemHandlingStatus; 
    private String authorityName;
    private String officeRoomNumber;
    private String hiddenIdentificationQuestion; // Hidden field
    private String verificationMethod; // Admin or Direct
    private boolean isBlurred;

    // Common
    private String preferredContactMethod;
    private String adminStatus; // Pending, Matched, Returned, etc.
    private long timestamp;

    public Item() {
        this.imageUrls = new ArrayList<>();
        this.proofOfOwnershipUrls = new ArrayList<>();
    }

    public Item(String id, String name, String category, String description, String location, String date, String status, String userId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.date = date;
        this.status = status;
        this.userId = userId;
        this.adminStatus = "Pending";
        this.timestamp = System.currentTimeMillis();
        this.imageUrls = new ArrayList<>();
        this.proofOfOwnershipUrls = new ArrayList<>();
    }

    /**
     * Constructor for sample data or minimal item representation.
     */
    public Item(String id, String name, String location, String date, String status, String category) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.date = date;
        this.status = status;
        this.category = category;
        this.adminStatus = "Pending";
        this.timestamp = System.currentTimeMillis();
        this.imageUrls = new ArrayList<>();
        this.proofOfOwnershipUrls = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getAdditionalLocationDetails() { return additionalLocationDetails; }
    public void setAdditionalLocationDetails(String additionalLocationDetails) { this.additionalLocationDetails = additionalLocationDetails; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserUniversityId() { return userUniversityId; }
    public void setUserUniversityId(String userUniversityId) { this.userUniversityId = userUniversityId; }
    public String getUserDepartment() { return userDepartment; }
    public void setUserDepartment(String userDepartment) { this.userDepartment = userDepartment; }
    public String getProofOfOwnershipUrl() { return proofOfOwnershipUrl; }
    public void setProofOfOwnershipUrl(String proofOfOwnershipUrl) { this.proofOfOwnershipUrl = proofOfOwnershipUrl; }
    public List<String> getProofOfOwnershipUrls() { return proofOfOwnershipUrls; }
    public void setProofOfOwnershipUrls(List<String> proofOfOwnershipUrls) { this.proofOfOwnershipUrls = proofOfOwnershipUrls; }
    public String getProofOfOwnershipDetail() { return proofOfOwnershipDetail; }
    public void setProofOfOwnershipDetail(String proofOfOwnershipDetail) { this.proofOfOwnershipDetail = proofOfOwnershipDetail; }
    public String getConfidentialIdentificationDetail() { return confidentialIdentificationDetail; }
    public void setConfidentialIdentificationDetail(String confidentialIdentificationDetail) { this.confidentialIdentificationDetail = confidentialIdentificationDetail; }
    public String getItemHandlingStatus() { return itemHandlingStatus; }
    public void setItemHandlingStatus(String itemHandlingStatus) { this.itemHandlingStatus = itemHandlingStatus; }
    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String authorityName) { this.authorityName = authorityName; }
    public String getOfficeRoomNumber() { return officeRoomNumber; }
    public void setOfficeRoomNumber(String officeRoomNumber) { this.officeRoomNumber = officeRoomNumber; }
    public String getHiddenIdentificationQuestion() { return hiddenIdentificationQuestion; }
    public void setHiddenIdentificationQuestion(String hiddenIdentificationQuestion) { this.hiddenIdentificationQuestion = hiddenIdentificationQuestion; }
    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
    public boolean isBlurred() { return isBlurred; }
    public void setBlurred(boolean blurred) { isBlurred = blurred; }
    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }
    public String getAdminStatus() { return adminStatus; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
