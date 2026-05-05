package org.example.entity;

import java.time.LocalDateTime;

public class LearningStats {

    private Long id;
    private User user;
    private int  totalXP              = 0;
    private int  wordsLearned         = 0;
    private int  totalMinutesStudied  = 0;
    private LocalDateTime lastStudySession;

    public LearningStats() {}

    public Long getId()            { return id; }
    public void setId(Long id)     { this.id = id; }

    public User getUser()          { return user; }
    public void setUser(User user) { this.user = user; }

    public int getTotalXP()                       { return totalXP; }
    public void setTotalXP(int totalXP)           { this.totalXP = totalXP; }

    public int getWordsLearned()                  { return wordsLearned; }
    public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

    public int getTotalMinutesStudied()                         { return totalMinutesStudied; }
    public void setTotalMinutesStudied(int totalMinutesStudied) { this.totalMinutesStudied = totalMinutesStudied; }

    public LocalDateTime getLastStudySession()         { return lastStudySession; }
    public void setLastStudySession(LocalDateTime v)   { this.lastStudySession = v; }

    @Override
    public String toString() {
        return "LearningStats{userId=" + (user != null ? user.getId() : "null")
                + ", xp=" + totalXP + ", words=" + wordsLearned
                + ", minutes=" + totalMinutesStudied + "}";
    }
}
