package controllers.backoffice;

import entities.Publication;
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
import services.ServicePublication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PublicationBackofficeController {
    @FXML private TableView<Publication> publicationTable;
    @FXML private TableColumn<Publication, Integer> colId;
    @FXML private TableColumn<Publication, String> colTitre;
    @FXML private TableColumn<Publication, String> colType;
    @FXML private TableColumn<Publication, Integer> colLikes;
    @FXML private TableColumn<Publication, Integer> colDislikes;

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
    private final ObservableList<Publication> masterData = FXCollections.observableArrayList();
    private final FilteredList<Publication> filteredData = new FilteredList<>(masterData, p -> true);
    private List<Publication> pagedSource = new ArrayList<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titrePub"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typePub"));
        colLikes.setCellValueFactory(new PropertyValueFactory<>("likes"));
        colDislikes.setCellValueFactory(new PropertyValueFactory<>("dislikes"));

        filterTypeCombo.setItems(FXCollections.observableArrayList("Tous", "post", "story", "Article"));
        filterTypeCombo.getSelectionModel().selectFirst();

        pageSizeComboBox.setItems(FXCollections.observableArrayList(5, 10, 15, 20));
        pageSizeComboBox.getSelectionModel().select(Integer.valueOf(10));

        searchTitreField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterTypeCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
        minLikesField.textProperty().addListener((obs, o, n) -> applyFilters());

        publicationTable.comparatorProperty().addListener((obs, o, n) -> rebuildPagination());
        pagination.currentPageIndexProperty().addListener((obs, o, n) -> showPage(n.intValue()));
        pageSizeComboBox.valueProperty().addListener((obs, o, n) -> rebuildPagination());

        refresh();
    }

    @FXML
    public void refresh() {
        try {
            masterData.setAll(servicePublication.getAll());
            updateKpis();
            applyFilters();
            setStatus("Liste publications actualisee.", false);
        } catch (Exception e) {
            showError("Chargement publications impossible", e.getMessage());
        }
    }

    private void updateKpis() {
        kpiTotalPublicationsLabel.setText(String.valueOf(masterData.size()));
        int likes = masterData.stream().mapToInt(Publication::getLikes).sum();
        int reports = masterData.stream().mapToInt(Publication::getReportPub).sum();
        long types = masterData.stream()
                .map(Publication::getTypePub)
                .filter(t -> t != null && !t.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .count();

        kpiTotalLikesLabel.setText(String.valueOf(likes));
        kpiTotalReportsLabel.setText(String.valueOf(reports));
        kpiTypesLabel.setText(String.valueOf(types));
    }

    private void applyFilters() {
        String search = searchTitreField.getText() == null ? "" : searchTitreField.getText().trim().toLowerCase();
        String type = filterTypeCombo.getValue();
        int minLikes = parseIntOrDefault(minLikesField.getText(), 0);

        filteredData.setPredicate(pub -> {
            if (pub == null) {
                return false;
            }

            boolean matchTitre = search.isBlank()
                    || (pub.getTitrePub() != null && pub.getTitrePub().toLowerCase().contains(search));
            boolean matchType = type == null || "Tous".equalsIgnoreCase(type)
                    || (pub.getTypePub() != null && pub.getTypePub().equalsIgnoreCase(type));
            boolean matchLikes = pub.getLikes() >= minLikes;
            return matchTitre && matchType && matchLikes;
        });

        rebuildPagination();
    }

    private void rebuildPagination() {
        pagedSource = filteredData.stream().collect(Collectors.toList());
        Comparator<Publication> comparator = publicationTable.getComparator();
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
            publicationTable.setItems(FXCollections.observableArrayList());
            pageInfoLabel.setText("0-0 / " + pagedSource.size());
            return;
        }

        publicationTable.setItems(FXCollections.observableArrayList(pagedSource.subList(from, to)));
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
        searchTitreField.clear();
        filterTypeCombo.getSelectionModel().selectFirst();
        minLikesField.clear();
        applyFilters();
        setStatus("Filtres reinitialises.", false);
    }

    @FXML
    public void openAddPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/publication_new.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(publicationTable.getScene().getWindow());
            stage.setTitle("Ajouter Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture page ajout impossible", e.getMessage());
        }
    }

    @FXML
    public void openEditPage() {
        Publication selected = publicationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne une publication a modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/publication_edit.fxml"));
            Parent root = loader.load();
            PublicationEditController controller = loader.getController();
            controller.setPublication(selected);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(publicationTable.getScene().getWindow());
            stage.setTitle("Modifier Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refresh();
        } catch (Exception e) {
            showError("Ouverture page modification impossible", e.getMessage());
        }
    }

    @FXML
    public void openShowPage() {
        Publication selected = publicationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne une publication a afficher.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/backoffice/fxml/publication_show.fxml"));
            Parent root = loader.load();
            PublicationShowController controller = loader.getController();
            controller.setPublication(selected);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(publicationTable.getScene().getWindow());
            stage.setTitle("Details Publication");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Ouverture page details impossible", e.getMessage());
        }
    }

    @FXML
    public void deletePublication() {
        Publication selected = publicationTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selectionne une publication a supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer la publication ?");
        confirm.setContentText("ID " + selected.getId() + " - " + selected.getTitrePub());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            servicePublication.delete(selected.getId());
            refresh();
            setStatus("Publication supprimee.", false);
        } catch (Exception e) {
            showError("Suppression publication impossible", e.getMessage());
        }
    }

    @FXML
    public void clearSelection() {
        publicationTable.getSelectionModel().clearSelection();
        setStatus("Selection effacee.", false);
    }

    private int getPageSize() {
        Integer value = pageSizeComboBox.getValue();
        return value == null || value <= 0 ? 10 : value;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
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
        BackofficeNav.navigateToDashboard(publicationTable);
    }
}

