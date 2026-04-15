package org.example.entity;

import java.time.LocalDateTime;

public class SupportResponse {
    private int id;
    private String message;
    private LocalDateTime respondedAt;
    private int reclamationId;
    private Integer authorId;

    public SupportResponse() {}

    public SupportResponse(String message, int reclamationId, Integer authorId) {
        this.message = message;
        this.reclamationId = reclamationId;
        this.authorId = authorId;
        this.respondedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime t) { this.respondedAt = t; }

    public int getReclamationId() { return reclamationId; }
    public void setReclamationId(int r) { this.reclamationId = r; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer a) { this.authorId = a; }
}

