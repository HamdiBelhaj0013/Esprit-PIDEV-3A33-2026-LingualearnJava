package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class FrontofficeController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnMyCourses;

    @FXML
    public void initialize() {
        FrontRouter.setContentPane(contentPane);
        activate(btnMyCourses);
        showLanguages();
    }

    @FXML
    private void showDashboard() {
        activate(btnDashboard);
        FrontRouter.goTo("/views/frontoffice/front-stats.fxml");
    }

    @FXML
    private void showLanguages() {
        activate(btnMyCourses);
        FrontRouter.goTo("/views/frontoffice/front-languages.fxml");
    }

    @FXML
    private void showMyCourses() {
        activate(btnMyCourses);
        FrontRouter.goTo("/views/frontoffice/front-languages.fxml");
    }

    private void activate(Button activeButton) {
        if (btnDashboard != null) {
            btnDashboard.getStyleClass().remove("sidebar-link-active");
        }
        if (btnMyCourses != null) {
            btnMyCourses.getStyleClass().remove("sidebar-link-active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("sidebar-link-active")) {
            activeButton.getStyleClass().add("sidebar-link-active");
        }
    }
}