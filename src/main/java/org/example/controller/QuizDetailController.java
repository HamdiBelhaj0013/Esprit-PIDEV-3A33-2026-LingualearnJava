package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.service.ExerciceService;
import org.example.service.LessonService;
import org.example.service.StripeService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class QuizDetailController {

    @FXML private Label titleLabel;
    @FXML private Label lessonLabel;
    @FXML private Label descLabel;
    @FXML private Label diffLabel;
    @FXML private Label scoreLabel;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    private Quiz quiz;
    private final LessonService lessonService = new LessonService();
    private final ExerciceService exerciceService = new ExerciceService();
    private final StripeService stripeService = new StripeService();
    private final org.example.service.UserService userService = new org.example.service.UserService();
    private final org.example.service.EmailService emailService = new org.example.service.EmailService();

    public void setQuiz(Quiz quiz) {
        System.err.println("!!! QuizDetailController.setQuiz CALLED !!!");
        this.quiz = quiz;
        
        titleLabel.setText(quiz.getTitle());
        descLabel.setText(quiz.getDescription());
        scoreLabel.setText(quiz.getPassingScore() + "%");
        countLabel.setText(quiz.getQuestionCount() + " Questions");

        // Status
        if (quiz.isEnabled()) {
            statusLabel.setText("Active");
            statusLabel.setStyle("-fx-background-color: rgba(34, 197, 94, 0.1); -fx-text-fill: #16a34a; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold;");
        } else {
            statusLabel.setText("Inactive");
            statusLabel.setStyle("-fx-background-color: rgba(245, 158, 11, 0.1); -fx-text-fill: #b45309; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold;");
        }

        // Lesson
        if (quiz.getLessonId() != null) {
            lessonService.getAllLessons().stream()
                .filter(l -> l.getId() == quiz.getLessonId())
                .findFirst()
                .ifPresentOrElse(
                    l -> lessonLabel.setText("Linked to: " + l.getTitle()),
                    () -> lessonLabel.setText("Standalone Quiz")
                );
        } else {
            lessonLabel.setText("Standalone Quiz");
        }

        // Difficulty
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < quiz.getDifficulty(); i++) stars.append("⭐");
        for (int i = quiz.getDifficulty(); i < 5; i++) stars.append("☆");
        diffLabel.setText(stars.toString());
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startQuiz() {
        if (quiz == null) return;

        // 1. BYPASS if user is ADMIN or PREMIUM
        boolean isPremiumUser = org.example.util.UserSession.currentUser != null && org.example.util.UserSession.currentUser.isPremium();
        if (org.example.util.UserSession.isAdmin || isPremiumUser) {
            proceedToQuiz();
            return;
        }

        // 2. CHECK DIFFICULTY
        int quizDiff = quiz.getDifficulty();
        List<Exercice> exercices = exerciceService.getExercicesByQuizId(quiz.getId());
        int maxExDiff = (exercices != null && !exercices.isEmpty()) 
                ? exercices.stream().mapToInt(Exercice::getDifficulty).max().orElse(0) 
                : 0;
        int finalDiff = Math.max(quizDiff, maxExDiff);

        if (finalDiff >= 4) {
            int price = (finalDiff == 4) ? 10 : 20;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Premium Access Required");
            alert.setHeaderText("Level " + finalDiff + " Content");
            alert.setContentText("This is a premium quiz.\nPrice: " + price + " USD\n\nRedirect to Stripe?");

            ButtonType payButton = new ButtonType("Pay with Stripe");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(payButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == payButton) {
                String checkoutUrl = stripeService.createCheckoutSession(price, "Premium Quiz: " + quiz.getTitle());
                if (checkoutUrl != null) {
                    try {
                        Desktop.getDesktop().browse(new URI(checkoutUrl));
                        
                        // SIMULATE PAYMENT VERIFICATION DIALOG
                        Alert confirmAlert = new Alert(Alert.AlertType.INFORMATION);
                        confirmAlert.setTitle("Payment Verification");
                        confirmAlert.setHeaderText("Waiting for payment...");
                        confirmAlert.setContentText("After completing the payment in your browser, click OK to activate your access.");
                        
                        confirmAlert.showAndWait();
                        
                        // UPDATE USER IN DB
                        if (org.example.util.UserSession.currentUser != null) {
                            int userId = org.example.util.UserSession.currentUser.getId();
                            userService.setPremiumStatus(userId, true);
                            org.example.util.UserSession.currentUser.setPremium(true);
                            
                            // SEND EMAIL
                            emailService.sendPaymentConfirmation(
                                org.example.util.UserSession.currentUser.getEmail(),
                                org.example.util.UserSession.currentUser.getFirstName(),
                                "Premium Access (Level " + finalDiff + ")",
                                price
                            );

                            // NOTIFY USER
                            Alert emailAlert = new Alert(Alert.AlertType.INFORMATION);
                            emailAlert.setTitle("Success");
                            emailAlert.setHeaderText("Access Activated!");
                            emailAlert.setContentText("A confirmation email has been sent to: " + org.example.util.UserSession.currentUser.getEmail());
                            emailAlert.showAndWait();

                            // NOW PROCEED
                            proceedToQuiz();
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return;
            } else {
                return;
            }
        }

        proceedToQuiz();
    }

    private void proceedToQuiz() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/QuizPlayView.fxml"));
            Parent root = loader.load();
            QuizPlayController controller = loader.getController();
            controller.setQuiz(quiz);
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
