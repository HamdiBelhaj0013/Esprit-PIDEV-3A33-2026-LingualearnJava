package org.example.entity;

import java.time.LocalDateTime;

public class Reclamation {
    private int           id;
    private String        subject;
    private String        messageBody;
    private String        status;
    private LocalDateTime submittedAt;
    private int           userId;
    private String        priority;
    private LocalDateTime slaDeadline;
    private String        imagePath;   // ✅ nouveau

    public Reclamation() {}

    public Reclamation(int userId, String subject, String messageBody, String priority) {
        this.userId      = userId;
        this.subject     = subject;
        this.messageBody = messageBody;
        this.priority    = priority;
        this.status      = "PENDING";
        this.submittedAt = LocalDateTime.now();
    }

    public int           getId()           { return id; }
    public void          setId(int id)     { this.id = id; }
    public String        getSubject()      { return subject; }
    public void          setSubject(String s)       { this.subject = s; }
    public String        getMessageBody()  { return messageBody; }
    public void          setMessageBody(String m)   { this.messageBody = m; }
    public String        getStatus()       { return status; }
    public void          setStatus(String s)        { this.status = s; }
    public LocalDateTime getSubmittedAt()  { return submittedAt; }
    public void          setSubmittedAt(LocalDateTime t) { this.submittedAt = t; }
    public int           getUserId()       { return userId; }
    public void          setUserId(int u)  { this.userId = u; }
    public String        getPriority()     { return priority; }
    public void          setPriority(String p)      { this.priority = p; }
    public LocalDateTime getSlaDeadline()  { return slaDeadline; }
    public void          setSlaDeadline(LocalDateTime s) { this.slaDeadline = s; }
    public String        getImagePath()    { return imagePath; }   // ✅
    public void          setImagePath(String i) { this.imagePath = i; } // ✅

    @Override
    public String toString() { return subject + " [" + status + "]"; }
}