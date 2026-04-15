package utils;

public class Session {
    private static int currentUserId = 1;
    private static String currentRole = "ROLE_USER";

    public static int getCurrentUserId() { return currentUserId; }
    public static String getCurrentRole() { return currentRole; }

    public static void setCurrentUser(int id, String role) {
        currentUserId = id;
        currentRole = role;
    }

    public static boolean isAdmin() {
        return "ROLE_ADMIN".equals(currentRole);
    }
}
