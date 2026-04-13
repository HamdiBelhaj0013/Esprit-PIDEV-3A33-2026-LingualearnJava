package org.example.controller.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;
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

    private EntityManagerFactory emf;

    public void setEntityManagerFactory(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void load() {
        EntityManager em = emf.createEntityManager();
        try {
            long total     = count(em, "SELECT COUNT(u) FROM User u");
            long active    = count(em, "SELECT COUNT(u) FROM User u WHERE u.status = 'active'");
            long suspended = count(em, "SELECT COUNT(u) FROM User u WHERE u.status = 'suspended'");
            long premium   = count(em, "SELECT COUNT(u) FROM User u WHERE u.isPremium = true");

            LocalDateTime start = LocalDateTime.now().withDayOfMonth(1)
                                               .withHour(0).withMinute(0).withSecond(0).withNano(0);
            long newMonth = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.createdAt >= :s", Long.class)
                .setParameter("s", start).getSingleResult();

            totalUsersLabel.setText(String.valueOf(total));
            activeUsersLabel.setText(String.valueOf(active));
            suspendedLabel.setText(String.valueOf(suspended));
            premiumLabel.setText(String.valueOf(premium));
            newThisMonthLabel.setText(String.valueOf(newMonth));

            List<User> recent = em.createQuery(
                "SELECT u FROM User u ORDER BY u.createdAt DESC", User.class)
                .setMaxResults(10).getResultList();

            setupTable();
            recentTable.setItems(FXCollections.observableArrayList(recent));
        } finally {
            em.close();
        }
    }

    private long count(EntityManager em, String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
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
