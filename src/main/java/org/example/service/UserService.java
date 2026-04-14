package org.example.service;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.validation.ValidationService;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserService implements IUserService {

    private static final int BCRYPT_ROUNDS = 12;

    private final UserRepository    userRepository;
    private final ValidationService validation;

    public UserService() {
        this.userRepository = new UserRepository();
        this.validation     = new ValidationService();
    }

    // ── Password ──────────────────────────────────────────────────────────────

    public String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public boolean verifyPassword(String plain, String hash) {
        if (hash != null && hash.startsWith("$2y$")) {
            hash = "$2a$" + hash.substring(4);
        }
        return BCrypt.checkpw(plain, hash);
    }

    // ── REGISTRATION ─────────────────────────────────────────────────────────

    /**
     * Public-facing user self-registration.
     * Validates inputs, hashes the password, then delegates to
     * {@link org.example.repository.UserRepository#register(User)}
     * which enforces the unique-email constraint at the DB level.
     */
    @Override
    public void registerUser(String firstName, String lastName, String email, String password) {
        ValidationService.requireNonBlank(firstName, "First name");
        ValidationService.requireNonBlank(lastName,  "Last name");
        ValidationService.requireMinLength(firstName, "First name", 2);
        ValidationService.requireMinLength(lastName,  "Last name",  2);
        ValidationService.requireValidEmail(email);
        ValidationService.requireMinLength(password, "Password", 6);

        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS)));
        user.setRoles(List.of("ROLE_USER"));
        user.setStatus("active");
        user.setSubscriptionPlan("FREE");
        user.setVerified(false);

        userRepository.register(user);
    }

    // ── IUserService aliases ──────────────────────────────────────────────────

    /** Alias for {@link #authenticate(String, String)} — satisfies {@link IUserService}. */
    @Override
    public Optional<User> login(String email, String password) {
        return authenticate(email, password);
    }

    /** Alias for {@link #findAll()} — satisfies {@link IUserService}. */
    @Override
    public List<User> getAllUsers() {
        return findAll();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public User createUser(String email, String plainPassword, String confirmPassword,
                           String firstName, String lastName, List<String> roles) {

        ValidationService.requireValidEmail(email);
        ValidationService.requireNonBlank(firstName, "First name");
        ValidationService.requireNonBlank(lastName,  "Last name");
        ValidationService.requireMinLength(plainPassword, "Password", 6);
        ValidationService.requirePasswordsMatch(plainPassword, confirmPassword);

        userRepository.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("An account with this email already exists.");
        });

        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPassword(hashPassword(plainPassword));
        user.setRoles(roles);
        user.setStatus("active");
        user.setSubscriptionPlan("FREE");
        user.setVerified(false);

        validation.validateOrThrow(user);

        LearningStats stats = new LearningStats();
        try {
            userRepository.saveWithStats(user, stats);
        } catch (Exception e) {
            throw new RuntimeException("Could not create user: " + e.getMessage(), e);
        }
        return user;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    public Optional<User> findById(Long id)         { return userRepository.findById(id); }
    public Optional<User> findByEmail(String email) { return userRepository.findByEmail(email); }
    public List<User>     findAll()                 { return userRepository.findAll(); }

    public List<User> findAdvanced(String search, String status, String plan,
                                   int page, int pageSize) {
        return userRepository.findAdvanced(search, status, null, plan, null, page, pageSize);
    }

    public List<User> findAdvanced(String search, String status, String role,
                                   String plan, String sort, int page, int pageSize) {
        return userRepository.findAdvanced(search, status, role, plan, sort, page, pageSize);
    }

    public long countAdvanced(String search, String status, String role, String plan) {
        return userRepository.countAdvanced(search, status, role, plan);
    }

    public List<User> search(String term) { return userRepository.search(term); }

    public long countAll()              { return userRepository.countAll(); }
    public long countByStatus(String s) { return userRepository.countByStatus(s); }
    public long countPremium()          { return userRepository.countPremium(); }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void updateName(User user, String firstName, String lastName) {
        ValidationService.requireNonBlank(firstName, "First name");
        ValidationService.requireNonBlank(lastName,  "Last name");
        ValidationService.requireMinLength(firstName, "First name", 2);
        ValidationService.requireMinLength(lastName,  "Last name",  2);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        saveAndFlush(user);
    }

    public void updateProfile(User user, String firstName, String lastName) {
        updateName(user, firstName, lastName);
    }

    public void adminUpdateUser(User user, String firstName, String lastName,
                                String newPassword, String confirmPassword) {
        ValidationService.requireNonBlank(firstName, "First name");
        ValidationService.requireNonBlank(lastName,  "Last name");
        ValidationService.requireMinLength(firstName, "First name", 2);
        ValidationService.requireMinLength(lastName,  "Last name",  2);

        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());

        if (newPassword != null && !newPassword.isBlank()) {
            ValidationService.requireMinLength(newPassword, "Password", 6);
            ValidationService.requirePasswordsMatch(newPassword, confirmPassword);
            user.setPassword(hashPassword(newPassword));
        }

        saveAndFlush(user);
    }

    public void activate(User user)  { activateUser(user); }
    public void suspend(User user)   { suspendUser(user); }
    public void delete(User user)    { deleteUser(user); }

    public void activateUser(User user) {
        user.setStatus("active");
        saveAndFlush(user);
    }

    public void suspendUser(User user) {
        user.setStatus("suspended");
        saveAndFlush(user);
    }

    public void deleteUser(User user) {
        try {
            userRepository.delete(user);
        } catch (Exception e) {
            throw new RuntimeException("Could not delete user: " + e.getMessage(), e);
        }
    }

    public void grantPremium(User user, String plan, LocalDateTime expiry) {
        upgradeToPremium(user, plan, expiry);
    }

    public void revokePremium(User user) {
        downgradeToFree(user);
    }

    public void upgradeToPremium(User user, String plan, LocalDateTime expiry) {
        ValidationService.requireValidPlan(plan);
        user.setSubscriptionExpiry(expiry);
        user.setSubscriptionPlan(plan);
        user.setLastPaymentStatus("success");
        saveAndFlush(user);
    }

    public void downgradeToFree(User user) {
        user.setSubscriptionPlan("FREE");
        user.setSubscriptionExpiry(null);
        saveAndFlush(user);
    }

    public int downgradeExpired()          { return checkExpiredSubscriptions(); }

    public int checkExpiredSubscriptions() {
        List<User> expired = userRepository.findExpiringSubscriptions(LocalDateTime.now());
        for (User u : expired) downgradeToFree(u);
        return expired.size();
    }

    public int countExpiredSubscriptions() {
        return userRepository.findExpiringSubscriptions(LocalDateTime.now()).size();
    }

    // ── ROLES ─────────────────────────────────────────────────────────────────

    public void changeRoles(User user, List<String> roles) {
        roles.forEach(ValidationService::requireValidRole);
        user.setRoles(roles);
        saveAndFlush(user);
    }

    // ── PASSWORD RESET ────────────────────────────────────────────────────────

    public void resetPassword(User user, String newPassword) {
        adminResetPassword(user, newPassword);
    }

    public void adminResetPassword(User user, String newPassword) {
        ValidationService.requireMinLength(newPassword, "Password", 6);
        user.setPassword(hashPassword(newPassword));
        saveAndFlush(user);
    }

    // ── LEARNING STATS ────────────────────────────────────────────────────────

    public LearningStats initLearningStats(User user) {
        if (user.getLearningStats() != null) return user.getLearningStats();
        LearningStats stats = new LearningStats();
        stats.setUser(user);
        user.setLearningStats(stats);
        try {
            userRepository.saveLearningStats(stats);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return stats;
    }

    public void updateLearningStats(LearningStats stats) {
        try {
            userRepository.saveLearningStats(stats);
        } catch (Exception e) {
            throw new RuntimeException("Could not update learning stats: " + e.getMessage(), e);
        }
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    public Optional<User> authenticate(String email, String plainPassword) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> "active".equals(u.getStatus()))
                .filter(u -> verifyPassword(plainPassword, u.getPassword()));
    }

    // ── STATISTICS ────────────────────────────────────────────────────────────

    public void printStats()      { printStatistics(); }

    public void printStatistics() {
        System.out.println("  Total users  : " + countAll());
        System.out.println("  Active       : " + countByStatus("active"));
        System.out.println("  Suspended    : " + countByStatus("suspended"));
        System.out.println("  Deleted      : " + countByStatus("deleted"));
        System.out.println("  Premium      : " + countPremium());
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private void saveAndFlush(User user) {
        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Save failed: " + e.getMessage(), e);
        }
    }
}
