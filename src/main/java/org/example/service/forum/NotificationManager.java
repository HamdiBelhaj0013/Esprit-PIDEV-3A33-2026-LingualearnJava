package org.example.service.forum;

import org.example.entities.forum.Notification;

import java.util.List;

/**
 * Singleton facade for forum notifications.
 * All storage is delegated to ServiceNotification (MySQL); the in-memory
 * ArrayList has been removed. Public method signatures are unchanged so
 * callers (MainController, NotificationController, PublicationController,
 * CommentaireController) require no modification.
 */
public class NotificationManager {

    private static NotificationManager instance;
    private final ServiceNotification service = new ServiceNotification();
    private Runnable onNewNotification;
    private Runnable onBadgeUpdate;

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persist a notification to the DB and fire the onNewNotification callback
     * so the UI badge / toast updates immediately.
     */
    public void ajouterNotification(String message, String type,
                                    int publicationId, int recipientId) {
        service.ajouterNotification(message, type, publicationId, recipientId);
        if (onNewNotification != null) {
            javafx.application.Platform.runLater(onNewNotification);
        }
    }

    /** Mark all notifications for userId as read, then fire the badge callback. */
    public void marquerLuesPourUser(int userId) {
        service.marquerLuesPourUser(userId);
        if (onBadgeUpdate != null) {
            javafx.application.Platform.runLater(onBadgeUpdate);
        }
    }

    // ── Read (recipient-filtered) ─────────────────────────────────────────────

    /** All notifications for userId, newest first. */
    public List<Notification> getForUser(int userId) {
        return service.getForUser(userId);
    }

    /** Unread notifications for userId. */
    public List<Notification> getNonLuesPourUser(int userId) {
        return service.getNonLuesPourUser(userId);
    }

    /** Count of unread notifications for userId. */
    public int getNombreNonLuesPourUser(int userId) {
        return service.getNombreNonLuesPourUser(userId);
    }

    // ── Legacy helpers (kept for any callers outside the forum module) ────────

    /** All notifications in the DB regardless of recipient. */
    public List<Notification> getAll() {
        return service.getAll();
    }

    public List<Notification> getNonLues() {
        // Without a "current user" in context, return all unread rows.
        // Prefer getNonLuesPourUser(userId) at call sites.
        return service.getAll().stream()
                .filter(n -> !n.isLue())
                .collect(java.util.stream.Collectors.toList());
    }

    public int getNombreNonLues() {
        return (int) getNonLues().size();
    }

    public void marquerToutesLues() {
        service.marquerToutesLues();
        if (onBadgeUpdate != null) {
            javafx.application.Platform.runLater(onBadgeUpdate);
        }
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setOnNewNotification(Runnable callback) { this.onNewNotification = callback; }

    /** Callback invoked only on badge changes (after marking as read). */
    public void setOnBadgeUpdate(Runnable callback) { this.onBadgeUpdate = callback; }
}
