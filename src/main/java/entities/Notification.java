package entities;

import java.time.LocalDateTime;

public class Notification {
    private String message;
    private String type; // "like" ou "commentaire"
    private LocalDateTime date;
    private boolean lue;
    private int publicationId;

    public Notification(String message, String type, int publicationId) {
        this.message = message;
        this.type = type;
        this.publicationId = publicationId;
        this.date = LocalDateTime.now();
        this.lue = false;
    }

    public String getMessage() { return message; }
    public String getType() { return type; }
    public LocalDateTime getDate() { return date; }
    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
    public int getPublicationId() { return publicationId; }
}