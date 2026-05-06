package org.example.controller.admin.user_managment;

import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.service.user_managment.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.example.repository.UserRepository;
import org.example.service.ai.AdminAiService;

public class StatsController {

    @FXML private Label   userNameLabel;
    @FXML private Label   currentXpLabel;
    @FXML private Label   currentWordsLabel;
    @FXML private Label   currentMinutesLabel;
    @FXML private Spinner<Integer> xpSpinner;
    @FXML private Spinner<Integer> wordsSpinner;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private Label   errorLabel;

    private User     user;
    private Runnable onSaved;

    // AI-FEATURE: stats-summary
    @FXML private Button generateStatsBtn;
    @FXML private Label  summaryLabel;

    public void setUser(User u, Runnable onSaved) {
        this.user    = u;
        this.onSaved = onSaved;

        userNameLabel.setText(u.getFullName());

        LearningStats stats = u.getLearningStats();
        int xp      = stats != null ? stats.getTotalXP()              : 0;
        int words   = stats != null ? stats.getWordsLearned()         : 0;
        int minutes = stats != null ? stats.getTotalMinutesStudied()  : 0;

        currentXpLabel.setText(String.valueOf(xp));
        currentWordsLabel.setText(String.valueOf(words));
        currentMinutesLabel.setText(String.valueOf(minutes));

        xpSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9_999_999, xp, 10));
        wordsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9_999_999, words, 10));
        minutesSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9_999_999, minutes, 10));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        // Commit any manually typed value in the spinners
        commitSpinner(xpSpinner);
        commitSpinner(wordsSpinner);
        commitSpinner(minutesSpinner);

        try {
            UserService svc = new UserService();

            // Reload user to get fresh managed instance
            User managed = svc.findById(user.getId()).orElseThrow(
                () -> new IllegalStateException("User not found"));

            LearningStats stats = managed.getLearningStats();
            if (stats == null) {
                stats = svc.initLearningStats(managed);
            }
            stats.setTotalXP(xpSpinner.getValue());
            stats.setWordsLearned(wordsSpinner.getValue());
            stats.setTotalMinutesStudied(minutesSpinner.getValue());
            svc.updateLearningStats(stats);

            closeStage();
            if (onSaved != null) onSaved.run();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void commitSpinner(Spinner<Integer> spinner) {
        try {
            spinner.getValueFactory().setValue(
                Integer.parseInt(spinner.getEditor().getText().trim()));
        } catch (NumberFormatException ignored) { /* keep current value */ }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeStage() {
        ((Stage) xpSpinner.getScene().getWindow()).close();
    }

    // AI-FEATURE: stats-summary ────────────────────────────────────────────────

    @FXML
    private void handleGenerateStats(ActionEvent event) {
        generateStatsBtn.setDisable(true);
        summaryLabel.setText("Generating summary…");
        summaryLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#6b7280;"
            + "-fx-background-color:#f9fafb; -fx-padding:8;"
            + "-fx-background-radius:6;");
        summaryLabel.setVisible(true);
        summaryLabel.setManaged(true);

        Thread t = new Thread(() -> {
            try {
                AdminAiService svc = new AdminAiService(new UserRepository());
                String result = svc.generateStatsSummary(user.getId());
                Platform.runLater(() -> {
                    summaryLabel.setText(result);
                    summaryLabel.setStyle(
                        "-fx-font-size:12px; -fx-text-fill:#374151;"
                        + "-fx-background-color:#f3f4f6; -fx-padding:8;"
                        + "-fx-background-radius:6;");
                    generateStatsBtn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    summaryLabel.setText(
                        "AI service unavailable. Make sure Ollama is running.");
                    summaryLabel.setStyle(
                        "-fx-font-size:11px; -fx-text-fill:#ef4444;"
                        + "-fx-background-color:#fef2f2; -fx-padding:8;"
                        + "-fx-background-radius:6;");
                    generateStatsBtn.setDisable(false);
                });
            }
        }, "stats-summary-thread");
        t.setDaemon(true);
        t.start();
    }
}
