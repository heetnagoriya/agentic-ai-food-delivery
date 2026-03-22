package com.project.agent_brain_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends OTP verification emails via Gmail SMTP.
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:your-email@gmail.com}")
    private String fromEmail;

    @Autowired
    private OtpStore otpStore;

    /**
     * Generates a 6-digit OTP, stores it, and sends it to the given email.
     * @return the generated OTP (for testing/debugging; do NOT expose in response)
     */
    public void sendOtp(String toEmail) {
        String otp = otpStore.generateAndStore(toEmail);

        if (mailSender == null) {
            System.out.println("⚠️ Mail not configured — OTP for " + toEmail + " is: " + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🍽️ Your C.A.F.E. Verification Code");
        message.setText(
            "Hi there!\n\n" +
            "Your one-time verification code for C.A.F.E. is:\n\n" +
            "  " + otp + "\n\n" +
            "This code expires in 5 minutes. Do not share it with anyone.\n\n" +
            "If you didn't request this, you can safely ignore this email.\n\n" +
            "— The C.A.F.E. Team 🤖"
        );

        try {
            mailSender.send(message);
            System.out.println("✅ OTP email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Email sending failed. Please check your SMTP configuration.");
        }
    }

    /** Verifies whether the given OTP matches what was sent to the email. */
    public boolean verifyOtp(String email, String otp) {
        return otpStore.verify(email, otp);
    }
}
