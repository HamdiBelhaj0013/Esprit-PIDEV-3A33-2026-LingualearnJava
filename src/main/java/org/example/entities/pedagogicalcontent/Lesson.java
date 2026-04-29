package org.example.entities.pedagogicalcontent;

import java.sql.Timestamp;

public class Lesson {

    private int id;
    private String title;
    private String content;
    private String vocabularyData;
    private String grammarData;
    private int xpReward;
    private int courseId;
    private String videoName;
    private String thumbName;
    private String resourceName;
    private Timestamp updatedAt;

    public Lesson() {
    }

    public Lesson(String title, String content, String vocabularyData, String grammarData,
                  int xpReward, int courseId, String videoName, String thumbName,
                  String resourceName, Timestamp updatedAt) {
        this.title = title;
        this.content = content;
        this.vocabularyData = vocabularyData;
        this.grammarData = grammarData;
        this.xpReward = xpReward;
        this.courseId = courseId;
        this.videoName = videoName;
        this.thumbName = thumbName;
        this.resourceName = resourceName;
        this.updatedAt = updatedAt;
    }

    public Lesson(int id, String title, String content, String vocabularyData, String grammarData,
                  int xpReward, int courseId, String videoName, String thumbName,
                  String resourceName, Timestamp updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.vocabularyData = vocabularyData;
        this.grammarData = grammarData;
        this.xpReward = xpReward;
        this.courseId = courseId;
        this.videoName = videoName;
        this.thumbName = thumbName;
        this.resourceName = resourceName;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVocabularyData() {
        return vocabularyData;
    }

    public void setVocabularyData(String vocabularyData) {
        this.vocabularyData = vocabularyData;
    }

    public String getGrammarData() {
        return grammarData;
    }

    public void setGrammarData(String grammarData) {
        this.grammarData = grammarData;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getThumbName() {
        return thumbName;
    }

    public void setThumbName(String thumbName) {
        this.thumbName = thumbName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return title;
    }
}