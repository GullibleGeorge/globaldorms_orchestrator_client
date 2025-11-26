package com.globaldorm.orchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globaldorm.orchestrator.model.Room;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for room-related operations
 * Handles room data loading, searching, and filtering
 * Implements data persistence using JSON file storage
 */
@Service
public class RoomService {
    private List<Room> rooms;
    private final ObjectMapper objectMapper;

    /**
     * Constructor - initializes service and loads room data
     */
    public RoomService() {
        this.objectMapper = new ObjectMapper();
        this.rooms = loadRooms();
    }

    /**
     * Load room data from JSON file
     * 
     * @return List of Room objects, empty list if file not found or invalid
     */
    private List<Room> loadRooms() {
        try {
            File roomsFile = new File("rooms.json");
            if (!roomsFile.exists()) {
                System.out.println("Error: rooms.json file not found");
                return new ArrayList<>();
            }

            // Parse JSON and extract rooms array
            JsonNode rootNode = objectMapper.readTree(roomsFile);
            JsonNode roomsNode = rootNode.get("rooms");

            if (roomsNode != null && roomsNode.isArray()) {
                List<Room> loadedRooms = objectMapper.convertValue(roomsNode, new TypeReference<List<Room>>() {
                });
                System.out.println("Loaded " + loadedRooms.size() + " rooms from rooms.json");
                return loadedRooms;
            }

            return new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Error loading rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Search and filter rooms based on criteria
     * 
     * @param city      Filter by city name (case-insensitive)
     * @param maxPrice  Filter by maximum monthly price
     * @param furnished Filter by furnished status
     * @param language  Filter by spoken language availability
     * @return Filtered list of rooms
     */
    public List<Room> searchRooms(String city, String maxPrice, String furnished, String language) {
        List<Room> filteredRooms = new ArrayList<>(rooms);

        // Apply city filter
        if (StringUtils.hasText(city)) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getLocation().getCity().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
        }

        // Apply price filter
        if (StringUtils.hasText(maxPrice)) {
            try {
                double maxPriceValue = Double.parseDouble(maxPrice);
                filteredRooms = filteredRooms.stream()
                        .filter(room -> room.getPricePerMonthGbp() <= maxPriceValue)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                // Ignore invalid price format
            }
        }

        // Apply furnished filter
        if (StringUtils.hasText(furnished)) {
            boolean furnishedValue = "true".equalsIgnoreCase(furnished);
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getDetails().isFurnished() == furnishedValue)
                    .collect(Collectors.toList());
        }

        // Apply language filter
        if (StringUtils.hasText(language)) {
            filteredRooms = filteredRooms.stream()
                    .filter(room -> room.getSpokenLanguages().contains(language))
                    .collect(Collectors.toList());
        }

        return filteredRooms;
    }

    /**
     * Find specific room by ID
     * 
     * @param roomId The room ID to search for
     * @return Room object if found, null otherwise
     */
    public Room getRoomById(int roomId) {
        return rooms.stream()
                .filter(room -> room.getId() == roomId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get total number of available rooms
     * 
     * @return Total room count
     */
    public int getTotalRooms() {
        return rooms.size();
    }
}