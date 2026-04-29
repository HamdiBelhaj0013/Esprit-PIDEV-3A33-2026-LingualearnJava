package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import org.example.entities.pedagogicalcontent.Course;
import org.example.entities.pedagogicalcontent.Lesson;
import org.example.service.CourseService;
import org.example.service.FrontProgressService;
import org.example.service.LessonService;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class FrontStatsController {

    @FXML private Label       lblTotalXp;
    @FXML private Label       lblXpChange;
    @FXML private Label       lblLessonsCompleted;
    @FXML private Label       lblLessonsChange;
    @FXML private Label       lblStudyTime;
    @FXML private Label       lblTimeChange;
    @FXML private Label       lblStreak;
    @FXML private Label       lblStreakMsg;
    @FXML private Label       lblLastActivity;
    @FXML private Label       lblLevelXp;
    @FXML private Label       lblLevelName;
    @FXML private Label       lblLevelBadge;
    @FXML private Label       lblLevelNext;
    @FXML private ProgressBar levelProgressBar;
    @FXML private HBox        weeklyHeatmap;
    @FXML private VBox        coursesProgressList;
    @FXML private Label       lblNoCoursesProgress;
    @FXML private VBox        recentActivityList;
    @FXML private Label       lblNoActivity;
    @FXML private FlowPane    achievementsPane;

    private static final int USER_ID = 124;

    private final FrontProgressService progressService = new FrontProgressService();
    private final CourseService        courseService   = new CourseService();
    private final LessonService        lessonService   = new LessonService();

    // Real values from learning_stats
    private int totalXp       = 0;
    private int totalMinutes  = 0;
    private int wordsLearned  = 0;
    private int totalLessons  = 0; // real count from lesson table

    @FXML
    public void initialize() {
        totalXp      = progressService.getTotalXp(USER_ID);
        totalMinutes = progressService.getTotalMinutesStudied(USER_ID);
        wordsLearned = progressService.getWordsLearned(USER_ID);

        // Real completed lessons count from user_lesson_status
        totalLessons = progressService.getCompletedLessonsCount(USER_ID);

        loadKpis();
        loadLevel();
        loadWeeklyHeatmap();
        loadCoursesProgress();
        loadRecentActivity();
        loadAchievements();
    }

    // ---- KPI CARDS ----
    private void loadKpis() {
        String lastSession = progressService.getLastStudySession(USER_ID);

        lblTotalXp.setText(String.valueOf(totalXp));
        lblLessonsCompleted.setText(String.valueOf(totalLessons));
        lblStudyTime.setText(formatMinutes(totalMinutes));
        lblStreak.setText(progressService.getStreak(USER_ID) + " 🔥");
        lblStreakMsg.setText("Last session: " + lastSession);
        lblLastActivity.setText("Last activity: " + lastSession);

        lblXpChange.setText(totalXp > 0 ? "+" + totalXp + " total" : "No XP yet");
        lblLessonsChange.setText(totalLessons + " in DB");
        lblTimeChange.setText(totalMinutes + " min total");
    }

    // ---- LEVEL SYSTEM ----
    private void loadLevel() {
        String[] names      = {"Beginner", "Explorer", "Learner", "Expert", "Master"};
        int[]    thresholds = {0, 100, 300, 600, 1000};
        String[] emojis     = {"🌱", "📘", "🎯", "🏆", "👑"};
        String[] colors     = {"#16a34a", "#2563eb", "#7c3aed", "#ea580c", "#b45309"};

        int idx = 0;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (totalXp >= thresholds[i]) { idx = i; break; }
        }

        int    nextXp   = idx < thresholds.length - 1 ? thresholds[idx + 1] : thresholds[thresholds.length - 1];
        int    floor    = thresholds[idx];
        double progress = idx == thresholds.length - 1 ? 1.0
                : (double)(totalXp - floor) / (nextXp - floor);

        lblLevelXp.setText(totalXp + " XP");
        lblLevelName.setText(emojis[idx] + "  " + names[idx]);
        lblLevelBadge.setText(names[idx]);
        lblLevelBadge.setStyle(
                "-fx-background-color:" + colors[idx] + ";" +
                        "-fx-text-fill:#ffffff; -fx-background-radius:20;" +
                        "-fx-padding:4 14; -fx-font-size:11px; -fx-font-weight:bold;");
        lblLevelNext.setText(idx < thresholds.length - 1
                ? (nextXp - totalXp) + " XP to next level" : "Max level reached! 🎉");
        levelProgressBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
    }

    // ---- WEEKLY HEATMAP ----
    private void loadWeeklyHeatmap() {
        weeklyHeatmap.getChildren().clear();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate day    = today.minusDays(i);
            // Real activity check from DB
            boolean   active = progressService.hasActivityOnDate(USER_ID, day);

            Label dayLabel = new Label(day.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            dayLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8; -fx-font-weight:bold;");

            Label dot = new Label();
            dot.setPrefSize(32, 32);
            dot.setMinSize(32, 32);

            if (day.equals(today)) {
                dot.setStyle("-fx-background-color:" + (active ? "#2563eb" : "#dbeafe") + ";" +
                        "-fx-background-radius:8; -fx-border-color:#2563eb;" +
                        "-fx-border-radius:8; -fx-border-width:2;");
            } else {
                dot.setStyle("-fx-background-color:" + (active ? "#2563eb" : "#e2e8f0") + ";" +
                        "-fx-background-radius:8;");
            }

            VBox cell = new VBox(4, dayLabel, dot);
            cell.setAlignment(Pos.CENTER);
            weeklyHeatmap.getChildren().add(cell);
        }
    }

    // ---- COURSES PROGRESS ----
    private void loadCoursesProgress() {
        try {
            List<Course> courses = courseService.getAll();
            coursesProgressList.getChildren().clear();

            if (courses == null || courses.isEmpty()) {
                lblNoCoursesProgress.setManaged(true);
                lblNoCoursesProgress.setVisible(true);
                return;
            }

            int shown = 0;
            for (Course course : courses) {
                List<Lesson> lessons = lessonService.getLessonsByCourse(course.getId());
                int total = lessons == null ? 0 : lessons.size();
                if (total == 0) continue;

                // Real earned XP in this course
                int courseEarnedXp = progressService.getEarnedXpForCourse(USER_ID, course.getId());
                int lessonsDone    = progressService.getCompletedLessonsCountForCourse(USER_ID, course.getId());
                
                // Calculate total XP possible for this course
                int courseMaxXp = lessons.stream().mapToInt(Lesson::getXpReward).sum();
                
                // Real completion ratio
                double pct = courseMaxXp > 0
                        ? Math.min(1.0, (double) courseEarnedXp / courseMaxXp)
                        : 0.0;

                String color = pct >= 1.0 ? "#16a34a" : pct >= 0.5 ? "#2563eb" : "#f59e0b";

                Label title = new Label(course.getTitle());
                title.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

                Label count = new Label(lessonsDone + "/" + total + " lessons • " + courseEarnedXp + " XP");
                count.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox top = new HBox(title, spacer, count);
                top.setAlignment(Pos.CENTER_LEFT);

                ProgressBar bar = new ProgressBar(pct);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefHeight(8);
                bar.setStyle("-fx-accent:" + color + ";");

                VBox row = new VBox(8, top, bar);
                row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:10;" +
                        "-fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-padding:12 14;");

                coursesProgressList.getChildren().add(row);
                shown++;
            }

            if (shown == 0) {
                lblNoCoursesProgress.setManaged(true);
                lblNoCoursesProgress.setVisible(true);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ---- RECENT ACTIVITY ----
    private void loadRecentActivity() {
        try {
            List<Lesson> recent = progressService.getRecentCompletedLessons(USER_ID, 5);
            recentActivityList.getChildren().clear();

            if (recent == null || recent.isEmpty()) {
                lblNoActivity.setManaged(true);
                lblNoActivity.setVisible(true);
                return;
            }

            for (Lesson lesson : recent) {
                Label icon = new Label("📖");
                icon.setStyle("-fx-font-size:16px;");

                Label name = new Label(lesson.getTitle());
                name.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

                Label xp = new Label(lesson.getXpReward() + " XP");
                xp.setStyle("-fx-font-size:11px; -fx-text-fill:#2563eb; -fx-font-weight:bold;");

                VBox info = new VBox(2, name, xp);
                HBox row  = new HBox(12, icon, info);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:10;" +
                        "-fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-padding:10 14;");

                recentActivityList.getChildren().add(row);
            }
        } catch (Exception e) {
            lblNoActivity.setManaged(true);
            lblNoActivity.setVisible(true);
        }
    }

    // ---- ACHIEVEMENTS — based on real data ----
    private void loadAchievements() {
        int streak = progressService.getStreak(USER_ID);
        achievementsPane.getChildren().clear();

        addAchievement("⭐", "First XP",     "Earn your first XP",   totalXp >= 1);
        addAchievement("💎", "50 XP",        "Earn 50 XP",           totalXp >= 50);
        addAchievement("🏅", "100 XP",       "Earn 100 XP",          totalXp >= 100);
        addAchievement("👑", "500 XP",       "Earn 500 XP",          totalXp >= 500);
        addAchievement("📖", "1 Lesson",     "Have 1 lesson",        totalLessons >= 1);
        addAchievement("📚", "5 Lessons",    "Have 5 lessons",       totalLessons >= 5);
        addAchievement("⏱",  "30 min",       "Study 30 minutes",     totalMinutes >= 30);
        addAchievement("🔥", "Active today", "Study today",          streak >= 1);
    }

    private void addAchievement(String emoji, String name, String desc, boolean unlocked) {
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:28px;" + (unlocked ? "" : " -fx-opacity:0.35;"));

        Label title = new Label(name);
        title.setWrapText(true);
        title.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-alignment:CENTER;" +
                "-fx-text-alignment:CENTER; -fx-text-fill:" + (unlocked ? "#0f172a" : "#94a3b8") + ";");

        Label description = new Label(desc);
        description.setWrapText(true);
        description.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8;" +
                " -fx-alignment:CENTER; -fx-text-alignment:CENTER;");

        Label status = new Label(unlocked ? "✓ Unlocked" : "🔒 Locked");
        status.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:"
                + (unlocked ? "#16a34a" : "#94a3b8") + ";");

        VBox badge = new VBox(6, icon, title, description, status);
        badge.setAlignment(Pos.CENTER);
        badge.setPrefWidth(120);
        badge.setPadding(new Insets(14, 12, 14, 12));
        badge.setStyle(unlocked
                ? "-fx-background-color:#ffffff; -fx-background-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-radius:12;" +
                "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.07),10,0,0,2);"
                : "-fx-background-color:#f8fafc; -fx-background-radius:12;" +
                "-fx-border-color:#e2e8f0; -fx-border-radius:12; -fx-opacity:0.55;");

        achievementsPane.getChildren().add(badge);
    }

    private String formatMinutes(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60, m = minutes % 60;
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }
}