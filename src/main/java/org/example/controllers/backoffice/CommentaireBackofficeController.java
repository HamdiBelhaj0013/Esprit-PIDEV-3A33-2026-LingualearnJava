package org.example.controllers.backoffice;

import org.example.entities.Commentaire;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.services.ServiceCommentaire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CommentaireBackofficeController {
    @FXML private TableView<Commentaire> commentaireTable;
    @FXML private TableColumn<Commentaire, Integer> colId;
    @FXML private TableColumn<Commentaire, String> colContenu;
    @FXML private TableColumn<Commentaire, Integer> colPublicationId;

    @FXML private TextField searchContenuField;
    @FXML private TextField filterPublicationIdField;
    @FXML private Label statusLabel;

    @FXML private Label kpiTotalCommentairesLabel;
    @FXML private Label kpiPublicationsLieesLabel;
    @FXML private Label kpiAvgLengthLabel;

    @FXML private Pagination pagination;
    @FXML private ComboBox<Integer> pageSizeComboBox;
    @FXML private Label pageInfoLabel;

    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final ObservableList<Commentaire> masterData = FXCollections.observableArrayList();
    private final FilteredList<Commentaire> filteredData = new FilteredList<>(masterData, c -> true);
    private List<Commentaire> pagedSource = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenuC"));
        colPublicationId.setCellValueFactory(new PropertyValueFactory<>("publicationId"));

        pageSizeComboBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20));
        pageSizeComboBox.getSelectionModel().select(Integer.valueOf(10));

        searchContenuField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterPublicationIdField.textProperty().addListener((obs, o, n) -> applyFilters());
        commentaireTable.comparatorProperty().addListener((obs, o, n) -> rebuildPagination());
        pagination.currentPageIndexProperty().addListener((obs, o, n) -> showPage(n.intValue()));
        pageSizeComboBox.valueProperty().addListener((obs, o, n) -> rebuildPagination());

        Integer selectedPublicationId = BackofficeContext.getSelectedPublicationId();
        if (selectedPublicationId != null) {
            filterPublicationIdField.setText(String.valueOf(selectedPublicationId));
            BackofficeContext.setSelectedPublicationId(null);
        }

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            masterData.setAll(serviceCommentaire.getAll());
            updateKpis();
            applyFilters();
            setStatus("Liste commentaires actualisee.", false);
        } catch (Exception e) {
            showError("Chargement commentaires impossible", e.getMessage());
        }
    }

    private void updateKpis() {
        kpiTotalCommentairesLabel.setText(String.valueOf(masterData.size()));
        long distinctPub = masterData.stream().map(Commentaire::getPublicationId).distinct().count();
        double avg = masterData.stream()
                .map(Commentaire::getContenuC)
                .filter(s -> s != null)
                .mapToInt(String::length)
                .average()
                .orElse(0.0);

        kpiPublicationsLieesLabel.setText(String.valueOf(distinctPub));
        kpiAvgLengthLabel.setText(String.format("%.0f", avg));
    }

    private void applyFilters() {
        String search = searchContenuField.getText() == null ? "" : searchContenuField.getText().trim().toLowerCase();
        String pubIdFilter = filterPublicationIdField.getText() == null ? "" : filterPublicationIdField.getText().trim();

        filteredData.setPredicate(c -> {
            if (c == null) {
                return false;
            }

            boolean matchContenu = search.isBlank()
                    || (c.getContenuC() != null && c.getContenuC().toLowerCase().contains(search));

            boolean matchPub = true;
            if (!pubIdFilter.isBlank()) {
                try {
                    int pubId = Integer.parseInt(pubIdFilter);
                    matchPub = c.getPublicationId() == pubId;
                } catch (NumberFormatException e) {
                    matchPub = false;
                }
            }

            return matchContenu && matchPub;
        });

        rebuildPagination();
    }

    private void rebuildPagination() {
        pagedSource = filteredData.stream().collect(Collectors.toList());
        Comparator<Commentaire> comparator = commentaireTable.getComparator();
        if (comparator != null) {
            pagedSource.sort(comparator);
        }

        int pageSize = getPageSize();
        int pageCount = Math.max(1, (int) Math.ceil((double) pagedSource.size() / pageSize));
        pagination.setPageCount(pageCount);

        int current = Math.min(pagination.getCurrentPageIndex(), pageCount - 1);
        if (current < 0) {
            current = 0;
        }
        pagination.setCurrentPageIndex(current);
        showPage(current);
    }

    private void showPage(int pageIndex) {
        int pageSize = getPageSize();
        int from = pageIndex * pageSize;
        int to = Math.min(from + pageSize, pagedSource.size());

        if (from >= pagedSource.size()) {
            commentaireTable.setItems(FXCollections.observableArrayList());
            pageInfoLabel.setText("0-0 / " + pagedSource.size());
            return;
        }

        commentaireTable.setItems(FXCollections.observableArrayList(pagedSource.subList(from, to)));
        pageInfoLabel.setText((from + 1) + "-" + to + " / " + pagedSource.size());
    }

    @FXML
    public void goToFirstPage() {
        pagination.setCurrentPageIndex(0);
    }

    @FXML
    public void goToLastPage() {
        if (pagination.getPageCount() > 0) {
            pagination.setCurrentPageIndex(pagination.getPageCount() - 1);
        }
    }

    @FXML
    public void resetFilters() {
        searchContenuField.clear();
        filterPublicationIdField.clear();
        applyFilters();
        setStatus("Filtres reinitialises.", false);
    }

    @FXML
    public void openAddPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/commentaire_new.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(commentaireTable.getScene().getWindow());
            stage.setTitle("Ajouter Commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture page ajout impossible", e.getMessage());
        }
    }

    @FXML
    public void openEditPage() {
        Commentaire selected = commentaireTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne un commentaire a modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/commentaire_edit.fxml"));
            Parent root = loader.load();
            CommentaireEditController controller = loader.getController();
            controller.setCommentaire(selected);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(commentaireTable.getScene().getWindow());
            stage.setTitle("Modifier Commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture page modification impossible", e.getMessage());
        }
    }

    @FXML
    public void openShowPage() {
        Commentaire selected = commentaireTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne un commentaire a afficher.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/commentaire_show.fxml"));
            Parent root = loader.load();
            CommentaireShowController controller = loader.getController();
            controller.setCommentaire(selected);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(commentaireTable.getScene().getWindow());
            stage.setTitle("Details Commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ouverture page details impossible", e.getMessage());
        }
    }

    @FXML
    public void deleteCommentaire() {
        Commentaire selected = commentaireTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne un commentaire a supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer le commentaire ?");
        confirm.setContentText("ID " + selected.getId());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            serviceCommentaire.delete(selected.getId());
            refresh();
            setStatus("Commentaire supprime.", false);
        } catch (Exception e) {
            showError("Suppression commentaire impossible", e.getMessage());
        }
    }

    @FXML
    public void clearSelection() {
        commentaireTable.getSelectionModel().clearSelection();
        setStatus("Selection effacee.", false);
    }

    private int getPageSize() {
        Integer value = pageSizeComboBox.getValue();
        return value == null || value <= 0 ? 10 : value;
    }

    private void setStatus(String text, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle(isError ? "-fx-text-fill: #b91c1c;" : "-fx-text-fill: #047857;");
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
        setStatus(header, true);
    }

    private void showWarning(String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(content);
        alert.showAndWait();
        setStatus(content, true);
    }

    @FXML
    public void goToDashboard() {
        BackofficeNav.navigateToDashboard(commentaireTable);
    }
}

