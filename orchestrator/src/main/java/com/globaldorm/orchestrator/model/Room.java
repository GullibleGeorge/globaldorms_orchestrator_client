package com.globaldorm.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Room data model representing accommodation listings
 * Uses Jackson annotations for JSON serialization/deserialization
 * Supports nested objects for location and room details
 */
public class Room {
    private int id;
    private String name;
    private Location location;
    private RoomDetails details;

    @JsonProperty("price_per_month_gbp")
    private double pricePerMonthGbp;

    @JsonProperty("availability_date")
    private String availabilityDate;

    @JsonProperty("spoken_languages")
    private List<String> spokenLanguages;

    // Default constructor required for Jackson
    public Room() {
    }

    // Standard getters and setters for JSON mapping
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public RoomDetails getDetails() {
        return details;
    }

    public void setDetails(RoomDetails details) {
        this.details = details;
    }

    public double getPricePerMonthGbp() {
        return pricePerMonthGbp;
    }

    public void setPricePerMonthGbp(double pricePerMonthGbp) {
        this.pricePerMonthGbp = pricePerMonthGbp;
    }

    public String getAvailabilityDate() {
        return availabilityDate;
    }

    public void setAvailabilityDate(String availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public List<String> getSpokenLanguages() {
        return spokenLanguages;
    }

    public void setSpokenLanguages(List<String> spokenLanguages) {
        this.spokenLanguages = spokenLanguages;
    }

    /**
     * Location nested class for room geographical information
     */
    public static class Location {
        private String city;
        private String county;
        private String postcode;

        public Location() {
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCounty() {
            return county;
        }

        public void setCounty(String county) {
            this.county = county;
        }

        public String getPostcode() {
            return postcode;
        }

        public void setPostcode(String postcode) {
            this.postcode = postcode;
        }
    }

    /**
     * RoomDetails nested class for accommodation specifications
     */
    public static class RoomDetails {
        private boolean furnished;
        private List<String> amenities;

        @JsonProperty("live_in_landlord")
        private boolean liveInLandlord;

        @JsonProperty("shared_with")
        private int sharedWith;

        @JsonProperty("bills_included")
        private boolean billsIncluded;

        @JsonProperty("bathroom_shared")
        private boolean bathroomShared;

        public RoomDetails() {
        }

        public boolean isFurnished() {
            return furnished;
        }

        public void setFurnished(boolean furnished) {
            this.furnished = furnished;
        }

        public List<String> getAmenities() {
            return amenities;
        }

        public void setAmenities(List<String> amenities) {
            this.amenities = amenities;
        }

        public boolean isLiveInLandlord() {
            return liveInLandlord;
        }

        public void setLiveInLandlord(boolean liveInLandlord) {
            this.liveInLandlord = liveInLandlord;
        }

        public int getSharedWith() {
            return sharedWith;
        }

        public void setSharedWith(int sharedWith) {
            this.sharedWith = sharedWith;
        }

        public boolean isBillsIncluded() {
            return billsIncluded;
        }

        public void setBillsIncluded(boolean billsIncluded) {
            this.billsIncluded = billsIncluded;
        }

        public boolean isBathroomShared() {
            return bathroomShared;
        }

        public void setBathroomShared(boolean bathroomShared) {
            this.bathroomShared = bathroomShared;
        }
    }
}