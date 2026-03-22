package com.project.agent_brain_service.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory OTP store. Maps email -> {otp, expiryMs}.
 * OTPs expire after 5 minutes.
 */
@Component
public class OtpStore {

    private static final long OTP_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final Map<String, Entry> store = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    public String generateAndStore(String email) {
        // 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1_000_000));
        store.put(email.toLowerCase(), new Entry(otp, System.currentTimeMillis() + OTP_TTL_MS));
        return otp;
    }

    /** Returns true and removes the OTP if valid; false if wrong or expired. */
    public boolean verify(String email, String otp) {
        Entry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expiryMs) {
            store.remove(email.toLowerCase());
            return false;
        }
        if (entry.otp.equals(otp)) {
            store.remove(email.toLowerCase());
            return true;
        }
        return false;
    }

    private record Entry(String otp, long expiryMs) {}
}
