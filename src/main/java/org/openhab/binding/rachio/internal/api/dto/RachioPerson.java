package org.openhab.binding.rachio.internal.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * DTO representing a Rachio person (user account)
 * 
 * @author dboyett-sketch
 */
public class RachioPerson {

    @SerializedName("id")
    public String id;

    @SerializedName("username")
    public String username;

    @SerializedName("email")
    public String email;

    @SerializedName("fullName")
    public String fullName;

    @SerializedName("createdDate")
    public Instant createdDate;

    @SerializedName("updatedDate")
    public Instant updatedDate;

    @SerializedName("devices")
    public List<RachioDevice> devices;

    @SerializedName("subscription")
    public Subscription subscription;

    @SerializedName("features")
    public Features features;

    @SerializedName("address")
    public Address address;

    /**
     * Default constructor for Gson
     */
    public RachioPerson() {
        this.devices = new ArrayList<>();
    }

    /**
     * Constructor with basic fields
     */
    public RachioPerson(String id, String username, String email) {
        this();
        this.id = id;
        this.username = username;
        this.email = email;
    }

    /**
     * Get person ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Get display name (full name or username)
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        return username != null ? username : email;
    }

    /**
     * Get created date
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Get updated date
     */
    public Instant getUpdatedDate() {
        return updatedDate;
    }

    /**
     * Get list of devices
     */
    public List<RachioDevice> getDevices() {
        return devices;
    }

    /**
     * Get number of devices
     */
    public int getDeviceCount() {
        return devices != null ? devices.size() : 0;
    }

    /**
     * Check if person has any devices
     */
    public boolean hasDevices() {
        return getDeviceCount() > 0;
    }

    /**
     * Get device by ID
     */
    public RachioDevice getDeviceById(String deviceId) {
        if (deviceId == null || devices == null) {
            return null;
        }

        for (RachioDevice device : devices) {
            if (deviceId.equals(device.id)) {
                return device;
            }
        }
        return null;
    }

    /**
     * Get subscription information
     */
    public Subscription getSubscription() {
        return subscription;
    }

    /**
     * Check if person has active subscription
     */
    public boolean hasActiveSubscription() {
        return subscription != null && subscription.isActive();
    }

    /**
     * Check if person has premium features
     */
    public boolean hasPremiumFeatures() {
        return subscription != null && subscription.isPremium();
    }

    /**
     * Get features
     */
    public Features getFeatures() {
        return features;
    }

    /**
     * Check if a specific feature is enabled
     */
    public boolean hasFeature(String featureName) {
        return features != null && features.isEnabled(featureName);
    }

    /**
     * Get address
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Check if address is set
     */
    public boolean hasAddress() {
        return address != null && address.isValid();
    }

    /**
     * Get person age in days
     */
    public long getAgeInDays() {
        if (createdDate == null) {
            return 0;
        }

        Instant now = Instant.now();
        long seconds = now.getEpochSecond() - createdDate.getEpochSecond();
        return seconds / (24 * 60 * 60);
    }

    /**
     * Validate person data
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && username != null && !username.isEmpty() && email != null
                && !email.isEmpty();
    }

    /**
     * Create summary string for logging
     */
    public String toSummaryString() {
        return String.format("Person[id=%s, name=%s, devices=%d, subscription=%s]", id, getDisplayName(),
                getDeviceCount(), subscription != null ? subscription.status : "none");
    }

    @Override
    public String toString() {
        return "RachioPerson{" + "id='" + id + '\'' + ", username='" + username + '\'' + ", email='" + email + '\''
                + ", fullName='" + fullName + '\'' + ", deviceCount=" + getDeviceCount() + ", subscription="
                + (subscription != null ? subscription.status : "none") + '}';
    }

    /**
     * Subscription information
     */
    public static class Subscription {

        @SerializedName("status")
        public String status;

        @SerializedName("type")
        public String type;

        @SerializedName("plan")
        public String plan;

        @SerializedName("expirationDate")
        public Instant expirationDate;

        @SerializedName("autoRenew")
        public Boolean autoRenew;

        @SerializedName("trial")
        public Boolean trial;

