package org.example.entities;

public class Rating {
    private int id;
    private int userId;
    private int exerciceId;
    private int value;

    public Rating() {}

    public Rating(int userId, int exerciceId, int value) {
        this.userId = userId;
        this.exerciceId = exerciceId;
        this.value = value;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getExerciceId() { return exerciceId; }
    public void setExerciceId(int exerciceId) { this.exerciceId = exerciceId; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
