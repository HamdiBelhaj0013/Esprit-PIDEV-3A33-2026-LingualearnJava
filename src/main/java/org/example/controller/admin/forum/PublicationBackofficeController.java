package org.example.controller.admin.forum;

import org.example.entities.forum.Publication;
import org.example.entity.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.service.forum.ServicePublication;
import org.example.service.user_managment.UserService;
import org.example.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PublicationBackofficeController {

    @FXML private FlowPane cardsPane;
    @FXML private ScrollPane cardsScrollPane;

    @FXML private TextField searchTitreField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private TextField minLikesField;
    @FXML private Label statusLabel;

    @FXML private Label kpiTotalPublicationsLabel;
    @FXML private Label kpiTotalLikesLabel;
    @FXML private Label kpiTotalReportsLabel;
    @FXML private Label kpiTypesLabel;

    @FXML private Pagination pagination;
    @FXML private ComboBox<Integer> pageSizeComboBox;
    @FXML private Label pageInfoLabel;

    private final ServicePublication servicePublication = new ServicePublication();
    private final UserService userService = new UserService();
    private final ObservableList<Publication> masterData = FXCollections.observableArrayList();
    private final FilteredList<Publication> filteredData = new FilteredList<>(masterData, p -> true);
    private List<Publication> pagedSource = new ArrayList<>();
    private Publication selectedPublication = null;

    private static final String CARD_DEFAULT  =
            "-fx-background-color: white; -fx-background-radius: 16;" +
                    "-fx-border-color: #e2e8f0; -fx-border-radius: 16; -fx-border-width: 2;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);";
    private static final String CARD_SELECTED =
            "-fx-background-color: #eef2ff; -fx-background-radius: 16;" +
                    "-fx-border-color: #4f46e5; -fx-border-radius: 16; -fx-border-width: 2.5;" +
                    "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.3), 12, 0, 0, 4);";

    @FXML
    public void initialize() {
        filterTypeCombo.setItems(FXCollections.observableArrayList("Tous", "post", "story"));
        filterTypeCombo.getSelectionModel().selectFirst();

        pageSizeComboBox.setItems(FXCollections.observableArrayList(6, 9, 12, 18));
        pageSizeComboBox.getSelectionModel().selectFirst();

        searchTitreField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterTypeCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
        minLikesField.textProperty().addListener((obs, o, n) -> applyFilters());

        pagination.currentPageIndexProperty().addListener((obs, o, n) -> showPage(n.intValue()));
        pageSizeComboBox.valueProperty().addListener((obs, o, n) -> rebuildPagination());

        refresh();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER : ID de l'utilisateur connecté (-1 si absent)
    // ─────────────────────────────────────────────────────────────────────────
    private int getCurrentUserId() {
        try {
            if (SessionManager.getCurrentUser() != null
                    && SessionManager.getCurrentUser().getId() != null) {
                return SessionManager.getCurrentUser().getId().intValue();
            }
        } catch (Exception e) {
            System.err.println("[WARN] Impossible de lire l'ID de session : " + e.getMessage());
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER : vérifie si l'utilisateur connecté est administrateur
    // Rôles stockés en JSON dans User : ["ROLE_USER","ROLE_ADMIN"]
    // user.hasRole() parse ce JSON automatiquement.
    // ─────────────────────────────────────────────────────────────────────────
    private boolean currentUserIsAdmin() {
        try {
            var user = SessionManager.getCurrentUser();
            return user != null && user.hasRole("ROLE_ADMIN");
        } catch (Exception e) {
            System.err.println("[WARN] Vérification rôle impossible : " + e.getMessage());
            return false;
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // HELPER : nom complet de l'auteur d'une publication
    // ─────────────────────────────────────────────────────────────────────────
    private String getAuthorName(int utilisateurId) {
        if (utilisateurId <= 0) {
            return "Auteur inconnu";
        }
        try {
            User author = userService.findById((long) utilisateurId).orElse(null);
            if (author == null) return "Auteur inconnu";

            // Essayer getFullName() en premier
            String fullName = null;
            try { fullName = author.getFullName(); } catch (Exception ignored) {}

            if (fullName != null && !fullName.isBlank()) return fullName;

            // Fallback : prénom + nom
            String first = "";
            String last  = "";
            try { first = author.getFirstName() != null ? author.getFirstName() : ""; } catch (Exception ignored) {}
            try { last  = author.getLastName()  != null ? author.getLastName()  : ""; } catch (Exception ignored) {}
            String combined = (first + " " + last).trim();
            if (!combined.isBlank()) return combined;

            // Fallback : email
            try {
                String email = author.getEmail();
                if (email != null && !email.isBlank()) return email;
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("[WARN] Récupération auteur id=" + utilisateurId + " : " + e.getMessage());
        }
        return "Auteur inconnu";
    }

    @FXML
    public void refresh() {
        try {
            masterData.setAll(servicePublication.getAll());
            selectedPublication = null;
            updateKpis();
            applyFilters();
            setStatus("Liste actualisée.", false);
        } catch (Exception e) {
            showError("Chargement impossible", e.getMessage());
        }
    }

    private void updateKpis() {
        kpiTotalPublicationsLabel.setText(String.valueOf(masterData.size()));
        kpiTotalLikesLabel.setText(String.valueOf(
                masterData.stream().mapToInt(Publication::getLikes).sum()));
        kpiTotalReportsLabel.setText(String.valueOf(
                masterData.stream().mapToInt(Publication::getReportPub).sum()));
        long types = masterData.stream()
                .map(Publication::getTypePub)
                .filter(t -> t != null && !t.isBlank())
                .map(String::toLowerCase).distinct().count();
        kpiTypesLabel.setText(String.valueOf(types));
    }

    private void applyFilters() {
        String search  = searchTitreField.getText() == null ? "" : searchTitreField.getText().trim().toLowerCase();
        String type    = filterTypeCombo.getValue();
        int minLikes   = parseIntOrDefault(minLikesField.getText(), 0);

        filteredData.setPredicate(pub -> {
            if (pub == null) return false;
            boolean matchTitre = search.isBlank()
                    || (pub.getTitrePub() != null && pub.getTitrePub().toLowerCase().contains(search));
            boolean matchType  = type == null || "Tous".equalsIgnoreCase(type)
                    || (pub.getTypePub() != null && pub.getTypePub().equalsIgnoreCase(type));
            boolean matchLikes = pub.getLikes() >= minLikes;
            return matchTitre && matchType && matchLikes;
        });
        rebuildPagination();
    }

    private void rebuildPagination() {
        pagedSource = new ArrayList<>(filteredData);
        int pageSize  = getPageSize();
        int pageCount = Math.max(1, (int) Math.ceil((double) pagedSource.size() / pageSize));
        pagination.setPageCount(pageCount);
        int current = Math.min(pagination.getCurrentPageIndex(), pageCount - 1);
        if (current < 0) current = 0;
        pagination.setCurrentPageIndex(current);
        showPage(current);
    }

    private void showPage(int pageIndex) {
        int pageSize = getPageSize();
        int from = pageIndex * pageSize;
        int to   = Math.min(from + pageSize, pagedSource.size());
        cardsPane.getChildren().clear();

        if (from >= pagedSource.size()) {
            pageInfoLabel.setText("0-0 / " + pagedSource.size());
            return;
        }

        for (Publication pub : pagedSource.subList(from, to)) {
            cardsPane.getChildren().add(buildCard(pub));
        }
        pageInfoLabel.setText((from + 1) + "-" + to + " / " + pagedSource.size());
    }

    private VBox buildCard(Publication pub) {
        VBox card = new VBox(0);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setStyle(isSelected(pub) ? CARD_SELECTED : CARD_DEFAULT);
        card.setCursor(javafx.scene.Cursor.HAND);

        // ── Image banner ───────────────────────────────────────────────────
        StackPane imageBanner = new StackPane();
        imageBanner.setPrefHeight(170);
        imageBanner.setMinHeight(170);
        imageBanner.setMaxWidth(280);
        imageBanner.setStyle("-fx-background-radius: 14 14 0 0; -fx-background-color: #e2e8f0;");

        String lien = pub.getLienPub();
        if (lien != null && !lien.isBlank()) {
            try {
                String url = (lien.startsWith("http://") || lien.startsWith("https://") || lien.startsWith("file:"))
                        ? lien : "file:///" + lien.replace("\\", "/");
                Image img = new Image(url, 280, 170, true, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(280);
                iv.setFitHeight(170);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                Rectangle clip = new Rectangle(280, 170);
                clip.setArcWidth(28);
                clip.setArcHeight(28);
                iv.setClip(clip);
                imageBanner.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        // ── Type badge ─────────────────────────────────────────────────────
        String typePub = pub.getTypePub() == null ? "post" : pub.getTypePub();
        Label typeBadge = new Label(typePub.toUpperCase());
        typeBadge.setStyle(
                "post".equalsIgnoreCase(typePub)
                        ? "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 20;"
                        : "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 9; -fx-background-radius: 20;");
        StackPane.setAlignment(typeBadge, Pos.TOP_LEFT);
        StackPane.setMargin(typeBadge, new Insets(10, 0, 0, 10));
        imageBanner.getChildren().add(typeBadge);

        // ── Body ───────────────────────────────────────────────────────────
        VBox body = new VBox(6);
        body.setPadding(new Insets(12, 14, 10, 14));

        Label titre = new Label(pub.getTitrePub() != null ? pub.getTitrePub() : "(sans titre)");
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        titre.setWrapText(true);
        titre.setMaxWidth(252);

        // ✅ CORRIGÉ : nom de l'auteur via la méthode sécurisée getAuthorName()
        String authorName = getAuthorName(pub.getUtilisateurId());
        Label auteur = new Label("Par : " + authorName);
        auteur.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c3aed; -fx-font-weight: bold;");

        String preview = pub.getContenuPub() != null && !pub.getContenuPub().isBlank()
                ? (pub.getContenuPub().length() > 80
                ? pub.getContenuPub().substring(0, 77) + "…"
                : pub.getContenuPub())
                : "";
        Label contenu = new Label(preview);
        contenu.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        contenu.setWrapText(true);
        contenu.setMaxWidth(252);

        // ── Stats ──────────────────────────────────────────────────────────
        HBox stats = new HBox(14);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(6, 0, 4, 0));
        stats.getChildren().addAll(
                statChip("👍 " + pub.getLikes(),     "#dcfce7", "#15803d"),
                statChip("👎 " + pub.getDislikes(),  "#fef9c3", "#b45309"),
                statChip("🚨 " + pub.getReportPub(), "#fee2e2", "#b91c1c")
        );

        body.getChildren().addAll(titre, auteur, contenu, stats);

        // ── Action buttons ─────────────────────────────────────────────────
        //  ✅ CORRIGÉ : vérification du rôle ET de la propriété
        int currentUserId  = getCurrentUserId();
        boolean isAdmin    = currentUserIsAdmin();
        boolean isOwner    = currentUserId != -1 && pub.getUtilisateurId() == currentUserId;
        boolean canManage  = isAdmin || isOwner;   // admin TOUT, propriétaire ses propres pubs

        System.out.println("[DEBUG][CARD] pub.id=" + pub.getId()
                + "  pub.utilisateurId=" + pub.getUtilisateurId()
                + "  session.userId=" + currentUserId
                + "  isAdmin=" + isAdmin
                + "  isOwner=" + isOwner);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(2, 10, 12, 10));

        // 👁️ Voir – visible par tous
        Button btnVoir = actionBtn("👁️", "#3b82f6");
        btnVoir.setOnAction(e -> { selectCard(pub); openShowFor(pub); });
        actions.getChildren().add(btnVoir);

        // 💬 Commentaires – visible par tous
        Button btnComm = actionBtn("💬", "#0ea5e9");
        btnComm.setOnAction(e -> { selectCard(pub); openCommentairesFor(pub); });
        actions.getChildren().add(btnComm);

        if (canManage) {
            // ✏️ Modifier – uniquement propriétaire ou admin
            Button btnEdit = actionBtn("✏️", "#f59e0b");
            btnEdit.setOnAction(e -> { selectCard(pub); openEditFor(pub); });
            actions.getChildren().add(btnEdit);

            // 🗑️ Supprimer – uniquement propriétaire ou admin
            Button btnDel = actionBtn("🗑️", "#ef4444");
            btnDel.setOnAction(e -> { selectCard(pub); deleteFor(pub); });
            actions.getChildren().add(btnDel);
        }

        card.getChildren().addAll(imageBanner, body, actions);
        card.setOnMouseClicked(e -> selectCard(pub));

        return card;
    }

    private Label statChip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 20;");
        return l;
    }

    private Button actionBtn(String icon, String color) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color: " + color
                + "; -fx-text-fill: white; -fx-font-size: 13px;"
                + " -fx-padding: 6 12; -fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }

    private boolean isSelected(Publication pub) {
        return selectedPublication != null && selectedPublication.getId() == pub.getId();
    }

    private void selectCard(Publication pub) {
        selectedPublication = pub;
        showPage(pagination.getCurrentPageIndex());
        setStatus("Sélectionné : " + pub.getTitrePub(), false);
    }

    private void openShowFor(Publication pub) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/forum/fxml/publication_show.fxml"));
            Parent root = loader.load();
            PublicationShowController ctrl = loader.getController();
            ctrl.setPublication(pub);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsPane.getScene().getWindow());
            stage.setTitle("Détails Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ouverture détails impossible", e.getMessage());
        }
    }

    private void openEditFor(Publication pub) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/forum/fxml/publication_edit.fxml"));
            Parent root = loader.load();
            PublicationEditController ctrl = loader.getController();
            ctrl.setPublication(pub);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsPane.getScene().getWindow());
            stage.setTitle("Modifier Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture modification impossible", e.getMessage());
        }
    }

    private void openCommentairesFor(Publication pub) {
        try {
            BackofficeContext.setSelectedPublicationId(pub.getId());
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/forum/fxml/commentaire_manager.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsPane.getScene().getWindow());
            stage.setTitle("💬 Commentaires – " + pub.getTitrePub());
            stage.setScene(new Scene(root, 860, 620));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture commentaires impossible", e.getMessage());
        } finally {
            BackofficeContext.setSelectedPublicationId(null);
        }
    }

    private void deleteFor(Publication pub) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer cette publication ?");
        confirm.setContentText(pub.getTitrePub());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            servicePublication.delete(pub.getId());
            refresh();
            setStatus("Publication supprimée.", false);
        } catch (Exception e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    @FXML
    public void openAddPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/forum/fxml/publication_new.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cardsPane.getScene().getWindow());
            stage.setTitle("Ajouter Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture ajout impossible", e.getMessage());
        }
    }

    @FXML public void goToFirstPage() { pagination.setCurrentPageIndex(0); }

    @FXML
    public void goToLastPage() {
        if (pagination.getPageCount() > 0)
            pagination.setCurrentPageIndex(pagination.getPageCount() - 1);
    }

    @FXML
    public void resetFilters() {
        searchTitreField.clear();
        filterTypeCombo.getSelectionModel().selectFirst();
        minLikesField.clear();
        applyFilters();
        setStatus("Filtres réinitialisés.", false);
    }

    @FXML public void goToDashboard() { BackofficeNav.navigateToDashboard(cardsPane); }

    @FXML
    public void goToAdminMain() {
        try {
            BackofficeNav.navigate(cardsPane, "/fxml/admin/AdminMain.fxml", "LinguaLearn - Admin");
        } catch (java.io.IOException e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    private int getPageSize() {
        Integer v = pageSizeComboBox.getValue();
        return v == null || v <= 0 ? 6 : v;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception e) { return fallback; }
    }

    private void setStatus(String text, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle(isError
                    ? "-fx-text-fill: #b91c1c; -fx-font-size: 12px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #047857; -fx-font-size: 12px; -fx-font-weight: bold;");
        }
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
        setStatus(header, true);
    }
}