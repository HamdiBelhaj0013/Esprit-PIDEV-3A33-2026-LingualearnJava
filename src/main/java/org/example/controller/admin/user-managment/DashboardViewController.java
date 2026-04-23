package org.example.controller.admin;

import org.example.entity.User;
import org.example.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardViewController {

    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label suspendedLabel;
    @FXML private Label premiumLabel;
    @FXML private Label newThisMonthLabel;

    @FXML private TableView<User>              recentTable;
    @FXML private TableColumn<User, String>    colName;
    @FXML private TableColumn<User, String>    colEmail;
    @FXML private TableColumn<User, String>    colStatus;
    @FXML private TableColumn<User, String>    colPlan;
    @FXML private TableColumn<User, LocalDateTime> colJoined;

    public void load() {
        UserService svc = new UserService();

        long total     = svc.countAll();
        long active    = svc.countByStatus("active");
        long suspended = svc.countByStatus("suspended");
        long premium   = svc.countPremium();

        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1)
                                           .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<User> allUsers = svc.getAllUsers();
        long newMonth = allUsers.stream()
            .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(start))
            .count();

        totalUsersLabel.setText(String.valueOf(total));
        activeUsersLabel.setText(String.valueOf(active));
        suspendedLabel.setText(String.valueOf(suspended));
        premiumLabel.setText(String.valueOf(premium));
        newThisMonthLabel.setText(String.valueOf(newMonth));

        List<User> recent = allUsers.stream()
            .sorted((a, b) -> {
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            })
            .limit(10)
            .toList();

        setupTable();
        recentTable.setItems(FXCollections.observableArrayList(recent));
    }

    private void setupTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        colName.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getFullName()));

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

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

        colPlan.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getSubscriptionPlan()));

        colJoined.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colJoined.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText(empty || dt == null ? null : fmt.format(dt));
            }
        });
    }
}
