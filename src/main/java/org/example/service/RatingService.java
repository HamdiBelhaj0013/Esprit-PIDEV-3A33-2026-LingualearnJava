package org.example.service;

import org.example.entities.Rating;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RatingService {
    private final Connection connection;

    public RatingService() {
        this.connection = DatabaseConnection.getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS rating_exercice (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "exercice_id INT NOT NULL," +
                "value INT NOT NULL," +
                "UNIQUE KEY unique_user_exercice (user_id, exercice_id)" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating rating table: " + e.getMessage());
        }
    }

    public void addOrUpdateRating(Rating rating) {
        String sql = "INSERT INTO rating_exercice (user_id, exercice_id, value) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, rating.getUserId());
            stmt.setInt(2, rating.getExerciceId());
            stmt.setInt(3, rating.getValue());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving rating: " + e.getMessage());
        }
    }

    public double getAverageRating(int exerciceId) {
        String sql = "SELECT AVG(value) as average FROM rating_exercice WHERE exercice_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, exerciceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("average");
            }
        } catch (SQLException e) {
            System.err.println("Error getting average rating: " + e.getMessage());
        }
        return 0.0;
    }

    public int getUserRating(int userId, int exerciceId) {
        String sql = "SELECT value FROM rating_exercice WHERE user_id = ? AND exercice_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, exerciceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("value");
            }
        } catch (SQLException e) {
            System.err.println("Error getting user rating: " + e.getMessage());
        }
        return 0;
    }
}
