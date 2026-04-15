package models;

import java.time.LocalDateTime;

public class FAQ {
    private int id;
    private String question;
    private String answer;
    private String subject;
    private String category;
    private LocalDateTime submittedAt;

    public FAQ() {}

    public FAQ(String question, String answer, String subject, String category) {
        this.question = question;
        this.answer = answer;
        this.subject = subject;
        this.category = category;
        this.submittedAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }

    public String getAnswer() { return answer; }
    public void setAnswer(String a) { this.answer = a; }

    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }

    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime t) { this.submittedAt = t; }

    @Override
    public String toString() { return question; }
}
