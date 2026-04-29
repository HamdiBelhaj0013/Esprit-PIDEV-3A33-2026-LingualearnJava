package org.example.entities.pedagogicalcontent;

import java.sql.Timestamp;

public class Course {

    private int id;
    private String title;
    private String level;
    private String status;
    private Timestamp publishedAt;
    private Integer authorId;
    private int platformLanguageId;

    public Course() {
    }

    public Course(String title, String level, String status, Timestamp publishedAt, Integer authorId, int platformLanguageId) {
        this.title = title;
        this.level = level;
        this.status = status;
        this.publishedAt = publishedAt;
        this.authorId = authorId;
        this.platformLanguageId = platformLanguageId;
    }

    public Course(int id, String title, String level, String status, Timestamp publishedAt, Integer authorId, int platformLanguageId) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.status = status;
        this.publishedAt = publishedAt;
        this.authorId = authorId;
        this.platformLanguageId = platformLanguageId;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public int getPlatformLanguageId() {
        return platformLanguageId;
    }

    public void setPlatformLanguageId(int platformLanguageId) {
        this.platformLanguageId = platformLanguageId;
    }

    @Override
    public String toString() {
        return title;
    }
}