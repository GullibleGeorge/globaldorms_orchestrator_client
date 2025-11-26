package com.globaldorm.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for external API integration
 * Coordinates calls to third-party services: geocoding, routing, and weather
 * Implements service composition with robust error handling
 */
@Service
public class ExternalApiService {

    @Autowired
    private RestTemplate restTemplate; // HTTP client for API calls

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Format UK postcode to standard format with space
     * Handles various input formats: NG28PT -> NG2 8PT
     * 
     * @param postcode Input postcode in any format
     * @return Properly formatted UK postcode
     */
    private String formatPostcode(String postcode) {
        if (postcode == null || postcode.trim().isEmpty()) {
            return postcode;
        }

        // Remove spaces and convert to uppercase
        String cleaned = postcode.replaceAll("\\s+", "").toUpperCase();
        System.out.println("Cleaning postcode: '" + postcode + "' -> '" + cleaned + "'");

        // Apply UK postcode formatting rules
        if (cleaned.length() >= 5 && cleaned.length() <= 7 && !cleaned.contains(" ")) {
            if (cleaned.length() == 6) {
                // Pattern: AB123C -> AB12 3CD
                String firstPart = cleaned.substring(0, 3);
                String lastPart = cleaned.substring(3);
                String formatted = firstPart + " " + lastPart;
                System.out.println("Formatted 6-char postcode: '" + cleaned + "' -> '" + formatted + "'");
                return formatted;
            } else if (cleaned.length() == 7) {
                // Pattern: AB123CD -> AB12 3CD
                String firstPart = cleaned.substring(0, 4);
                String lastPart = cleaned.substring(4);
                String formatted = firstPart + " " + lastPart;
                System.out.println("Formatted 7-char postcode: '" + cleaned + "' -> '" + formatted + "'");
                return formatted;
            } else if (cleaned.length() == 5) {
                // Pattern: AB1CD -> AB1 CD
                String firstPart = cleaned.substring(0, 2);
                String lastPart = cleaned.substring(2);
                String formatted = firstPart + " " + lastPart;
                System.out.println("Formatted 5-char postcode: '" + cleaned + "' -> '" + formatted + "'");
                return formatted;
            }
        }

        // Return as uppercase if already formatted or unusual
        String result = postcode.toUpperCase().trim();
        System.out.println("Using postcode as-is: '" + postcode + "' -> '" + result + "'");
        return result;
    }

    /**
     * Convert UK postcode to geographical coordinates
     * Uses postcodes.io free API for geocoding service
     * 
     * @param postcode UK postcode to geocode
     * @return Map with lat/lng coordinates, null if failed
     */
    public Map<String, Object> getCoordinatesFromPostcode(String postcode) {
        try {
            String formattedPostcode = formatPostcode(postcode);
            String url = "https://api.postcodes.io/postcodes/" + formattedPostcode;

            System.out.println("Geocoding postcode: " + formattedPostcode + " (original: " + postcode + ")");
            System.out.println("Geocoding URL: " + url);

            // Make API call with timeout handling
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("Geocoding response: " + response);

            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);

                // Check API response status
                if (rootNode.has("status") && rootNode.get("status").asInt() == 200) {
                    JsonNode result = rootNode.get("result");
                    Map<String, Object> coordinates = new HashMap<>();
                    coordinates.put("lat", result.get("latitude").asDouble());
                    coordinates.put("lng", result.get("longitude").asDouble());
                    System.out.println("Successfully geocoded " + formattedPostcode + " to " +
                            coordinates.get("lat") + "," + coordinates.get("lng"));
                    return coordinates;
                } else {
                    System.out.println(
                            "Geocoding failed for " + formattedPostcode + ": status " + rootNode.get("status"));
                    if (rootNode.has("error")) {
                        System.out.println("Error details: " + rootNode.get("error"));
                    }
                }
            } else {
                System.out.println("No response from geocoding API");
            }

