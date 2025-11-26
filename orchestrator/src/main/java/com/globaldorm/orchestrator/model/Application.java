package com.globaldorm.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Application data model representing room booking applications
 * Tracks application status and stores room details for history
 * Uses Jackson for JSON serialization with snake_case property mapping
 */
public class Application {
    private int id;

    @JsonProperty("room_id")
    private int roomId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_email")
    private String userEmail;

    private String status; // pending, accepted, rejected, cancelled

    @JsonProperty("application_date")
    private String applicationDate;

    @JsonProperty("cancelled_date")
    private String cancelledDate;

    @JsonProperty("room_details")
    private Room roomDetails; // Embedded room info for application history

    // Default constructor for Jackson deserialization
    public Application() {
    }

    /**
     * Constructor for creating new applications
     * Sets initial status to 'pending' and current timestamp
     */
    public Application(int id, int roomId, String userId, String userEmail, Room roomDetails) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.status = "pending";
        this.applicationDate = LocalDateTime.now().toString();
        this.roomDetails = roomDetails;
    }

    // Standard getters and setters for all fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(String applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(String cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public Room getRoomDetails() {
        return roomDetails;
    }

    public void setRoomDetails(Room roomDetails) {
        this.roomDetails = roomDetails;
    }
}