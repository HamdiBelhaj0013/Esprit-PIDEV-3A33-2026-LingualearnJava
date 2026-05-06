package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.services.BackofficeStatsService;
import org.example.services.BackofficeStatsService.StudentStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackofficeStatsController {

    // ── KPI Labels ─────────────────────────────────────────────────────────────
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalCourses;
    @FXML private Label lblTotalLessons;
    @FXML private Label lblTotalXp;
    @FXML private Label lblAvgScore;

    // ── Charts ─────────────────────────────────────────────────────────────────
    @FXML private PieChart coursesPieChart;
    @FXML private PieChart premiumPieChart;
    @FXML private BarChart<String, Number> lessonsBarChart;

    // ── Custom Panels ──────────────────────────────────────────────────────────
    @FXML private FlowPane barLegendPane;
    @FXML private VBox leaderboardPane;

    private final BackofficeStatsService service = new BackofficeStatsService();
    private final Random random = new Random();

    // 10 maximally distinct colors — one per element regardless of palette
    private static final String[] DISTINCT = {
        "#e11d48", // red
        "#f59e0b", // amber
        "#10b981", // emerald
        "#3b82f6", // blue
        "#8b5cf6", // purple
        "#f97316", // orange
        "#06b6d4", // cyan
        "#84cc16", // lime
        "#ec4899", // pink
        "#6366f1"  // indigo
    };

    // Refresh color modifier: bright or muted
    private boolean brightMode = true;
    private final List<String> courseNames = new ArrayList<>();

    @FXML
    public void initialize() {
        refreshStats();
    }

    @FXML
    private void refreshStats() {
        brightMode = !brightMode; // toggle contrast on each refresh
        loadKpis();
        loadCharts();
        Platform.runLater(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            applyDistinctColors();
            buildBarLegend();
            buildLeaderboard();
        });
    }

    // ── KPIs ───────────────────────────────────────────────────────────────────

    private void loadKpis() {
        lblTotalUsers.setText(String.valueOf(service.getTotalUsers()));
        lblTotalCourses.setText(String.valueOf(service.getTotalCourses()));
        lblTotalLessons.setText(String.valueOf(service.getTotalLessons()));
        lblTotalXp.setText(String.format("%,d", service.getTotalPlatformXp()));
        lblAvgScore.setText(service.getAverageQuizScore() + "%");
    }

    // ── Chart Data ─────────────────────────────────────────────────────────────

    private void loadCharts() {
        loadPieChart();
        loadBarChart();
        loadPremiumPie();
    }

    private void loadPieChart() {
        Map<String, Integer> data = service.getCoursesPerLanguage();
        coursesPieChart.getData().clear();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            coursesPieChart.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }
    }

    private void loadBarChart() {
        Map<String, Integer> data = service.getLessonsPerCourse();
        lessonsBarChart.getData().clear();
        courseNames.clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Lessons");
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("—", 0));
            courseNames.add("C1 — No data");
        } else {
            int idx = 1;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                series.getData().add(new XYChart.Data<>("C" + idx, e.getValue()));
                courseNames.add("C" + idx + "  " + e.getKey());
                idx++;
            }
        }
        lessonsBarChart.getData().add(series);
    }



    private void loadPremiumPie() {
        Map<String, Integer> data = service.getPremiumVsFreeCount();
        premiumPieChart.getData().clear();
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            premiumPieChart.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }
    }

    // ── Color Application ──────────────────────────────────────────────────────

    private String color(int index) {
        String hex = DISTINCT[index % DISTINCT.length];
        if (brightMode) return hex;
        // Muted version: blend toward white slightly
        return hex + "cc"; // ~80% opacity makes it feel lighter on refresh
    }

    private void applyDistinctColors() {
        // Pie: content distribution — each language gets a distinct color
        int i = 0;
        for (PieChart.Data d : coursesPieChart.getData()) {
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-pie-color: " + color(i) + ";");
            i++;
        }

        // Bar chart — each bar is a different distinct color
        if (!lessonsBarChart.getData().isEmpty()) {
            int j = 0;
            for (XYChart.Data<String, Number> d : lessonsBarChart.getData().get(0).getData()) {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-bar-fill: " + color(j) + ";");
                j++;
            }
        }



        // Premium pie — gold and slate
        String[] premiumColors = {"#f59e0b", "#94a3b8"}; // amber=premium, slate=free
        int k = 0;
        for (PieChart.Data d : premiumPieChart.getData()) {
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-pie-color: " + premiumColors[k % 2] + ";");
            k++;
        }
    }

    // ── Custom Bar Legend ──────────────────────────────────────────────────────

    private void buildBarLegend() {
        barLegendPane.getChildren().clear();
        for (int i = 0; i < courseNames.size(); i++) {
            String c = color(i);
            Circle dot = new Circle(7);
            dot.setFill(Color.web(DISTINCT[i % DISTINCT.length]));
            dot.setStroke(Color.web("#e2e8f0"));
            dot.setStrokeWidth(1);

            Label lbl = new Label(courseNames.get(i));
            lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-font-weight: 600;");

            HBox item = new HBox(6, dot, lbl);
            item.setAlignment(Pos.CENTER_LEFT);
            barLegendPane.getChildren().add(item);
        }
    }

    // ── Leaderboard ────────────────────────────────────────────────────────────

    private void buildLeaderboard() {
        leaderboardPane.getChildren().clear();
        List<StudentStat> top = service.getTopStudentsByXp();

        if (top.isEmpty()) {
            Label empty = new Label("No student data found. Run populate_stats_test.sql to add test data.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
            leaderboardPane.getChildren().add(empty);
            return;
        }

        int maxXp = top.get(0).xp();
        String[] medals = {"🥇", "🥈", "🥉", "4️⃣", "5️⃣"};

        for (int i = 0; i < top.size(); i++) {
            StudentStat s = top.get(i);
            String barColor = DISTINCT[i % DISTINCT.length];

            // Rank badge
            Label rank = new Label(medals[i]);
            rank.setStyle("-fx-font-size: 18px; -fx-min-width: 36px;");
            rank.setAlignment(Pos.CENTER);

            // Student name
            Label name = new Label(s.name());
            name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-min-width: 160px;");

            // XP Progress bar
            ProgressBar bar = new ProgressBar(maxXp > 0 ? (double) s.xp() / maxXp : 0);
            bar.setPrefHeight(14);
            bar.setStyle("-fx-accent: " + barColor + ";");
            HBox.setHgrow(bar, Priority.ALWAYS);

            // XP label
            Label xpLbl = new Label(String.format("%,d XP", s.xp()));
            xpLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + barColor + "; -fx-min-width: 90px;");
            xpLbl.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(12, rank, name, bar, xpLbl);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 10 14; -fx-background-color: " +
                         (i % 2 == 0 ? "#f8fafc" : "white") +
                         "; -fx-background-radius: 8;");
            leaderboardPane.getChildren().add(row);
        }
    }
}
