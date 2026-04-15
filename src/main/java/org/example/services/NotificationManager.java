package org.example.services;

import org.example.entities.Notification;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private static NotificationManager instance;
    private List<Notification> notifications = new ArrayList<>();
    private Runnable onNewNotification;
    private Runnable onBadgeUpdate; // callback léger, juste pour le badge

    private NotificationManager() {}

    public static NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    public void ajouterNotification(String message, String type, int publicationId) {
        notifications.add(0, new Notification(message, type, publicationId));
        if (onNewNotification != null) {
            javafx.application.Platform.runLater(onNewNotification);
        }
    }

    public List<Notification> getAll() { return notifications; }

    public List<Notification> getNonLues() {
        List<Notification> nonLues = new ArrayList<>();
        for (Notification n : notifications) {
            if (!n.isLue()) nonLues.add(n);
        }
        return nonLues;
    }

    public int getNombreNonLues() { return getNonLues().size(); }

    /** Marque toutes lues SANS déclencher le callback onNewNotification */
    public void marquerToutesLues() {
        for (Notification n : notifications) n.setLue(true);
        // Mettre à jour uniquement le badge, pas le toast
        if (onBadgeUpdate != null) {
            javafx.application.Platform.runLater(onBadgeUpdate);
        }
    }

    public void setOnNewNotification(Runnable callback) { this.onNewNotification = callback; }

    /** Callback appelé uniquement lors d'un changement de badge (marquer lues) */
    public void setOnBadgeUpdate(Runnable callback) { this.onBadgeUpdate = callback; }
}