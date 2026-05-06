package org.example.entities;

public class Lesson {
    private int id;
    private String title;

    public Lesson() {}

    public Lesson(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public String toString() {
        return title;
    }
}
