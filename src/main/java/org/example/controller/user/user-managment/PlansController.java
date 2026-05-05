package org.example.controller.user;

import com.stripe.exception.StripeException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import org.example.entity.User;
import org.example.service.StripeService;
import org.example.service.UserService;
import org.example.util.SessionManager;

import java.time.format.DateTimeFormatter;

public class PlansController {

    @FXML private ScrollPane        rootPane;
    @FXML private HBox              activePlanBanner;
    @FXML private Label             activePlanName;
    @FXML private Label             activePlanExpiry;
    @FXML private Button            freeBtn;
    @FXML private Button            monthlyBtn;
    @FXML private Button            yearlyBtn;
    @FXML private Button            cancelBtn;
    @FXML private Label             cancelNoteLabel;
    @FXML private Label             statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @FXML
    public void initialize() {
        rootPane.getStylesheets().addAll(
            getClass().getResource("/css/user.css").toExternalForm(),
            getClass().getResource("/css/admin.css").toExternalForm()
        );
    }

    // ── Data load ─────────────────────────────────────────────────────────────

    public void loadData() {
        User cached = SessionManager.getCurrentUser();
        if (cached == null) return;

        // Always fetch a fresh copy from DB
        User user = new UserService().findById(cached.getId()).orElse(cached);

        if (user.isPremium()) {
            // Active plan banner
            activePlanBanner.setVisible(true);
            activePlanBanner.setManaged(true);
            activePlanName.setText("MONTHLY".equals(user.getSubscriptionPlan())
                ? "Monthly Premium" : "Yearly Premium");
            if (user.getSubscriptionExpiry() != null) {
                activePlanExpiry.setText("Renews " +
                    user.getSubscriptionExpiry().format(DATE_FMT));
            }

            // Cancel controls
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
            if (user.getSubscriptionExpiry() != null) {
                cancelNoteLabel.setText("Cancelling keeps access until " +
                    user.getSubscriptionExpiry().format(DATE_FMT) + ".");
            }
            cancelNoteLabel.setVisible(true);
            cancelNoteLabel.setManaged(true);

            // Highlight current plan button, offer switch on the other
            if ("MONTHLY".equals(user.getSubscriptionPlan())) {
                monthlyBtn.setText("Current Plan");
                monthlyBtn.setDisable(true);
                yearlyBtn.setText("Switch to Yearly ->");
            } else {
                yearlyBtn.setText("Current Plan");
                yearlyBtn.setDisable(true);
                monthlyBtn.setText("Switch to Monthly");
            }
        } else {
            // Free user
            activePlanBanner.setVisible(false);
            activePlanBanner.setManaged(false);
            freeBtn.setText("Current Plan");
            freeBtn.setDisable(true);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
        }
    }

    // ── Free button ───────────────────────────────────────────────────────────

    @FXML
    private void handleSelectFree(ActionEvent event) {
        showStatus("You are already on the Free plan.", false);
    }

    // ── Subscribe / switch — Monthly ──────────────────────────────────────────

    @FXML
    private void handleSubscribeMonthly(ActionEvent event) {
        startCheckout("MONTHLY");
    }

    // ── Subscribe / switch — Yearly ───────────────────────────────────────────

    @FXML
    private void handleSubscribeYearly(ActionEvent event) {
        startCheckout("YEARLY");
    }

    // ── Cancel subscription ───────────────────────────────────────────────────

    @FXML
    private void handleCancel(ActionEvent event) {
        User user = freshUser();
        if (user == null) return;

        String expiryText = user.getSubscriptionExpiry() != null
            ? user.getSubscriptionExpiry().format(DATE_FMT)
            : "the end of the billing period";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Subscription");
        confirm.setHeaderText("Cancel your subscription?");
        confirm.setContentText(
            "Your subscription will remain active until the end of the billing period.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                runCancelTask(user, expiryText);
            }
        });
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void startCheckout(String plan) {
        User user = freshUser();
        if (user == null) return;

        showLoading(true);
        hideStatus();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return StripeService.createCheckoutSession(user, plan);
            }
        };

        task.setOnSucceeded(e -> {
            String url = task.getValue();
            Platform.runLater(() -> {
                showLoading(false);
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    showStatus("Could not open browser: " + ex.getMessage(), true);
                }
            });
        });

        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            String msg = cause instanceof StripeException se
                ? se.getMessage()
                : (cause != null ? cause.getMessage() : "Unknown error");
            Platform.runLater(() -> {
                showLoading(false);
                showStatus("Stripe error: " + msg, true);
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void runCancelTask(User user, String expiryText) {
        showLoading(true);
        hideStatus();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                StripeService.cancelAtPeriodEnd(user);
                return null;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            showLoading(false);
            showStatus("Subscription cancelled. Access continues until " + expiryText + ".", false);
        }));

        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            String msg = cause instanceof StripeException se
                ? se.getMessage()
                : (cause != null ? cause.getMessage() : "Unknown error");
            Platform.runLater(() -> {
                showLoading(false);
                showStatus("Could not cancel subscription: " + msg, true);
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private User freshUser() {
        User cached = SessionManager.getCurrentUser();
        if (cached == null) return null;
        return new UserService().findById(cached.getId()).orElse(cached);
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
        loadingIndicator.setManaged(show);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #d63939;"
            : "-fx-text-fill: #2fb344;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}
