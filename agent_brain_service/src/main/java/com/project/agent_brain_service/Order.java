package com.project.agent_brain_service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Order {
    public String orderId;
    public String item;
    public String restaurantName;
    public int deliveryTimeMins;
    public LocalDateTime orderTime;

    // Delivery Tracking Fields
    public String status;              // PLACED → PREPARING → OUT_FOR_DELIVERY → DELIVERED | CANCELLED
    public String deliveryPartnerName;
    public String deliveryPartnerVehicle;

    // Fake GPS coordinates (lat, lng)
    public double restaurantLat;
    public double restaurantLng;
    public double userLat;
    public double userLng;

    // 🆕 Payment & Cancellation Fields
    public String userId;
    public String restaurantId;
    public double paidAmount;           // How much was actually charged
    public double discountApplied;      // Coupon discount that was used
    public String couponCode;           // Coupon code used (if any)
    public boolean isCancelled = false;
    public LocalDateTime cancelledAt;
    public String cancellationReason;
    public double refundAmount;         // How much was refunded (if cancelled)

    // 🆕 Issue Reporting Fields
    public boolean hasIssue = false;
    public String issueType;            // WRONG_ITEM, MISSING_ITEM, COLD_FOOD, NEVER_DELIVERED, BAD_QUALITY
    public String issueResolution;      // FULL_REFUND, PARTIAL_REFUND, COUPON_COMPENSATION, REPLACEMENT
    public double issueRefundAmount;

    @DynamoDbPartitionKey
    public String getOrderId() {
        return orderId;
    }

    public Order(String item, String restaurantName, int deliveryTimeMins,
                 double restaurantLat, double restaurantLng,
                 double userLat, double userLng) {
        this.orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        this.item = item;
        this.restaurantName = restaurantName;
        this.deliveryTimeMins = deliveryTimeMins;
        this.orderTime = LocalDateTime.now();
        this.status = "PLACED";

        // Coordinates
        this.restaurantLat = restaurantLat;
        this.restaurantLng = restaurantLng;
        this.userLat = userLat;
        this.userLng = userLng;

        // Random delivery partner
        String[] partners = {"Rahul K.", "Priya S.", "Amit R.", "Deepa M.", "Karan J."};
        String[] vehicles = {"🏍️ Bajaj Pulsar", "🛵 Honda Activa", "🏍️ Hero Splendor", "🛵 TVS Jupiter"};
        this.deliveryPartnerName = partners[(int) (Math.random() * partners.length)];
        this.deliveryPartnerVehicle = vehicles[(int) (Math.random() * vehicles.length)];
    }

    // Simple constructor for backward compat
    public Order(String item) {
        this(item, "Unknown", 30, 22.5726, 72.9290, 22.5645, 72.9280);
    }

    /**
     * Auto-calculates the current delivery status based on elapsed time.
     * If cancelled, always returns CANCELLED.
     * Timeline: PLACED (0-20%) → PREPARING (20-50%) → OUT_FOR_DELIVERY (50-90%) → DELIVERED (90%+)
     */
    public String calculateCurrentStatus() {
        if (isCancelled) return "CANCELLED";

        long elapsedSeconds = ChronoUnit.SECONDS.between(orderTime, LocalDateTime.now());
        long totalSeconds = 30L; // Fast prototype progression
        double progress = (double) elapsedSeconds / totalSeconds;

        if (progress >= 0.9)      return "DELIVERED";
        else if (progress >= 0.5) return "OUT_FOR_DELIVERY";
        else if (progress >= 0.2) return "PREPARING";
        else                      return "PLACED";
    }

    /**
     * Returns the interpolated delivery partner position (fake GPS) based on progress.
     */
    public double[] getDeliveryPartnerPosition() {
        if (isCancelled) return new double[]{restaurantLat, restaurantLng};

        long elapsedSeconds = ChronoUnit.SECONDS.between(orderTime, LocalDateTime.now());
        long totalSeconds = 30L; // Fast prototype progression
        double progress = Math.min(1.0, (double) elapsedSeconds / totalSeconds);

        // Only start moving after 50% (OUT_FOR_DELIVERY phase)
        double moveProgress = progress < 0.5 ? 0.0 : (progress - 0.5) / 0.5;
        moveProgress = Math.min(1.0, moveProgress);

        double lat = restaurantLat + (userLat - restaurantLat) * moveProgress;
        double lng = restaurantLng + (userLng - restaurantLng) * moveProgress;
        return new double[]{lat, lng};
    }

    /**
     * Returns estimated minutes remaining.
     */
    public int getEstimatedMinutesRemaining() {
        if (isCancelled) return 0;
        long elapsedSeconds = ChronoUnit.SECONDS.between(orderTime, LocalDateTime.now());
        long totalSeconds = 30L; // Fast prototype progression
        long remaining = (totalSeconds - elapsedSeconds) / 60;
        return (int) Math.max(0, remaining);
    }
}