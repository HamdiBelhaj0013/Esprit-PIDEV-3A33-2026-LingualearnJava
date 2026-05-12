package org.example.entities.forum;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String message;
    private String type; // "like", "dislike", ou "commentaire"
    private LocalDateTime date;
    private boolean lue;
    private int publicationId;
    /** ID of the user who should receive this notification (the post author). */
    private int recipientId;

    /** Constructor for new (unsaved) notifications. */
    public Notification(String message, String type, int publicationId, int recipientId) {
        this.message = message;
        this.type = type;
        this.publicationId = publicationId;
        this.recipientId = recipientId;
        this.date = LocalDateTime.now();
        this.lue = false;
    }

    /** Constructor used when loading a row from the DB. */
    public Notification(int id, String message, String type,
                        int publicationId, int recipientId,
                        LocalDateTime date, boolean lue) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.publicationId = publicationId;
        this.recipientId = recipientId;
        this.date = date;
        this.lue = lue;
    }

    public int getId() { return id; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public LocalDateTime getDate() { return date; }
    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
    public int getPublicationId() { return publicationId; }
    public int getRecipientId() { return recipientId; }
}
