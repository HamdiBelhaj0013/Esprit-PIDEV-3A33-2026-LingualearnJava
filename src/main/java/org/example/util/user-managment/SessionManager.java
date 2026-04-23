package org.example.util;

import org.example.entity.User;

/**
 * Holds the authenticated user for the duration of a login session.
 * EntityManager has been removed — all DB access now goes through
 * MyDataBase.getInstance().getConnection() via the service/repository layer.
 */
public class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static User getCurrentUser()           { return currentUser; }
    public static void setCurrentUser(User user)  { currentUser = user; }

    public static void clearSession() {
        currentUser = null;
    }
}
