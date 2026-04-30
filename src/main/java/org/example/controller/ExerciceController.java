package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.entities.Exercice;
import org.example.entities.Quiz;
import org.example.service.ExerciceService;
import org.example.service.QuizService;
import org.example.service.StripeService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExerciceController {

    @FXML private FlowPane exerciceGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<Quiz> filterQuiz;
    @FXML private ComboBox<String> comboSort;
    @FXML private Label labelTotalCount;
    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statAI;
    @FXML private Button btnAddNew;
    @FXML private Label headerTitle;
    @FXML private Label headerSubtitle;

    private final ExerciceService exerciceService = new ExerciceService();
    private final QuizService quizService = new QuizService();
    private final StripeService stripeService = new StripeService();
    private final ObservableList<Exercice> exerciceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (!org.example.util.UserSession.isAdmin) {
            if (btnAddNew != null) {
                btnAddNew.setVisible(false);
                btnAddNew.setManaged(false);
            }
            if (headerTitle != null) headerTitle.setText("Exercise Catalog");
            if (headerSubtitle != null) headerSubtitle.setText("Explore and practice individual exercises");
        }
        
        loadData();
        setupFilters();

        searchField.textProperty().addListener((obs, oldV, newV) -> filterExercices());
        filterType.valueProperty().addListener((obs, oldV, newV) -> filterExercices());
        filterQuiz.valueProperty().addListener((obs, oldV, newV) -> filterExercices());
        if (comboSort != null) {
            comboSort.valueProperty().addListener((obs, oldV, newV) -> filterExercices());
        }
    }

    private void loadData() {
        exerciceList.setAll(exerciceService.getAllExercices());
        filterQuiz.setItems(FXCollections.observableArrayList(quizService.getAllQuizzes()));
        updateStats();
        renderGrid(exerciceList);
    }

    private void updateStats() {
        long total = exerciceList.size();
        long active = exerciceList.stream().filter(Exercice::isEnabled).count();
        long ai = exerciceList.stream().filter(Exercice::isAiGenerated).count();

        statTotal.setText(String.valueOf(total));
        statActive.setText(String.valueOf(active));
        statAI.setText(String.valueOf(ai));
    }

    private void setupFilters() {
        filterType.setItems(FXCollections.observableArrayList("All Types", "multiple_choice", "true_false", "fill_blank", "matching"));
        filterType.setValue("All Types");
        
        if (comboSort != null) {
            comboSort.setItems(FXCollections.observableArrayList(
                "Latest Added", "Oldest Added", "Difficulty (Low to High)", "Difficulty (High to Low)"
            ));
            comboSort.setValue("Latest Added");
        }
    }

    private void renderGrid(List<Exercice> exercises) {
        exerciceGrid.getChildren().clear();
        for (Exercice ex : exercises) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciceCard.fxml"));
                Parent card = loader.load();
                ExerciceCardController ctrl = loader.getController();
                
                if (org.example.util.UserSession.isAdmin) {
                    ctrl.setData(ex, this::viewExercice, this::goToEditForm, this::confirmDelete);
                } else {
                    ctrl.setData(ex, this::viewExercice, null, null);
                }
                
                exerciceGrid.getChildren().add(card);
            } catch (IOException e) { e.printStackTrace(); }
        }
        labelTotalCount.setText("Showing " + exercises.size() + " of " + exerciceList.size() + " exercises");
    }

    private void filterExercices() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String type = filterType.getValue();
        Quiz quiz = filterQuiz.getValue();

        List<Exercice> filtered = exerciceList.stream()
            .filter(ex -> ex.getQuestion().toLowerCase().contains(search))
            .filter(ex -> {
                if (type == null || "All Types".equals(type)) return true;
                return type.equals(ex.getType());
            })
            .filter(ex -> quiz == null || ex.getQuizId() == quiz.getId())
            .collect(Collectors.toList());

        if (comboSort != null && comboSort.getValue() != null) {
            switch (comboSort.getValue()) {
                case "Latest Added":
                    filtered.sort((e1, e2) -> Integer.compare(e2.getId(), e1.getId()));
                    break;
                case "Oldest Added":
                    filtered.sort((e1, e2) -> Integer.compare(e1.getId(), e2.getId()));
                    break;
                case "Difficulty (Low to High)":
                    filtered.sort((e1, e2) -> Integer.compare(e1.getDifficulty(), e2.getDifficulty()));
                    break;
                case "Difficulty (High to Low)":
                    filtered.sort((e1, e2) -> Integer.compare(e2.getDifficulty(), e1.getDifficulty()));
                    break;
            }
        }

        renderGrid(filtered);
    }

    @FXML
    private void goToCreateForm() { navigateToForm(null); }
    private void goToEditForm(Exercice ex) { navigateToForm(ex); }
    
    private void viewExercice(Exercice ex) {
        if (ex.getDifficulty() >= 4 && !org.example.util.UserSession.isAdmin) {
            int price = (ex.getDifficulty() == 4) ? 10 : 20;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Premium Content");
            alert.setHeaderText("Premium Exercise Detected");
            alert.setContentText("This is a premium exercise (Difficulty " + ex.getDifficulty() + ").\n" +
                    "To view details, you must pay " + price + " USD via Stripe.\n\n" +
                    "Would you like to proceed to payment?");

            ButtonType payButton = new ButtonType("Pay Now (" + price + "$)");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
            alert.getButtonTypes().setAll(payButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == payButton) {
                String checkoutUrl = stripeService.createCheckoutSession(price, "Premium Exercise (Difficulty " + ex.getDifficulty() + ")");
                if (checkoutUrl != null) {
                    try {
                        Desktop.getDesktop().browse(new URI(checkoutUrl));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Alert urlAlert = new Alert(Alert.AlertType.INFORMATION);
                        urlAlert.setTitle("Payment Link");
                        urlAlert.setHeaderText("Redirect failed");
                        urlAlert.setContentText("Please copy this link to pay:\n" + checkoutUrl);
                        urlAlert.showAndWait();
                    }
                }
                return;
            } else {
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciceDetailView.fxml"));
            Parent root = loader.load();
            ExerciceDetailController ctrl = loader.getController();
            ctrl.setExercice(ex);
            Stage stage = (Stage) exerciceGrid.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void navigateToForm(Exercice ex) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExerciceForm.fxml"));
            Parent root = loader.load();
            ExerciceFormController ctrl = loader.getController();
            ctrl.setExercice(ex);
            Stage stage = (Stage) exerciceGrid.getScene().getWindow();
            Scene scene = stage.getScene();
            scene.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void confirmDelete(Exercice ex) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete exercise: \"" + ex.getQuestion() + "\"?\nThis action cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Exercise");
        alert.setHeaderText("⚠️  Confirm Deletion");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                exerciceService.deleteExercice(ex.getId());
                loadData();
            }
        });
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        filterType.setValue("All Types");
        filterQuiz.setValue(null);
    }
}
