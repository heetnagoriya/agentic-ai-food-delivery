package com.project.agent_brain_service;

/**
 * Represents a restaurant-specific coupon with conditions.
 * Supports both percentage-based and flat discount coupons.
 * 
 * Examples:
 *   - "60% off up to ₹120 on orders above ₹199"  → percentOff=60, maxDiscount=120, minOrder=199
 *   - "Flat ₹175 off on orders above ₹350"        → flatOff=175, minOrder=350
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@DynamoDbBean
public class Coupon {

    public String code;
    public double percentOff;    // e.g., 60.0 means 60%. Set to 0 for flat discounts.
    public double flatOff;       // e.g., 175.0 means ₹175 off. Set to 0 for percentage discounts.
    public double maxDiscount;   // Maximum discount cap for percentage coupons (in ₹)
    public double minOrder;      // Minimum order value required to use this coupon (in ₹)
    public String description;   // Human-readable description

    public Coupon(String code, double percentOff, double flatOff, double maxDiscount, double minOrder, String description) {
        this.code = code;
        this.percentOff = percentOff;
        this.flatOff = flatOff;
        this.maxDiscount = maxDiscount;
        this.minOrder = minOrder;
        this.description = description;
    }

    /**
     * Calculates the actual discount amount for a given order value.
     * Returns 0 if the order doesn't meet the minimum requirement.
     */
    public double calculateDiscount(double orderAmount) {
        if (orderAmount < minOrder) return 0;
        if (percentOff > 0) {
            double rawDiscount = orderAmount * percentOff / 100.0;
            return Math.min(rawDiscount, maxDiscount);
        }
        return flatOff;
    }
}
