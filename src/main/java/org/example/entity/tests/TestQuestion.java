package org.example.entity.tests;

import java.time.LocalDateTime;

public class TestQuestion {

    private Long          id;
    private MockTest      mockTest;
    private String        sectionCategory;
    private String        questionType;
    private String        questionText;
    private String        readingPassage;
    private String        audioText;
    private String        writingSubject;
    private String        options;
    private String        correctAnswer;
    private int           points = 1;
    private boolean       isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String        embedding;

    public TestQuestion() {}

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }
    public MockTest getMockTest()                    { return mockTest; }
    public void setMockTest(MockTest mockTest)        { this.mockTest = mockTest; }
    public String getSectionCategory()               { return sectionCategory; }
    public void setSectionCategory(String s)         { this.sectionCategory = s; }
    public String getQuestionType()                  { return questionType; }
    public void setQuestionType(String t)            { this.questionType = t; }
    public String getQuestionText()                  { return questionText; }
    public void setQuestionText(String t)            { this.questionText = t; }
    public String getReadingPassage()                { return readingPassage; }
    public void setReadingPassage(String r)          { this.readingPassage = r; }
    public String getAudioText()                     { return audioText; }
    public void setAudioText(String a)               { this.audioText = a; }
    public String getWritingSubject()                { return writingSubject; }
    public void setWritingSubject(String w)          { this.writingSubject = w; }
    public String getOptions()                       { return options; }
    public void setOptions(String options)           { this.options = options; }
    public String getCorrectAnswer()                 { return correctAnswer; }
    public void setCorrectAnswer(String c)           { this.correctAnswer = c; }
    public int getPoints()                           { return points; }
    public void setPoints(int points)                { this.points = points; }
    public boolean isActive()                        { return isActive; }
    public void setActive(boolean active)            { this.isActive = active; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)        { this.updatedAt = v; }
    public String getEmbedding()                     { return embedding; }
    public void setEmbedding(String embedding)       { this.embedding = embedding; }

    @Override
    public String toString() {
        return "TestQuestion{id=" + id + ", section='" + sectionCategory
                + "', points=" + points + "}";
    }
}
