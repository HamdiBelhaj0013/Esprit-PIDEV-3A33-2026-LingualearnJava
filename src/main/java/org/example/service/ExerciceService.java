package org.example.service;

import org.example.entities.Exercice;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExerciceService {

    private Connection connection;

    public ExerciceService() {
        this.connection = DatabaseConnection.getConnection();
    }

    // 1. CREATE
    public void addExercice(Exercice exercice) {
        String sql = "INSERT INTO exercice (type, question, options, correct_answer, ai_generated, enabled, quiz_id, skill_codes, difficulty) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, exercice.getType());
            statement.setString(2, exercice.getQuestion());
            // Force un tableau JSON vide if empty/null
            statement.setString(3, (exercice.getOptions() == null || exercice.getOptions().trim().isEmpty()) ? "[]" : exercice.getOptions());
            statement.setString(4, exercice.getCorrectAnswer());
            statement.setBoolean(5, exercice.isAiGenerated());
            statement.setBoolean(6, exercice.isEnabled());
            statement.setInt(7, exercice.getQuizId());
            statement.setString(8, (exercice.getSkillCodes() == null || exercice.getSkillCodes().trim().isEmpty()) ? "[]" : exercice.getSkillCodes());
            statement.setInt(9, exercice.getDifficulty());

            statement.executeUpdate();
            
            // Mettre à jour l'ID de l'Exercice avec l'ID auto-généré par la base de données
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                exercice.setId(rs.getInt(1));
            }
            System.out.println("Exercice ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'exercice : " + e.getMessage());
        }
    }

    // 2. READ ALL
    public List<Exercice> getAllExercices() {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT * FROM exercice";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Exercice exercice = new Exercice();
                exercice.setId(resultSet.getInt("id"));
                exercice.setType(resultSet.getString("type"));
                exercice.setQuestion(resultSet.getString("question"));
                exercice.setOptions(resultSet.getString("options"));
                exercice.setCorrectAnswer(resultSet.getString("correct_answer"));
                exercice.setAiGenerated(resultSet.getBoolean("ai_generated"));
                exercice.setEnabled(resultSet.getBoolean("enabled"));
                exercice.setQuizId(resultSet.getInt("quiz_id"));
                exercice.setSkillCodes(resultSet.getString("skill_codes"));
                exercice.setDifficulty(resultSet.getInt("difficulty"));

                exercices.add(exercice);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des exercices : " + e.getMessage());
        }
        return exercices;
    }

    // 3. READ ONE
    public Exercice getExerciceById(int id) {
        String sql = "SELECT * FROM exercice WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Exercice exercice = new Exercice();
                exercice.setId(resultSet.getInt("id"));
                exercice.setType(resultSet.getString("type"));
                exercice.setQuestion(resultSet.getString("question"));
                exercice.setOptions(resultSet.getString("options"));
                exercice.setCorrectAnswer(resultSet.getString("correct_answer"));
                exercice.setAiGenerated(resultSet.getBoolean("ai_generated"));
                exercice.setEnabled(resultSet.getBoolean("enabled"));
                exercice.setQuizId(resultSet.getInt("quiz_id"));
                exercice.setSkillCodes(resultSet.getString("skill_codes"));
                exercice.setDifficulty(resultSet.getInt("difficulty"));

                return exercice;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'exercice : " + e.getMessage());
        }
        return null;
    }

    public List<Exercice> getExercicesByQuizId(int quizId) {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT * FROM exercice WHERE quiz_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quizId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Exercice exercice = new Exercice();
                exercice.setId(resultSet.getInt("id"));
                exercice.setType(resultSet.getString("type"));
                exercice.setQuestion(resultSet.getString("question"));
                exercice.setOptions(resultSet.getString("options"));
                exercice.setCorrectAnswer(resultSet.getString("correct_answer"));
                exercice.setAiGenerated(resultSet.getBoolean("ai_generated"));
                exercice.setEnabled(resultSet.getBoolean("enabled"));
                exercice.setQuizId(resultSet.getInt("quiz_id"));
                exercice.setSkillCodes(resultSet.getString("skill_codes"));
                exercice.setDifficulty(resultSet.getInt("difficulty"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des exercices par quiz : " + e.getMessage());
        }
        return exercices;
    }

    // 4. UPDATE
    public void updateExercice(Exercice exercice) {
        String sql = "UPDATE exercice SET type=?, question=?, options=?, correct_answer=?, ai_generated=?, enabled=?, quiz_id=?, skill_codes=?, difficulty=? WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, exercice.getType());
            statement.setString(2, exercice.getQuestion());
            // Force un tableau JSON vide if empty/null
            statement.setString(3, (exercice.getOptions() == null || exercice.getOptions().trim().isEmpty()) ? "[]" : exercice.getOptions());
            statement.setString(4, exercice.getCorrectAnswer());
            statement.setBoolean(5, exercice.isAiGenerated());
            statement.setBoolean(6, exercice.isEnabled());
            statement.setInt(7, exercice.getQuizId());
            statement.setString(8, (exercice.getSkillCodes() == null || exercice.getSkillCodes().trim().isEmpty()) ? "[]" : exercice.getSkillCodes());
            statement.setInt(9, exercice.getDifficulty());
            statement.setInt(10, exercice.getId());

            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                System.out.println("Exercice mis à jour avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'exercice : " + e.getMessage());
        }
    }

    // 5. DELETE
    public void deleteExercice(int id) {
        String sql = "DELETE FROM exercice WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            int deletedRows = statement.executeUpdate();
            if (deletedRows > 0) {
                System.out.println("Exercice supprimé avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'exercice : " + e.getMessage());
        }
    }
}
