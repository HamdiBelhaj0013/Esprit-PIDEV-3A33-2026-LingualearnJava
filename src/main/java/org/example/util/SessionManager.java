package org.example.util;

import jakarta.persistence.EntityManager;
import org.example.App;
import org.example.entity.User;

/**
 * Holds the authenticated user and a session-scoped EntityManager
 * for the duration of a user's login session.
 */
public class SessionManager {

    private static User          currentUser;
    private static EntityManager entityManager;

    private SessionManager() {}

    // ── Current user ─────────────────────────────────────────────────────────

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // ── Session EntityManager ─────────────────────────────────────────────────

    public static EntityManager getEntityManager() {
        if (entityManager == null || !entityManager.isOpen()) {
            entityManager = App.getEmf().createEntityManager();
        }
        return entityManager;
    }

    // ── Logout / cleanup ──────────────────────────────────────────────────────

    public static void clearSession() {
        currentUser = null;
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        entityManager = null;
    }
}
