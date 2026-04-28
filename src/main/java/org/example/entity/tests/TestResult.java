package org.example.entity.tests;

import org.example.entity.User;
import java.time.LocalDateTime;

public class TestResult {

    private Long          id;
    private User          user;
    private MockTest      mockTest;
    private float         overallScore;
    private float         aiPredictedScore;
    private String        aiWeaknessReport;
    private String        aiCorrection;
    private String        aiNote;
    private LocalDateTime dateTaken;
    private LocalDateTime updatedAt;

    public TestResult() {}

    public Long getId()                                  { return id; }
    public void setId(Long id)                           { this.id = id; }
    public User getUser()                                { return user; }
    public void setUser(User user)                       { this.user = user; }
    public MockTest getMockTest()                        { return mockTest; }
    public void setMockTest(MockTest mockTest)           { this.mockTest = mockTest; }
    public float getOverallScore()                       { return overallScore; }
    public void setOverallScore(float overallScore)      { this.overallScore = overallScore; }
    public float getAiPredictedScore()                   { return aiPredictedScore; }
    public void setAiPredictedScore(float s)             { this.aiPredictedScore = s; }
    public String getAiWeaknessReport()                  { return aiWeaknessReport; }
    public void setAiWeaknessReport(String r)            { this.aiWeaknessReport = r; }
    public String getAiCorrection()                      { return aiCorrection; }
    public void setAiCorrection(String c)                { this.aiCorrection = c; }
    public String getAiNote()                            { return aiNote; }
    public void setAiNote(String n)                      { this.aiNote = n; }
    public LocalDateTime getDateTaken()                  { return dateTaken; }
    public void setDateTaken(LocalDateTime dateTaken)    { this.dateTaken = dateTaken; }
    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)            { this.updatedAt = v; }

    @Override
    public String toString() {
        return "TestResult{id=" + id
                + ", user=" + (user != null ? user.getEmail() : "null")
                + ", test=" + (mockTest != null ? mockTest.getTitle() : "null")
                + ", score=" + overallScore + "}";
    }
}