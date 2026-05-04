package org.example.service;

import org.example.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class PasswordResetService {

    private static final Map<String, OtpEntry> otpStore = new HashMap<>();

    private static class OtpEntry {
        final String        otp;
        final LocalDateTime expiresAt;

        OtpEntry(String otp) {
            this.otp       = otp;
            this.expiresAt = LocalDateTime.now().plusMinutes(10);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public String generateAndSendOTP(String email) {
        Optional<User> userOpt = new UserService().findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("No account found with this email.");
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(email.toLowerCase(), new OtpEntry(otp));
        EmailService.sendOTP(email, otp);
        return otp;
    }

    public boolean verifyOTP(String email, String enteredOtp) {
        OtpEntry entry = otpStore.get(email.toLowerCase());
        if (entry == null) return false;
        if (entry.isExpired()) {
            otpStore.remove(email.toLowerCase());
            return false;
        }
        return entry.otp.equals(enteredOtp);
    }

    public void resetPassword(String email, String otp, String newPassword, String confirmPassword) {
        if (!verifyOTP(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired code.");
        }

        Optional<User> userOpt = new UserService().findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found.");
        }

        new UserService().adminResetPassword(userOpt.get(), newPassword);
        otpStore.remove(email.toLowerCase());
    }
}
