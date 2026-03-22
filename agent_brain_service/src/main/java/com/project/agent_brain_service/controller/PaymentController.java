package com.project.agent_brain_service.controller;

import com.project.agent_brain_service.service.DynamoDbService;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Stripe Checkout integration — lets users top up their wallet with real (or test) card payments.
 * Uses Stripe's hosted Checkout page — no card details ever touch your server.
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class PaymentController {

    @Value("${stripe.api-key:sk_test_placeholder}")
    private String stripeApiKey;

    @Value("${stripe.success-url:http://localhost:5173/payment-success}")
    private String successUrl;

    @Value("${stripe.cancel-url:http://localhost:5173}")
    private String cancelUrl;

    @Autowired
    private DynamoDbService dbService;

    /**
     * POST /api/payments/create-checkout
     * Body: { "userId": "email@example.com", "amount": 500 }
     *
     * Creates a Stripe Checkout Session and returns the URL to redirect the user to.
     * Amount is in INR (₹). Minimum ₹50, maximum ₹10,000.
     */
    @PostMapping("/create-checkout")
    public Map<String, Object> createCheckout(@RequestBody Map<String, Object> body) {
        try {
            Stripe.apiKey = stripeApiKey;

            String userId = (String) body.get("userId");
            int amountRupees = ((Number) body.getOrDefault("amount", 500)).intValue();

            // Clamp amount to valid range
            amountRupees = Math.max(50, Math.min(10000, amountRupees));

            // Stripe uses smallest currency unit (paise for INR)
            long amountPaise = amountRupees * 100L;

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&userId=" + userId + "&amount=" + amountRupees)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("inr")
                                .setUnitAmount(amountPaise)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("C.A.F.E. Wallet Top-Up")
                                        .setDescription("Add ₹" + amountRupees + " to your food delivery wallet")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .putMetadata("userId", userId)
                .putMetadata("amount", String.valueOf(amountRupees))
                .build();

            Session session = Session.create(params);
            return Map.of("url", session.getUrl(), "sessionId", session.getId());

        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * POST /api/payments/verify
     * Body: { "sessionId": "...", "userId": "...", "amount": 500 }
     *
     * Called by the frontend after Stripe redirects back to the success URL.
     * Verifies the session is paid and credits the wallet.
     */
    @PostMapping("/verify")
    public Map<String, Object> verifyPayment(@RequestBody Map<String, Object> body) {
        try {
            Stripe.apiKey = stripeApiKey;

            String sessionId = (String) body.get("sessionId");
            String userId = (String) body.get("userId");
            int amount = ((Number) body.getOrDefault("amount", 0)).intValue();

            Session session = Session.retrieve(sessionId);

            if ("paid".equals(session.getPaymentStatus()) && userId != null && amount > 0) {
                var wallet = dbService.getWallet(userId);
                if (wallet == null) {
                    wallet = new com.project.agent_brain_service.UserWallet(userId, 0.0);
                }

                // Make idempotent: prevent double-charging on page reload or strict-mode double-fetches
                boolean alreadyProcessed = false;
                if (wallet.getTransactionHistory() != null) {
                    for (String tx : wallet.getTransactionHistory()) {
                        if (tx != null && tx.contains("Session: " + sessionId)) {
                            alreadyProcessed = true;
                            break;
                        }
                    }
                }

                if (alreadyProcessed) {
                    return Map.of("status", "success", "newBalance", wallet.getBalance(),
                            "message", "Payment already processed successfully.");
                }

                wallet.addFunds(amount, "Stripe Top-Up (Session: " + sessionId + ")");
                dbService.saveWallet(wallet);
                return Map.of("status", "success", "newBalance", wallet.getBalance(),
                        "message", "₹" + amount + " added to your wallet!");
            } else {
                return Map.of("status", "pending", "message", "Payment not yet confirmed");
            }
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
