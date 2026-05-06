package org.example.entities;

import java.sql.Timestamp;

public class Quiz {
    private int id;
    private String title;
    private String description;
    private int passingScore;
    private int questionCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean enabled;
    private Integer lessonId; // Peut être null
    private int difficulty;
    private String skillCodes;

    // Constructeur vide
    public Quiz() {
    }

    // Constructeur avec paramètres (sans l'ID pour l'insertion)
    public Quiz(String title, String description, int passingScore, int questionCount, boolean enabled, Integer lessonId, int difficulty, String skillCodes) {
        this.title = title;
        this.description = description;
        this.passingScore = passingScore;
        this.questionCount = questionCount;
        this.enabled = enabled;
        this.lessonId = lessonId;
        this.difficulty = difficulty;
        this.skillCodes = skillCodes;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Integer getLessonId() { return lessonId; }
    public void setLessonId(Integer lessonId) { this.lessonId = lessonId; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    public String getSkillCodes() { return skillCodes; }
    public void setSkillCodes(String skillCodes) { this.skillCodes = skillCodes; }

    @Override
    public String toString() {
        return title + " (Questions: " + questionCount + ")";
    }
}
