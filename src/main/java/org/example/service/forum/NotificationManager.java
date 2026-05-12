package org.example.service.forum;

import org.example.entities.forum.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationManager {

    private static NotificationManager instance;
    private List<Notification> notifications = new ArrayList<>();
    private Runnable onNewNotification;
    private Runnable onBadgeUpdate;

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    /**
     * Add a notification targeted at a specific recipient (the post author).
     * Call sites must pass the author's user ID as recipientId.
     */
    public void ajouterNotification(String message, String type, int publicationId, int recipientId) {
        notifications.add(0, new Notification(message, type, publicationId, recipientId));
        if (onNewNotification != null) {
            javafx.application.Platform.runLater(onNewNotification);
        }
    }

    // ── Full list (used only for legacy callers; prefer getForUser) ──────────
    public List<Notification> getAll() { return notifications; }

    // ── Recipient-filtered accessors ─────────────────────────────────────────

    /** Returns all notifications whose recipientId matches userId. */
    public List<Notification> getForUser(int userId) {
        return notifications.stream()
                .filter(n -> n.getRecipientId() == userId)
                .collect(Collectors.toList());
    }

    /** Returns unread notifications for a specific user. */
    public List<Notification> getNonLuesPourUser(int userId) {
        return notifications.stream()
                .filter(n -> n.getRecipientId() == userId && !n.isLue())
                .collect(Collectors.toList());
    }

    /** Returns the count of unread notifications for a specific user. */
    public int getNombreNonLuesPourUser(int userId) {
        return getNonLuesPourUser(userId).size();
    }

    /** Marks all notifications for a specific user as read, then fires the badge callback. */
    public void marquerLuesPourUser(int userId) {
        notifications.stream()
                .filter(n -> n.getRecipientId() == userId)
                .forEach(n -> n.setLue(true));
        if (onBadgeUpdate != null) {
            javafx.application.Platform.runLater(onBadgeUpdate);
        }
    }

    // ── Legacy helpers (kept for any callers outside the forum module) ────────

    public List<Notification> getNonLues() {
        return notifications.stream().filter(n -> !n.isLue()).collect(Collectors.toList());
    }

    public int getNombreNonLues() { return getNonLues().size(); }

    public void marquerToutesLues() {
        notifications.forEach(n -> n.setLue(true));
        if (onBadgeUpdate != null) {
            javafx.application.Platform.runLater(onBadgeUpdate);
        }
    }

    public void setOnNewNotification(Runnable callback) { this.onNewNotification = callback; }

    /** Callback invoked only on badge changes (after marking as read). */
    public void setOnBadgeUpdate(Runnable callback) { this.onBadgeUpdate = callback; }
}
