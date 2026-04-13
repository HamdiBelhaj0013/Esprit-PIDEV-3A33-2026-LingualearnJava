package org.example.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages notifications via native SQL so we don't need to add
 * a Notification entity to persistence.xml.
 */
public class NotificationService {

    private final EntityManager em;

    public NotificationService(EntityManager em) {
        this.em = em;
    }

    /** Insert a new notification for a user. */
    @SuppressWarnings("unchecked")
    public void sendNotification(Long userId, String type, String message) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            em.createNativeQuery(
                "INSERT INTO notifications (user_id, type, message, is_read, created_at) " +
                "VALUES (:uid, :type, :msg, 0, :now)"
            )
            .setParameter("uid",  userId)
            .setParameter("type", type)
            .setParameter("msg",  message)
            .setParameter("now",  LocalDateTime.now())
            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Could not send notification: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the 5 most recent notifications for a user as plain DTOs.
     * Each NotifRow has: id, type, message, isRead, createdAt.
     */
    @SuppressWarnings("unchecked")
    public List<NotifRow> getRecentForUser(Long userId) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT id, type, message, is_read, created_at " +
            "FROM notifications WHERE user_id = :uid " +
            "ORDER BY created_at DESC LIMIT 5"
        )
        .setParameter("uid", userId)
        .getResultList();

        List<NotifRow> result = new ArrayList<>();
        for (Object[] r : rows) {
            NotifRow n = new NotifRow();
            n.id        = ((Number) r[0]).longValue();
            n.type      = (String) r[1];
            n.message   = (String) r[2];
            n.isRead    = r[3] != null && ((Number) r[3]).intValue() == 1;
            n.createdAt = r[4] != null ? r[4].toString() : "";
            result.add(n);
        }
        return result;
    }

    public static class NotifRow {
        public long    id;
        public String  type;
        public String  message;
        public boolean isRead;
        public String  createdAt;
    }
}
