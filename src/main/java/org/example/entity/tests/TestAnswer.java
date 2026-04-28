package org.example.entity.tests;

import java.time.LocalDateTime;

/**
 * Représente la réponse d'un utilisateur à une question lors d'un passage de test.
 * Permet de calculer les performances réelles par section (Reading, Listening, etc.).
 */
public class TestAnswer {

    private Long          id;
    private Long          testResultId;   // FK → test_result.id
    private Long          questionId;     // FK → test_question.id
    private String        sectionCategory;
    private String        userAnswer;
    private boolean       isCorrect;
    private int           pointsObtained;
    private int           pointsMax;
    private LocalDateTime createdAt;

    public TestAnswer() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Long getTestResultId()                    { return testResultId; }
    public void setTestResultId(Long testResultId)   { this.testResultId = testResultId; }

    public Long getQuestionId()                      { return questionId; }
    public void setQuestionId(Long questionId)       { this.questionId = questionId; }

    public String getSectionCategory()               { return sectionCategory; }
    public void setSectionCategory(String s)         { this.sectionCategory = s; }

    public String getUserAnswer()                    { return userAnswer; }
    public void setUserAnswer(String a)              { this.userAnswer = a; }

    public boolean isCorrect()                       { return isCorrect; }
    public void setCorrect(boolean correct)          { this.isCorrect = correct; }

    public int getPointsObtained()                   { return pointsObtained; }
    public void setPointsObtained(int p)             { this.pointsObtained = p; }

    public int getPointsMax()                        { return pointsMax; }
    public void setPointsMax(int p)                  { this.pointsMax = p; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }
}