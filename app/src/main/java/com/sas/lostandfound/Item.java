package com.sas.lostandfound;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Item {
    @SerializedName("id")
    private String id;
    
    @SerializedName("display_id")
    private String displayId;
    
    @SerializedName("type")
    private String type; // "lost" or "found" (virtual field)
    
    @SerializedName("reporter_id")
    private String reporterId;
    
    @SerializedName("item_name")
    private String itemName;
    
    @SerializedName("category")
    private String category;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("manual_location")
    private String manualLocation;
    
    @SerializedName("additional_location_details")
    private String additionalLocationDetails;
    
    @SerializedName("date_occurred")
    private String dateOccurred; // Format: YYYY-MM-DD
    
    @SerializedName("time_occurred")
    private String timeOccurred;
    
    @SerializedName("image_urls")
    private List<String> imageUrls;

    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("status")
    private String status; // "active", "resolved", "deleted"
    
    @SerializedName("admin_status")
    private String adminStatus; // "Pending", "Claimed", etc.
    
    @SerializedName("claimed_by_id")
    private String claimedById;
    
    @SerializedName("proof_of_ownership_detail")
    private String proofOfOwnershipDetail;
    
    @SerializedName("hidden_identification_question")
    private String hiddenIdentificationQuestion;
    
    @SerializedName("item_handling_status")
    private String itemHandlingStatus;
    
    @SerializedName("authority_name")
    private String authorityName;
    
    @SerializedName("office_room_number")
    private String officeRoomNumber;
    
    @SerializedName("preferred_contact_method")
    private String preferredContactMethod;
    
    @SerializedName("is_edited")
    private boolean isEdited;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("deleted_by_user")
    private boolean deletedByUser;

    @SerializedName("user_id")
    private String authUserId;

    @SerializedName("contact_name")
    private String userName;

    @SerializedName("contact_email")
    private String userEmail;

    @SerializedName("contact_phone")
    private String userPhone;

    // Transient fields for backward compatibility in UI (should be fetched from profiles)
    private transient String userUniversityId;
    private transient String userDepartment;

    public Item() {
        this.imageUrls = new ArrayList<>();
    }

    public Item(String id, String name, String category, String description, String location, String date, String type, String reporterId) {
        this.id = id;
        this.itemName = name;
        this.category = category;
        this.description = description;
        this.location = location;
        this.dateOccurred = date;
        this.type = type;
        this.reporterId = reporterId;
        this.status = "active";
        this.adminStatus = "Pending";
        this.imageUrls = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters with compatibility mapping
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }

    public String getReportId() { return displayId; } // Compatibility
    public void setReportId(String reportId) { this.displayId = reportId; }

    public String getName() { return itemName; }
    public void setName(String name) { this.itemName = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getManualLocation() { return manualLocation; }
    public void setManualLocation(String manualLocation) { this.manualLocation = manualLocation; }

    public String getDate() { return dateOccurred; }
    public void setDate(String date) { this.dateOccurred = date; }

    public String getTime() { return timeOccurred; }
    public void setTime(String time) { this.timeOccurred = time; }

    public String getAdditionalLocationDetails() { return additionalLocationDetails; }
    public void setAdditionalLocationDetails(String details) { this.additionalLocationDetails = details; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getImageUrl() { 
        if (imageUrl != null) return imageUrl;
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null; 
    }
    public void setImageUrl(String url) { 
        this.imageUrl = url;
        if (imageUrls == null) imageUrls = new ArrayList<>();
        if (url != null && !imageUrls.contains(url)) imageUrls.add(0, url);
    }

    public String getStatus() { return type != null ? type : status; } // UI expects 'lost'/'found'
    public void setStatus(String status) { this.status = status; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return reporterId; }
    public void setUserId(String userId) { this.reporterId = userId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getAdminStatus() { return adminStatus; }
    public void setAdminStatus(String adminStatus) { this.adminStatus = adminStatus; }

    public String getClaimedById() { return claimedById; }
    public void setClaimedById(String claimedById) { this.claimedById = claimedById; }
    
    public String getClaimedByUserId() { return claimedById; } // Compatibility
    public void setClaimedByUserId(String id) { this.claimedById = id; }

    public String getProofOfOwnershipDetail() { return proofOfOwnershipDetail; }
    public void setProofOfOwnershipDetail(String detail) { this.proofOfOwnershipDetail = detail; }

    public String getHiddenIdentificationQuestion() { return hiddenIdentificationQuestion; }
    public void setHiddenIdentificationQuestion(String question) { this.hiddenIdentificationQuestion = question; }

    public String getItemHandlingStatus() { return itemHandlingStatus; }
    public void setItemHandlingStatus(String status) { this.itemHandlingStatus = status; }

    public String getAuthorityName() { return authorityName; }
    public void setAuthorityName(String name) { this.authorityName = name; }

    public String getOfficeRoomNumber() { return officeRoomNumber; }
    public void setOfficeRoomNumber(String num) { this.officeRoomNumber = num; }

    public String getPreferredContactMethod() { return preferredContactMethod; }
    public void setPreferredContactMethod(String method) { this.preferredContactMethod = method; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isDeletedByUser() { return deletedByUser; }
    public void setDeletedByUser(boolean deletedByUser) { this.deletedByUser = deletedByUser; }

    public String getAuthUserId() { return authUserId; }
    public void setAuthUserId(String authUserId) { this.authUserId = authUserId; }

    // Compatibility Getters for UI
    public String getUserName() { return userName; }
    public void setUserName(String name) { this.userName = name; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String email) { this.userEmail = email; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String phone) { this.userPhone = phone; }
    public String getUserUniversityId() { return userUniversityId; }
    public void setUserUniversityId(String id) { this.userUniversityId = id; }

    public String getUserDepartment() { return userDepartment; }
    public void setUserDepartment(String department) { this.userDepartment = department; }
}
