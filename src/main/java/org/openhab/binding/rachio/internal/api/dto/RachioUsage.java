package org.openhab.binding.rachio.internal.api.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for Rachio usage data
 * 
 * @author dboyett-sketch
 */
@NonNullByDefault
public class RachioUsage {
    @SerializedName("startDate")
    public @Nullable String startDate;

    @SerializedName("endDate")
    public @Nullable String endDate;

    @SerializedName("duration")
    public @Nullable Double duration;

    @SerializedName("water")
    public @Nullable Double water;

    /**
     * Get start date safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     * COMPILATION ERROR: "Null type mismatch" at line 32
     * ROOT CAUSE: Method returns String (implied @NonNull) but ternary could return ""
     * SOLUTION: Add explicit @org.eclipse.jdt.annotation.NonNull annotation
     */
    public @org.eclipse.jdt.annotation.NonNull String getStartDate() {
        String date = this.startDate;
        return date != null ? date : "";
    }

    /**
     * Get end date safely
     * ===== CRITICAL PATCH: Added explicit @NonNull annotation =====
     * COMPILATION ERROR: "Null type mismatch" at line 40
     * ROOT CAUSE: Method returns String (implied @NonNull) but ternary could return ""
     * SOLUTION: Add explicit @org.eclipse.jdt.annotation.NonNull annotation
     */
    public @org.eclipse.jdt.annotation.NonNull String getEndDate() {
        String date = this.endDate;
        return date != null ? date : "";
    }

    /**
     * Get duration safely
     * ===== CRITICAL PATCH: Fixed null-safe auto-unboxing =====
     * WARNING: "Potential null pointer access: This expression of type java.lang.Double may be null"
     * SOLUTION: Local variable + explicit null check
     */
    public double getDuration() {
        Double dur = this.duration;
        return dur != null ? dur : 0.0;
    }

    /**
     * Get water amount safely
     * ===== CRITICAL PATCH: Fixed null-safe auto-unboxing =====
     * WARNING: "Potential null pointer access: This expression of type java.lang.Double may be null"
     * SOLUTION: Local variable + explicit null check
     */
    public double getWater() {
        Double w = this.water;
        return w != null ? w : 0.0;
    }

    /**
     * Check if usage data is valid (has at least one field set)
     * ===== CRITICAL PATCH: Added local variables for null safety =====
     */
    public boolean isValid() {
        String start = this.startDate;
        String end = this.endDate;
        Double dur = this.duration;
        Double w = this.water;

        return (start != null && !start.isEmpty()) || (end != null && !end.isEmpty()) || (dur != null) || (w != null);
    }

    /**
     * Get formatted usage string
     * ===== CRITICAL PATCH: Added local variables for null safety =====
     */
    public @org.eclipse.jdt.annotation.NonNull String getFormattedUsage() {
        if (!isValid()) {
            return "No usage data";
        }

        StringBuilder sb = new StringBuilder();
        String start = this.startDate;
        String end = this.endDate;
        Double dur = this.duration;
        Double w = this.water;

        if (start != null && end != null) {
            sb.append(start).append(" to ").append(end);
        } else if (start != null) {
            sb.append("Starting ").append(start);
        } else if (end != null) {
            sb.append("Until ").append(end);
        }

        if (dur != null) {
            if (sb.length() > 0)
                sb.append(" - ");
            sb.append("Duration: ").append(dur).append(" seconds");
        }

        if (w != null) {
            if (sb.length() > 0)
                sb.append(" - ");
            sb.append("Water: ").append(w).append(" gallons");
        }

        return sb.toString();
    }

    @Override
    public @org.eclipse.jdt.annotation.NonNull String toString() {
        return getFormattedUsage();
    }
}
