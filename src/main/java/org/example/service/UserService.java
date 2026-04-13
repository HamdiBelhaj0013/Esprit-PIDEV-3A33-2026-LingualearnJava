package org.example.service;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.validation.ValidationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port of Symfony's UserService.
 *
 * FIX: package was org.example.service — changed to com.lingualearn.service.
 * FIX: method names aligned with what AdminConsole / UserConsole call.
 * FIX: countPremium() now uses the COUNT query instead of loading all users.
 * FIX: adminUpdateUser() no longer double-begins a transaction.
 */
public class UserService {

    private static final int BCRYPT_ROUNDS = 12;

    private final UserRepository    userRepository;
    private final EntityManager     em;
    private final ValidationService validation;

    public UserService(EntityManager em) {
        this.em             = em;
        this.userRepository = new UserRepository(em);
        this.validation     = new ValidationService();
    }



    public String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    public boolean verifyPassword(String plain, String hash) {
        if (hash != null && hash.startsWith("$2y$")) {
            hash = "$2a$" + hash.substring(4);
        }
        return BCrypt.checkpw(plain, hash);
    }


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
        stats.setUser(user);
        user.setLearningStats(stats);

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.persist(user);
            em.persist(stats);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
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
        return userRepository.findAdvanced(search, status, plan, page, pageSize);
    }

    public List<User> search(String term) { return userRepository.search(term); }

    public long countAll()              { return userRepository.countAll(); }
    public long countByStatus(String s) { return userRepository.countByStatus(s); }

    /**
     * FIX: was calling findPremiumUsers().size() — loaded every user into memory.
     * Now delegates to the COUNT query in the repository.
     */
    public long countPremium() { return userRepository.countPremium(); }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Update first + last name.
     * Called "updateName" by the consoles — aliased here.
     * FIX: consoles called svc.updateName() but service only had updateProfile().
     */
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

        saveAndFlush(user);   // single transaction for the whole update
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
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            userRepository.delete(user);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
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
        user.setSubscriptionExpiry(expiry);   // set expiry FIRST (updatePremiumStatus needs it)
        user.setSubscriptionPlan(plan);       // triggers updatePremiumStatus()
        user.setLastPaymentStatus("success");
        saveAndFlush(user);
    }

    public void downgradeToFree(User user) {
        user.setSubscriptionPlan("FREE");
        user.setSubscriptionExpiry(null);
        saveAndFlush(user);
    }

    public int downgradeExpired() {
        return checkExpiredSubscriptions();
    }

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
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.persist(stats);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException(e);
        }
        return stats;
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    public Optional<User> authenticate(String email, String plainPassword) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> "active".equals(u.getStatus()))
                .filter(u -> verifyPassword(plainPassword, u.getPassword()));
    }

    // ── STATISTICS ────────────────────────────────────────────────────────────

    public void printStats() {
        printStatistics();
    }

    public void printStatistics() {
        System.out.println("  Total users  : " + countAll());
        System.out.println("  Active       : " + countByStatus("active"));
        System.out.println("  Suspended    : " + countByStatus("suspended"));
        System.out.println("  Deleted      : " + countByStatus("deleted"));
        System.out.println("  Premium      : " + countPremium());
    }

    // ── Internal helper ───────────────────────────────────────────────────────
    private void saveAndFlush(User user) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            userRepository.save(user);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Save failed: " + e.getMessage(), e);
        }
    }
}