package com.project.agent_brain_service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class CouponService {

    // Coupon Code -> Discount Percentage (0.20 = 20%)
    private static final Map<String, Double> COUPONS = new HashMap<>();

    static {
        COUPONS.put("HUNGRY20", 0.20); // 20% Off
        COUPONS.put("WELCOME50", 0.50); // 50% Off
        COUPONS.put("FLAT100", 100.0); // Flat 100 Off (Handled differently in logic if needed)
    }

    public String getBestCoupon() {
        // In a real app, logic would be complex. For now, we return the standard one.
        return "HUNGRY20"; 
    }
    
    public double getDiscountPercentage(String code) {
        return COUPONS.getOrDefault(code, 0.0);
    }
}