package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.service.ExerciceService;
import org.example.service.QuizService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsController {

    @FXML private SidebarController sidebarController;

    // Summary Cards
    @FXML private Label lblTotalQuizzes;
    @FXML private Label lblTotalExercises;
    @FXML private Label lblAvgScore;

    // Charts
    @FXML private BarChart<String, Number> barChartQuestions;
    @FXML private PieChart pieChartDifficulty;
    @FXML private LineChart<String, Number> lineChartScores;

    private final QuizService quizService = new QuizService();
    private final ExerciceService exService = new ExerciceService();

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setActive("stats");
        }
        refreshStats();
    }

    @FXML
    private void refreshStats() {
        List<Quiz> quizzes = quizService.getAllQuizzes();
        List<Exercice> exercises = exService.getAllExercices();

        // 1. Summary Cards Data
        lblTotalQuizzes.setText(String.valueOf(quizzes.size()));
        lblTotalExercises.setText(String.valueOf(exercises.size()));

        double avgScore = quizzes.stream().mapToInt(Quiz::getPassingScore).average().orElse(0.0);
        lblAvgScore.setText(String.format("%.1f%%", avgScore));

        // Clear existing chart data
        barChartQuestions.getData().clear();
        pieChartDifficulty.getData().clear();
        lineChartScores.getData().clear();

        // 2. Bar Chart: Exercises by Category Type
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Exercise Count");
        Map<String, Long> typeCounts = exercises.stream()
                .filter(e -> e.getType() != null && !e.getType().isEmpty())
                .collect(Collectors.groupingBy(Exercice::getType, Collectors.counting()));
        
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            String typeName = entry.getKey().replace("_", " ");
            typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
            barSeries.getData().add(new XYChart.Data<>(typeName, entry.getValue()));
        }
        barChartQuestions.getData().add(barSeries);

        // 3. Pie Chart: Exercise Difficulty
        Map<Integer, Long> diffCounts = exercises.stream()
                .collect(Collectors.groupingBy(Exercice::getDifficulty, Collectors.counting()));
        
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<Integer, Long> entry : diffCounts.entrySet()) {
            pieData.add(new PieChart.Data("Level " + entry.getKey(), entry.getValue()));
        }
        pieChartDifficulty.setData(pieData);

        // 4. Line Chart: Quiz Passing Scores
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Passing Score");
        for (Quiz q : quizzes) {
            lineSeries.getData().add(new XYChart.Data<>(shortenTitle(q.getTitle()), q.getPassingScore()));
        }
        lineChartScores.getData().add(lineSeries);
    }

    private String shortenTitle(String title) {
        if (title == null) return "N/A";
        return title.length() > 15 ? title.substring(0, 15) + "..." : title;
    }
}
