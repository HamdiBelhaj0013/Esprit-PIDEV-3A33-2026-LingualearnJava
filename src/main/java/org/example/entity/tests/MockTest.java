package org.example.entity.tests;

import java.time.LocalDateTime;

public class MockTest {

    private Long             id;
    private String           title;
    private String           testType;
    private String           testCategory;
    private String           level;
    private int              durationMinutes;
    private boolean          isActive = true;
    private LocalDateTime    createdAt;
    private LocalDateTime    updatedAt;
    private PlatformLanguage platformLanguage;

    public MockTest() {}

    public Long getId()                                  { return id; }
    public void setId(Long id)                           { this.id = id; }
    public String getTitle()                             { return title; }
    public void setTitle(String title)                   { this.title = title; }
    public String getTestType()                          { return testType; }
    public void setTestType(String testType)             { this.testType = testType; }
    public String getTestCategory()                      { return testCategory; }
    public void setTestCategory(String testCategory)     { this.testCategory = testCategory; }
    public String getLevel()                             { return level; }
    public void setLevel(String level)                   { this.level = level; }
    public int getDurationMinutes()                      { return durationMinutes; }
    public void setDurationMinutes(int d)                { this.durationMinutes = d; }
    public boolean isActive()                            { return isActive; }
    public void setActive(boolean active)                { this.isActive = active; }
    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime v)            { this.createdAt = v; }
    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)            { this.updatedAt = v; }
    public PlatformLanguage getPlatformLanguage()        { return platformLanguage; }
    public void setPlatformLanguage(PlatformLanguage l)  { this.platformLanguage = l; }

    @Override
    public String toString() {
        return "MockTest{id=" + id + ", type='" + testType
                + "', title='" + title + "', level='" + level + "'}";
    }
}