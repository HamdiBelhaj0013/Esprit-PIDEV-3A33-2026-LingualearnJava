package org.example.service;

import org.example.entity.User;
import org.example.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class EmailVerificationService {

    private static final Map<String, VerifyEntry> verifyStore = new HashMap<>();

    private static class VerifyEntry {
        final String        code;
        final LocalDateTime expiresAt;

        VerifyEntry(String code) {
            this.code      = code;
            this.expiresAt = LocalDateTime.now().plusMinutes(30);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    // ── Generate, store, and email a 6-digit code ─────────────────────────────

    public String generateAndSendCode(String email) {
        String code = String.format("%06d", new Random().nextInt(999999));
        verifyStore.put(email.toLowerCase(), new VerifyEntry(code));
        EmailService.sendVerificationCode(email, code);
        return code;
    }

    // ── Validate a code entered by the user ───────────────────────────────────

    public boolean verifyCode(String email, String enteredCode) {
        VerifyEntry entry = verifyStore.get(email.toLowerCase());
        if (entry == null) return false;
        if (entry.isExpired()) {
            verifyStore.remove(email.toLowerCase());
            return false;
        }
        return entry.code.equals(enteredCode);
    }

    // ── Persist verification and clear the in-memory code ────────────────────

    public void markVerified(String email) {
        Optional<User> userOpt = new UserService().findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setVerified(true);
            new UserRepository().save(user);
        }
        verifyStore.remove(email.toLowerCase());
    }
}
