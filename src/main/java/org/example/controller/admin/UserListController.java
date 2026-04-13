package org.example.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.LearningStats;
import org.example.entity.User;
import org.example.service.UserService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UserListController {

    // ── FXML refs ──────────────────────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> roleFilter;
    @FXML private ComboBox<String> planFilter;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Boolean> colSelect;
    @FXML private TableColumn<User, Long>    colId;
    @FXML private TableColumn<User, String>  colName;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colStatus;
    @FXML private TableColumn<User, String>  colPlan;
    @FXML private TableColumn<User, Boolean> colPremium;
    @FXML private TableColumn<User, LocalDateTime> colJoined;
    @FXML private TableColumn<User, Void>    colActions;

    @FXML private Label  countLabel;
    @FXML private Label  pageLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Button bulkActivateBtn;
    @FXML private Button bulkSuspendBtn;
    @FXML private Button bulkDeleteBtn;

    // ── State ──────────────────────────────────────────────────────────────────
    private AdminMainController      mainController;
    private EntityManagerFactory     emf;

    private final Set<Long>          selected  = new HashSet<>();
    private int                      page      = 1;
    private static final int         PAGE_SIZE = 20;
    private long                     totalCount = 0;

    // ── Init ───────────────────────────────────────────────────────────────────

    public void setMainController(AdminMainController mc)  { this.mainController = mc; }
    public void setEntityManagerFactory(EntityManagerFactory e) { this.emf = e; }

    public void load() {
        initFilters();
        setupColumns();
        refresh();
    }

    private void initFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All", "active", "suspended", "deleted"));
        statusFilter.getSelectionModel().selectFirst();

        roleFilter.setItems(FXCollections.observableArrayList(
            "All", "ROLE_USER", "ROLE_ADMIN", "ROLE_TEACHER"));
        roleFilter.getSelectionModel().selectFirst();

        planFilter.setItems(FXCollections.observableArrayList(
            "All", "FREE", "MONTHLY", "YEARLY"));
        planFilter.getSelectionModel().selectFirst();

        sortCombo.setItems(FXCollections.observableArrayList(
            "Recent", "Name A→Z", "Email A→Z", "Status", "Premium first"));
        sortCombo.getSelectionModel().selectFirst();
    }

    // ── Columns ────────────────────────────────────────────────────────────────

    private void setupColumns() {
        userTable.setEditable(true);

        // Checkbox
        colSelect.setCellValueFactory(c -> {
            User user = c.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(selected.contains(user.getId()));
            prop.addListener((obs, oldVal, newVal) -> {
                if (newVal) selected.add(user.getId());
                else        selected.remove(user.getId());
                updateBulkButtons();
            });
            return prop;
        });
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));
        colSelect.setEditable(true);

        // ID
        colId.setCellValueFactory(c ->
            new javafx.beans.property.SimpleLongProperty(c.getValue().getId()).asObject());

        // Full name
        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getFullName()));

        // Email
        colEmail.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getEmail()));

        // Status badge
        colStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("badge-" + s);
                setGraphic(badge);
                setText(null);
            }
        });

        // Plan
        colPlan.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSubscriptionPlan()));
        colPlan.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("FREE".equals(s) ? "badge-free" : "badge-premium");
                setGraphic(badge);
                setText(null);
            }
        });

        // Premium
        colPremium.setCellValueFactory(c ->
            new SimpleBooleanProperty(c.getValue().isPremium()).asObject());
        colPremium.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setText(null); return; }
                setText(b ? "★ Yes" : "—");
                setStyle(b ? "-fx-text-fill: #856404; -fx-font-weight: bold;"
                           : "-fx-text-fill: #adb5bd;");
            }
        });

        // Joined date
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        colJoined.setCellValueFactory(c ->
            new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getCreatedAt()));
        colJoined.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : fmt.format(dt));
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn   = new Button("View");
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(4, viewBtn, editBtn, deleteBtn);

            {
                viewBtn.getStyleClass().addAll("btn-outline", "btn-sm");
                editBtn.getStyleClass().addAll("btn-outline", "btn-sm");
                deleteBtn.getStyleClass().addAll("btn-danger", "btn-sm");

                viewBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleViewUser(u);
                });
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleEditUser(u);
                });
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleDeleteUser(u);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ── Data loading ───────────────────────────────────────────────────────────

    private void refresh() {
        selected.clear();
        updateBulkButtons();

        String search = searchField.getText().trim();
        String status = filterValue(statusFilter);
        String role   = filterValue(roleFilter);
        String plan   = filterValue(planFilter);
        String sort   = sortKey();

        EntityManager em = emf.createEntityManager();
        try {
            UserService svc = new UserService(em);
            totalCount = svc.countAdvanced(search, status, role, plan);
            List<User> users = svc.findAdvanced(search, status, role, plan, sort, page, PAGE_SIZE);

            userTable.setItems(FXCollections.observableArrayList(users));
            updatePagination();
        } finally {
            em.close();
        }
    }

    private String filterValue(ComboBox<String> cb) {
        String v = cb.getValue();
        return (v == null || v.equals("All")) ? null : v;
    }

    private String sortKey() {
        String s = sortCombo.getValue();
        if (s == null) return null;
        return switch (s) {
            case "Name A→Z"     -> "name";
            case "Email A→Z"    -> "email";
            case "Status"       -> "status";
            case "Premium first"-> "premium";
            default             -> null;
        };
    }

    private void updatePagination() {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        pageLabel.setText("Page " + page + " of " + totalPages);
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
        countLabel.setText(totalCount + " user" + (totalCount != 1 ? "s" : "") + " found");
    }

    private void updateBulkButtons() {
        boolean hasSelection = !selected.isEmpty();
        bulkActivateBtn.setDisable(!hasSelection);
        bulkSuspendBtn.setDisable(!hasSelection);
        bulkDeleteBtn.setDisable(!hasSelection);
    }

    // ── FXML handlers ──────────────────────────────────────────────────────────

    @FXML private void handleApplyFilters(ActionEvent e) { page = 1; refresh(); }
    @FXML private void handleClearFilters(ActionEvent e) {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        roleFilter.getSelectionModel().selectFirst();
        planFilter.getSelectionModel().selectFirst();
        sortCombo.getSelectionModel().selectFirst();
        page = 1;
        refresh();
    }
    @FXML private void handleRefresh(ActionEvent e)  { refresh(); }
    @FXML private void handlePrevPage(ActionEvent e) { if (page > 1) { page--; refresh(); } }
    @FXML private void handleNextPage(ActionEvent e) {
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        if (page < totalPages) { page++; refresh(); }
    }

    @FXML private void handleCreate(ActionEvent e) {
        mainController.openCreateUser(this::refresh);
    }

    // ── Per-row actions ───────────────────────────────────────────────────────

    private void handleViewUser(User user) {
        user = reloadUser(user.getId());
        if (user != null) mainController.openUserDetail(user);
    }

    private void handleEditUser(User user) {
        user = reloadUser(user.getId());
        if (user != null) {
            User finalUser = user;
            mainController.openEditUser(finalUser, this::refresh);
        }
    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete " + user.getFullName() + "?\nThis cannot be undone.",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Deletion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                EntityManager em = emf.createEntityManager();
                try {
                    UserService svc = new UserService(em);
                    svc.findById(user.getId()).ifPresent(svc::deleteUser);
                    refresh();
                } catch (Exception ex) {
                    showError("Delete failed: " + ex.getMessage());
                } finally {
                    em.close();
                }
            }
        });
    }

    // ── Bulk actions ──────────────────────────────────────────────────────────

    @FXML private void handleBulkActivate(ActionEvent e) {
        applyBulkAction("Activate", u -> {
            EntityManager em = emf.createEntityManager();
            try { new UserService(em).activateUser(u); }
            finally { em.close(); }
        });
    }

    @FXML private void handleBulkSuspend(ActionEvent e) {
        applyBulkAction("Suspend", u -> {
            EntityManager em = emf.createEntityManager();
            try { new UserService(em).suspendUser(u); }
            finally { em.close(); }
        });
    }

    @FXML private void handleBulkDelete(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete " + selected.size() + " selected user(s)? This cannot be undone.",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Confirm Bulk Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                applyBulkAction("Delete", u -> {
                    EntityManager em = emf.createEntityManager();
                    try { new UserService(em).deleteUser(u); }
                    finally { em.close(); }
                });
            }
        });
    }

    private void applyBulkAction(String label, java.util.function.Consumer<User> action) {
        EntityManager em = emf.createEntityManager();
        try {
            UserService svc = new UserService(em);
            List<Long> ids = new ArrayList<>(selected);
            for (Long id : ids) {
                svc.findById(id).ifPresent(u -> {
                    try { action.accept(u); }
                    catch (Exception ex) { /* skip individual failures */ }
                });
            }
        } finally {
            em.close();
        }
        refresh();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User reloadUser(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return new UserService(em).findById(id).orElse(null);
        } finally {
            em.close();
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
