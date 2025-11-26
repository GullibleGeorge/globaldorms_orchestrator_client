package com.globaldorm.orchestrator.controller;

import com.globaldorm.orchestrator.model.Room;
import com.globaldorm.orchestrator.model.Application;
import com.globaldorm.orchestrator.service.ApplicationService;
import com.globaldorm.orchestrator.service.ExternalApiService;
import com.globaldorm.orchestrator.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Global Dorm orchestration
 * Provides REST API endpoints for room search, applications, and external
 * services
 * Implements service coordination and HTTP request/response handling
 * Demonstrates SOA principles through service composition
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Enable CORS for web clients
public class OrchestratorController {

    // Service dependencies injected by Spring
    @Autowired
    private RoomService roomService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ExternalApiService externalApiService;

    /**
     * Search and filter rooms endpoint
     * Supports multiple optional query parameters for filtering
     * GET /api/rooms?city=X&max_price=Y&furnished=true&language=Z
     * 
     * @param city      Optional city filter (case-insensitive)
     * @param max_price Optional maximum price filter in GBP
     * @param furnished Optional furnished status filter (true/false)
     * @param language  Optional spoken language filter
     * @return ResponseEntity with JSON containing filtered rooms list and metadata
     */
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> searchRooms(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String max_price,
            @RequestParam(required = false) String furnished,
            @RequestParam(required = false) String language) {

        try {
            // Delegate to room service for business logic
            List<Room> rooms = roomService.searchRooms(city, max_price, furnished, language);

            // Build successful response with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("rooms", rooms);
            response.put("total", rooms.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Handle unexpected errors with generic response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get specific room details endpoint
     * Returns detailed information for a single room
     * GET /api/rooms/{roomId}
     * 
     * @param roomId Room identifier from URL path
     * @return ResponseEntity with room details or 404 if not found
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> getRoomDetails(@PathVariable int roomId) {
        try {
            Room room = roomService.getRoomById(roomId);

            Map<String, Object> response = new HashMap<>();
            if (room != null) {
                // Room found - return details
                response.put("success", true);
                response.put("room", room);
                return ResponseEntity.ok(response);
            } else {
                // Room not found - return 404
                response.put("success", false);
                response.put("message", "Room not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create room application endpoint
     * Accepts JSON payload with application details
     * POST /api/applications
     * Expected JSON body: {"room_id": 1, "user_id": "student123", "user_email":
     * "student@example.com"}
     * 
     * @param requestData JSON request body with application details
     * @return ResponseEntity with application result including application ID
     */
    @PostMapping("/applications")
    public ResponseEntity<Map<String, Object>> applyForRoom(@RequestBody Map<String, Object> requestData) {
        try {
            // Validate all required fields are present
            if (!requestData.containsKey("room_id") ||
                    !requestData.containsKey("user_id") ||
                    !requestData.containsKey("user_email")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Missing required fields: room_id, user_id, user_email");
                return ResponseEntity.badRequest().body(response);
            }

            // Extract and validate request data
            int roomId = (Integer) requestData.get("room_id");
            String userId = (String) requestData.get("user_id");
            String userEmail = (String) requestData.get("user_email");

            // Verify room exists before creating application
            Room room = roomService.getRoomById(roomId);
            if (room == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Room not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Process application through service layer
            ApplicationService.ApplicationResult result = applicationService.createApplication(roomId, userId,
                    userEmail, room);

            // Build response based on application result
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.isSuccess()) {
                response.put("application_id", result.getApplicationId());
            }

            // Return appropriate HTTP status code
            HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (ClassCastException e) {
            // Handle invalid data type in request
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid data format in request");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cancel application endpoint
     * Allows users to cancel their own applications
     * DELETE /api/applications/{applicationId}
     * Expected JSON body: {"user_id": "student123"}
     * 
     * @param applicationId Application identifier to cancel from URL path
     * @param requestData   JSON request body with user verification
     * @return ResponseEntity with cancellation result
     */
    @DeleteMapping("/applications/{applicationId}")
    public ResponseEntity<Map<String, Object>> cancelApplication(
            @PathVariable int applicationId,
            @RequestBody Map<String, Object> requestData) {

        try {
            // Validate user_id is provided for security
            if (!requestData.containsKey("user_id")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Missing user_id for verification");
                return ResponseEntity.badRequest().body(response);
            }

            String userId = (String) requestData.get("user_id");

            // Process cancellation through service layer
            ApplicationService.ApplicationResult result = applicationService.cancelApplication(applicationId, userId);

            // Build response with operation result
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            HttpStatus status = result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user applications history endpoint
     * Returns all applications for a specific user, sorted by date
     * GET /api/users/{userId}/applications
     * 
     * @param userId User identifier from URL path
     * @return ResponseEntity with user's complete application history
     */
    @GetMapping("/users/{userId}/applications")
    public ResponseEntity<Map<String, Object>> getUserApplications(@PathVariable String userId) {
        try {
            // Retrieve applications through service layer
            List<Application> applications = applicationService.getUserApplications(userId);

            // Build response with applications and metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applications", applications);
            response.put("total", applications.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Distance calculation endpoint
     * Calculates driving distance and time between two UK postcodes
     * Uses external OSRM routing service via ExternalApiService
     * GET /api/distance?room_postcode=NG2+8PT&campus_postcode=NG1+5DT
     * 
     * @param room_postcode   Starting location postcode (URL encoded)
     * @param campus_postcode Destination postcode (URL encoded)
     * @return ResponseEntity with distance in km/miles and duration
     */
    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> getDistance(
            @RequestParam String room_postcode,
            @RequestParam String campus_postcode) {

        try {
            // Delegate to external service for distance calculation
            Map<String, Object> distanceInfo = externalApiService.calculateDistance(room_postcode, campus_postcode);

            Map<String, Object> response = new HashMap<>();
            if (distanceInfo != null) {
                // Successful calculation - include all distance data
                response.put("success", true);
                response.put("distance", distanceInfo);
                return ResponseEntity.ok(response);
            } else {
                // Calculation failed - could be invalid postcodes or service unavailable
                response.put("success", false);
                response.put("message", "Could not calculate distance - check postcodes or try again later");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Weather forecast endpoint
     * Gets 3-day weather forecast for a UK postcode location
     * Uses external weather service via ExternalApiService
     * GET /api/weather?postcode=NG2+8PT
     * 
     * @param postcode Location postcode for weather forecast (URL encoded)
     * @return ResponseEntity with weather forecast data including temperature and
     *         conditions
     */
    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> getWeather(@RequestParam String postcode) {
        try {
            // Delegate to external service for weather data
            Map<String, Object> weatherInfo = externalApiService.getWeatherForecast(postcode);

            Map<String, Object> response = new HashMap<>();
            if (weatherInfo != null) {
                // Successful forecast retrieval
                response.put("success", true);
                response.put("weather", weatherInfo);
                return ResponseEntity.ok(response);
            } else {
                // Weather service failed - could be invalid postcode or service unavailable
                response.put("success", false);
                response.put("message", "Could not get weather forecast - check postcode or try again later");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test endpoint for debugging postcode geocoding
     * Useful for troubleshooting external API integration issues
     * GET /api/test/postcode?postcode=NG2+8PT
     * 
     * @param postcode Postcode to test geocoding functionality
     * @return ResponseEntity with detailed geocoding test results and debug
     *         information
     */
    @GetMapping("/test/postcode")
    public ResponseEntity<Map<String, Object>> testPostcode(@RequestParam String postcode) {
        try {
            System.out.println("=== Testing postcode geocoding: " + postcode + " ===");

            // Test geocoding functionality directly
            Map<String, Object> coords = externalApiService.getCoordinatesFromPostcode(postcode);

            // Build detailed test response
            Map<String, Object> response = new HashMap<>();
            response.put("input_postcode", postcode);
            response.put("coordinates", coords);
            response.put("success", coords != null);

            if (coords != null) {
                response.put("message", "Geocoding successful - coordinates retrieved");
                response.put("latitude", coords.get("lat"));
                response.put("longitude", coords.get("lng"));
            } else {
                response.put("message", "Geocoding failed - check Java console for detailed error logs");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Exception during testing: " + e.getMessage());
            errorResponse.put("input_postcode", postcode);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * System health check endpoint
     * Provides system status and key metrics for monitoring
     * Used by clients to verify service availability
     * GET /api/health
     * 
     * @return ResponseEntity with system health status, timestamp, and data counts
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("total_rooms", roomService.getTotalRooms());
            response.put("total_applications", applicationService.getTotalApplications());
            response.put("external_services", "OSRM, 7timer, postcodes.io");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Even health check can fail - return degraded status
            Map<String, Object> response = new HashMap<>();
            response.put("status", "degraded");
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}