        /**
         * Check if subscription is active
         */
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status) || "TRIAL".equalsIgnoreCase(status);
        }

        /**
         * Check if subscription is expired
         */
        public boolean isExpired() {
            return "EXPIRED".equalsIgnoreCase(status);
        }

        /**
         * Check if subscription is cancelled
         */
        public boolean isCancelled() {
            return "CANCELLED".equalsIgnoreCase(status);
        }

        /**
         * Check if this is a premium subscription
         */
        public boolean isPremium() {
            return "PREMIUM".equalsIgnoreCase(plan) || "PRO".equalsIgnoreCase(plan);
        }

        /**
         * Check if this is a free/trial subscription
         */
        public boolean isFree() {
            return "FREE".equalsIgnoreCase(plan) || "TRIAL".equalsIgnoreCase(plan) || trial != null && trial;
        }

        /**
         * Check if subscription auto-renews
         */
        public boolean isAutoRenew() {
            return autoRenew != null && autoRenew;
        }

        /**
         * Get days until expiration
         */
        public long getDaysUntilExpiration() {
            if (expirationDate == null) {
                return Long.MAX_VALUE;
            }

            Instant now = Instant.now();
            if (now.isAfter(expirationDate)) {
                return 0;
            }

            long seconds = expirationDate.getEpochSecond() - now.getEpochSecond();
            return seconds / (24 * 60 * 60);
        }

        /**
         * Check if subscription expires soon (within 30 days)
         */
        public boolean expiresSoon() {
            return getDaysUntilExpiration() <= 30;
        }

        @Override
        public String toString() {
            return "Subscription{" + "status='" + status + '\'' + ", type='" + type + '\'' + ", plan='" + plan + '\''
                    + ", expirationDate=" + expirationDate + ", autoRenew=" + autoRenew + ", trial=" + trial + '}';
        }
    }

    /**
     * Feature flags
     */
    public static class Features {

        @SerializedName("flexSchedules")
        public Boolean flexSchedules;

        @SerializedName("weatherIntelligence")
        public Boolean weatherIntelligence;

        @SerializedName("cycleSoak")
        public Boolean cycleSoak;

        @SerializedName("advancedScheduling")
        public Boolean advancedScheduling;

        @SerializedName("waterUsage")
        public Boolean waterUsage;

        @SerializedName("forecast")
        public Boolean forecast;

        @SerializedName("historicalWeather")
        public Boolean historicalWeather;

        @SerializedName("customNozzles")
        public Boolean customNozzles;

        @SerializedName("customSoils")
        public Boolean customSoils;

        @SerializedName("customCrops")
        public Boolean customCrops;

        @SerializedName("customSlopes")
        public Boolean customSlopes;

        @SerializedName("customShades")
        public Boolean customShades;

        /**
         * Check if a specific feature is enabled
         */
        public boolean isEnabled(String featureName) {
            if (featureName == null) {
                return false;
            }

            switch (featureName.toLowerCase()) {
                case "flexschedules":
                case "flex_schedules":
                    return flexSchedules != null && flexSchedules;

                case "weatherintelligence":
                case "weather_intelligence":
                    return weatherIntelligence != null && weatherIntelligence;

                case "cyclesoak":
                case "cycle_soak":
                    return cycleSoak != null && cycleSoak;

                case "advancedscheduling":
                case "advanced_scheduling":
                    return advancedScheduling != null && advancedScheduling;

                case "waterusage":
                case "water_usage":
                    return waterUsage != null && waterUsage;

                case "forecast":
                    return forecast != null && forecast;

                case "historicalweather":
                case "historical_weather":
                    return historicalWeather != null && historicalWeather;

                case "customnozzles":
                case "custom_nozzles":
                    return customNozzles != null && customNozzles;

                case "customsoils":
                case "custom_soils":
                    return customSoils != null && customSoils;

                case "customcrops":
                case "custom_crops":
                    return customCrops != null && customCrops;

                case "customslopes":
                case "custom_slopes":
                    return customSlopes != null && customSlopes;

                case "customshades":
                case "custom_shades":
                    return customShades != null && customShades;

                default:
                    return false;
            }
        }

        /**
         * Check if smart watering features are available
         */
        public boolean hasSmartWatering() {
            return isEnabled("weatherIntelligence") || isEnabled("flexSchedules") || isEnabled("cycleSoak");
        }

        /**
         * Check if custom zone configuration is available
         */
        public boolean hasCustomZoneConfiguration() {
            return isEnabled("customNozzles") || isEnabled("customSoils") || isEnabled("customCrops")
                    || isEnabled("customSlopes") || isEnabled("customShades");
        }

        /**
         * Check if water usage tracking is available
         */
        public boolean hasWaterUsageTracking() {
            return isEnabled("waterUsage");
        }

        /**
         * Check if weather features are available
         */
        public boolean hasWeatherFeatures() {
            return isEnabled("forecast") || isEnabled("historicalWeather") || isEnabled("weatherIntelligence");
        }

        @Override
        public String toString() {
            return "Features{" + "flexSchedules=" + flexSchedules + ", weatherIntelligence=" + weatherIntelligence
                    + ", cycleSoak=" + cycleSoak + ", waterUsage=" + waterUsage + '}';
        }
    }

    /**
     * Address information
     */
    public static class Address {

        @SerializedName("street")
        public String street;

        @SerializedName("city")
        public String city;

        @SerializedName("state")
        public String state;

        @SerializedName("zip")
        public String zip;

        @SerializedName("country")
        public String country;

        @SerializedName("latitude")
        public Double latitude;

        @SerializedName("longitude")
        public Double longitude;

        @SerializedName("timeZone")
        public String timeZone;

        /**
         * Get full address string
         */
        public String getFullAddress() {
            StringBuilder sb = new StringBuilder();

            if (street != null && !street.isEmpty()) {
                sb.append(street).append(", ");
            }

            if (city != null && !city.isEmpty()) {
                sb.append(city).append(", ");
            }

            if (state != null && !state.isEmpty()) {
                sb.append(state).append(" ");
            }

            if (zip != null && !zip.isEmpty()) {
                sb.append(zip);
            }

            if (country != null && !country.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(country);
            }

            return sb.toString().trim();
        }

        /**
         * Check if address has coordinates
         */
        public boolean hasCoordinates() {
            return latitude != null && longitude != null;
        }

        /**
         * Get coordinates as string
         */
        public String getCoordinatesString() {
            if (hasCoordinates()) {
                return String.format("%.6f, %.6f", latitude, longitude);
            }
            return null;
        }

        /**
         * Check if address is valid
         */
        public boolean isValid() {
            return (street != null && !street.isEmpty()) || (city != null && !city.isEmpty())
                    || (zip != null && !zip.isEmpty());
        }

        /**
         * Check if timezone is set
         */
        public boolean hasTimeZone() {
            return timeZone != null && !timeZone.isEmpty();
        }

        @Override
        public String toString() {
            return "Address{" + "street='" + street + '\'' + ", city='" + city + '\'' + ", state='" + state + '\''
                    + ", zip='" + zip + '\'' + ", country='" + country + '\'' + ", coordinates="
                    + getCoordinatesString() + '}';
        }
    }
}
