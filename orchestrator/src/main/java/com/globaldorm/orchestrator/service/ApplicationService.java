package com.globaldorm.orchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaldorm.orchestrator.model.Application;
import com.globaldorm.orchestrator.model.Room;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for application management
 * Handles room application lifecycle: create, cancel, retrieve
 * Provides data persistence through JSON file storage
 */
@Service
public class ApplicationService {
    private List<Application> applications;
    private final ObjectMapper objectMapper;
    private static final String APPLICATIONS_FILE = "applications.json";

    /**
     * Constructor - initializes service and loads existing applications
     */
    public ApplicationService() {
        this.objectMapper = new ObjectMapper();
        this.applications = loadApplications();
    }

    /**
     * Load applications from JSON file
     * 
     * @return List of existing applications, empty list if file doesn't exist
     */
    private List<Application> loadApplications() {
        try {
            File applicationsFile = new File(APPLICATIONS_FILE);
            if (!applicationsFile.exists()) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(applicationsFile, new TypeReference<List<Application>>() {
            });
        } catch (IOException e) {
            System.out.println("Error loading applications: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Persist applications to JSON file
     * Uses pretty printing for readable JSON format
     */
    private void saveApplications() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(APPLICATIONS_FILE), applications);
        } catch (IOException e) {
            System.out.println("Error saving applications: " + e.getMessage());
        }
    }

    /**
     * Create new room application
     * Prevents duplicate applications for same user-room combination
     * 
     * @param roomId      ID of the room to apply for
     * @param userId      User identifier
     * @param userEmail   User email address
     * @param roomDetails Full room information for application history
     * @return ApplicationResult with success status and message
     */
    public ApplicationResult createApplication(int roomId, String userId, String userEmail, Room roomDetails) {
        // Check for existing active application
        boolean alreadyApplied = applications.stream()
                .anyMatch(app -> app.getRoomId() == roomId &&
                        app.getUserId().equals(userId) &&
                        ("pending".equals(app.getStatus()) || "accepted".equals(app.getStatus())));

        if (alreadyApplied) {
            return new ApplicationResult(false, "Already applied for this room", 0);
        }

        // Create and save new application
        int newId = applications.size() + 1;
        Application application = new Application(newId, roomId, userId, userEmail, roomDetails);

        applications.add(application);
        saveApplications();

        return new ApplicationResult(true, "Application submitted successfully", newId);
    }

    /**
     * Cancel existing application
     * Only allows cancellation by the original applicant
     * 
     * @param applicationId ID of application to cancel
     * @param userId        User requesting cancellation (must match original
     *                      applicant)
     * @return ApplicationResult with operation status
     */
    public ApplicationResult cancelApplication(int applicationId, String userId) {
        Application application = applications.stream()
                .filter(app -> app.getId() == applicationId && app.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (application == null) {
            return new ApplicationResult(false, "Application not found", 0);
        }

        // Business logic validation
        if ("cancelled".equals(application.getStatus())) {
            return new ApplicationResult(false, "Application already cancelled", 0);
        }

        if ("accepted".equals(application.getStatus())) {
            return new ApplicationResult(false, "Cannot cancel accepted application", 0);
        }

        // Update application status
        application.setStatus("cancelled");
        application.setCancelledDate(LocalDateTime.now().toString());
        saveApplications();

        return new ApplicationResult(true, "Application cancelled successfully", applicationId);
    }

    /**
     * Retrieve all applications for a specific user
     * Returns results sorted by application date (newest first)
     * 
     * @param userId User identifier
     * @return List of user's applications
     */
    public List<Application> getUserApplications(String userId) {
        return applications.stream()
                .filter(app -> app.getUserId().equals(userId))
                .sorted(Comparator.comparing(Application::getApplicationDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get total number of applications in system
     * 
     * @return Total application count
     */
    public int getTotalApplications() {
        return applications.size();
    }

    /**
     * Result wrapper class for application operations
     * Provides structured response with success status and messages
     */
    public static class ApplicationResult {
        private final boolean success;
        private final String message;
        private final int applicationId;

        public ApplicationResult(boolean success, String message, int applicationId) {
            this.success = success;
            this.message = message;
            this.applicationId = applicationId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getApplicationId() {
            return applicationId;
        }
    }
}