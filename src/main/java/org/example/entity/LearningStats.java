package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_stats")
public class LearningStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private int totalXP = 0;

    @Column(name = "words_learned", nullable = false)
    private int wordsLearned = 0;

    @Column(name = "total_minutes_studied", nullable = false)
    private int totalMinutesStudied = 0;

    @Column(name = "last_study_session")
    private LocalDateTime lastStudySession;

    public LearningStats() {}


    public Long getId()                           { return id; }

    public User getUser()                         { return user; }
    public void setUser(User user)                { this.user = user; }

    public int getTotalXP()                       { return totalXP; }
    public void setTotalXP(int totalXP)           { this.totalXP = totalXP; }

    public int getWordsLearned()                  { return wordsLearned; }
    public void setWordsLearned(int wordsLearned) { this.wordsLearned = wordsLearned; }

    public int getTotalMinutesStudied()                         { return totalMinutesStudied; }
    public void setTotalMinutesStudied(int totalMinutesStudied) { this.totalMinutesStudied = totalMinutesStudied; }

    public LocalDateTime getLastStudySession()              { return lastStudySession; }
    public void setLastStudySession(LocalDateTime v)        { this.lastStudySession = v; }

    @Override
    public String toString() {
        return "LearningStats{userId=" + (user != null ? user.getId() : "null")
                + ", xp=" + totalXP + ", words=" + wordsLearned
                + ", minutes=" + totalMinutesStudied + "}";
    }
}