            return null;
        } catch (Exception e) {
            System.out.println("Exception geocoding postcode " + postcode + ": " + e.getClass().getSimpleName() + " - "
                    + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Calculate driving distance and duration between two postcodes
     * Uses OSRM (Open Source Routing Machine) for route calculation
     * 
     * @param roomPostcode   Starting location postcode
     * @param campusPostcode Destination location postcode
     * @return Map with distance (km/miles) and duration data, null if failed
     */
    public Map<String, Object> calculateDistance(String roomPostcode, String campusPostcode) {
        try {
            System.out.println("Calculating distance between: " + roomPostcode + " and " + campusPostcode);

            // First geocode both locations
            Map<String, Object> roomCoords = getCoordinatesFromPostcode(roomPostcode);
            Map<String, Object> campusCoords = getCoordinatesFromPostcode(campusPostcode);

            if (roomCoords == null) {
                System.out.println("Failed to get coordinates for room postcode: " + roomPostcode);
                return null;
            }

            if (campusCoords == null) {
                System.out.println("Failed to get coordinates for campus postcode: " + campusPostcode);
                return null;
            }

            // Build OSRM API URL for driving route
            String url = String.format(
                    "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f",
                    (Double) roomCoords.get("lng"), (Double) roomCoords.get("lat"),
                    (Double) campusCoords.get("lng"), (Double) campusCoords.get("lat"));

            System.out.println("Calling OSRM API: " + url);

            // Make routing API call
            String response = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response);

            // Process successful route response
            if ("Ok".equals(rootNode.get("code").asText()) && rootNode.get("routes").size() > 0) {
                JsonNode route = rootNode.get("routes").get(0);

                // Extract and convert distance/duration values
                double distanceMeters = route.get("distance").asDouble();
                double durationSeconds = route.get("duration").asDouble();

                double distanceKm = Math.round(distanceMeters / 1000 * 100.0) / 100.0;
                double distanceMiles = Math.round(distanceMeters / 1609.344 * 100.0) / 100.0;
                double durationMinutes = Math.round(durationSeconds / 60 * 10.0) / 10.0;

                int hours = (int) (durationMinutes / 60);
                int mins = (int) (durationMinutes % 60);
                String durationFormatted = hours > 0 ? hours + " hour" + (hours > 1 ? "s" : "") + " " + mins + " min"
                        : mins + " min";

                // Build response with all distance/time formats
                Map<String, Object> result = new HashMap<>();
                result.put("distance_km", distanceKm);
                result.put("distance_miles", distanceMiles);
                result.put("duration_minutes", durationMinutes);
                result.put("duration_formatted", durationFormatted);
                result.put("from", formatPostcode(roomPostcode));
                result.put("to", formatPostcode(campusPostcode));

                System.out.println("Distance calculation successful: " + distanceKm + "km (" +
                        distanceMiles + " miles), " + durationFormatted);
                return result;
            } else {
                System.out.println("OSRM API returned error: " + rootNode.get("code").asText());
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error calculating distance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get weather forecast for a location using postcode
     * Uses 7timer.info API for meteorological data
     * 
     * @param postcode UK postcode for weather location
     * @return Map with location and 3-day forecast data, null if failed
     */
    public Map<String, Object> getWeatherForecast(String postcode) {
        try {
            System.out.println("Getting weather forecast for: " + postcode);

            // Get coordinates for weather API call
            Map<String, Object> coords = getCoordinatesFromPostcode(postcode);
            if (coords == null) {
                System.out.println("Cannot get weather - failed to geocode postcode: " + postcode);
                return null;
            }

            // Build 7timer API URL with coordinates
            String url = String.format(
                    "https://www.7timer.info/bin/api.pl?lon=%f&lat=%f&product=civil&output=json",
                    (Double) coords.get("lng"), (Double) coords.get("lat"));

            System.out.println("Calling weather API: " + url);

            // Make weather API call
            String response = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(response);

            // Process weather data series
            if (rootNode.has("dataseries") && rootNode.get("dataseries").size() > 0) {
                List<Map<String, Object>> forecast = new ArrayList<>();
                JsonNode dataseries = rootNode.get("dataseries");

                // Extract up to 3 days of forecast
                for (int i = 0; i < Math.min(3, dataseries.size()); i++) {
                    JsonNode dayData = dataseries.get(i);

                    Map<String, Object> forecastDay = new HashMap<>();
                    forecastDay.put("date", dayData.has("date") ? dayData.get("date").asText() : "Day " + (i + 1));
                    forecastDay.put("weather", dayData.has("weather") ? dayData.get("weather").asText() : "Unknown");

                    // Handle temperature data (various API formats)
                    if (dayData.has("temp2m")) {
                        JsonNode tempData = dayData.get("temp2m");
                        if (tempData.isObject()) {
                            forecastDay.put("temp_max", tempData.has("max") ? tempData.get("max").asText() : "N/A");
                            forecastDay.put("temp_min", tempData.has("min") ? tempData.get("min").asText() : "N/A");
                        } else {
                            forecastDay.put("temp_max", tempData.asText());
                            forecastDay.put("temp_min", tempData.asText());
                        }
                    } else {
                        forecastDay.put("temp_max", "N/A");
                        forecastDay.put("temp_min", "N/A");
                    }

                    // Handle wind speed data
                    if (dayData.has("wind10m")) {
                        JsonNode windData = dayData.get("wind10m");
                        if (windData.isObject()) {
                            forecastDay.put("wind_speed",
                                    windData.has("speed") ? windData.get("speed").asText() : "N/A");
                        } else {
                            forecastDay.put("wind_speed", windData.asText());
                        }
                    } else {
                        forecastDay.put("wind_speed", "N/A");
                    }

                    forecast.add(forecastDay);
                }

                // Build weather response
                Map<String, Object> result = new HashMap<>();
                result.put("location", formatPostcode(postcode));
                result.put("forecast", forecast);

                System.out.println("Weather forecast successful for: " + formatPostcode(postcode));
                return result;
            } else {
                System.out.println("Weather API returned no data");
            }

            return null;
        } catch (Exception e) {
            System.out.println("Error getting weather forecast: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}