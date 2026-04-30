package org.example.service;

import org.example.entities.Quiz;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {
    
    public QuizService() {
    }

    // 1. CREATE
    public void addQuiz(Quiz quiz) {
        String sql = "INSERT INTO quiz (title, description, passing_score, question_count, created_at, updated_at, enabled, lesson_id, difficulty, skill_codes) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, ?, ?)";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, quiz.getTitle());
            statement.setString(2, quiz.getDescription());
            statement.setInt(3, quiz.getPassingScore());
            statement.setInt(4, quiz.getQuestionCount());
            statement.setBoolean(5, quiz.isEnabled());
            
            if (quiz.getLessonId() != null) {
                statement.setInt(6, quiz.getLessonId());
            } else {
                statement.setNull(6, Types.INTEGER);
            }
            
            statement.setInt(7, quiz.getDifficulty());
            statement.setString(8, (quiz.getSkillCodes() == null || quiz.getSkillCodes().trim().isEmpty()) ? "[]" : quiz.getSkillCodes());

            statement.executeUpdate();
            
            // Mettre à jour l'ID du Quiz avec l'ID auto-généré par la base de données
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                quiz.setId(rs.getInt(1));
            }
            System.out.println("Quiz ajouté avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du quiz : " + e.getMessage());
        }
    }

    // 2. READ ALL
    public List<Quiz> getAllQuizzes() {
        List<Quiz> quizzes = new ArrayList<>();
        String sql = "SELECT * FROM quiz";
        try (Statement statement = DatabaseConnection.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Quiz quiz = new Quiz();
                quiz.setId(resultSet.getInt("id"));
                quiz.setTitle(resultSet.getString("title"));
                quiz.setDescription(resultSet.getString("description"));
                quiz.setPassingScore(resultSet.getInt("passing_score"));
                quiz.setQuestionCount(resultSet.getInt("question_count"));
                quiz.setCreatedAt(resultSet.getTimestamp("created_at"));
                quiz.setUpdatedAt(resultSet.getTimestamp("updated_at"));
                quiz.setEnabled(resultSet.getBoolean("enabled"));
                
                int lessonId = resultSet.getInt("lesson_id");
                quiz.setLessonId(resultSet.wasNull() ? null : lessonId);
                
                quiz.setDifficulty(resultSet.getInt("difficulty"));
                quiz.setSkillCodes(resultSet.getString("skill_codes"));

                quizzes.add(quiz);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des quiz : " + e.getMessage());
        }
        return quizzes;
    }

    // 3. READ ONE
    public Quiz getQuizById(int id) {
        String sql = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Quiz quiz = new Quiz();
                quiz.setId(resultSet.getInt("id"));
                quiz.setTitle(resultSet.getString("title"));
                quiz.setDescription(resultSet.getString("description"));
                quiz.setPassingScore(resultSet.getInt("passing_score"));
                quiz.setQuestionCount(resultSet.getInt("question_count"));
                quiz.setCreatedAt(resultSet.getTimestamp("created_at"));
                quiz.setUpdatedAt(resultSet.getTimestamp("updated_at"));
                quiz.setEnabled(resultSet.getBoolean("enabled"));
                
                int lessonId = resultSet.getInt("lesson_id");
                quiz.setLessonId(resultSet.wasNull() ? null : lessonId);
                
                quiz.setDifficulty(resultSet.getInt("difficulty"));
                quiz.setSkillCodes(resultSet.getString("skill_codes"));
                
                return quiz;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du quiz : " + e.getMessage());
        }
        return null;
    }

    // 4. UPDATE
    public void updateQuiz(Quiz quiz) {
        String sql = "UPDATE quiz SET title=?, description=?, passing_score=?, question_count=?, updated_at=CURRENT_TIMESTAMP, enabled=?, lesson_id=?, difficulty=?, skill_codes=? WHERE id=?";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql)) {
            statement.setString(1, quiz.getTitle());
            statement.setString(2, quiz.getDescription());
            statement.setInt(3, quiz.getPassingScore());
            statement.setInt(4, quiz.getQuestionCount());
            statement.setBoolean(5, quiz.isEnabled());
            
            if (quiz.getLessonId() != null) {
                statement.setInt(6, quiz.getLessonId());
            } else {
                statement.setNull(6, Types.INTEGER);
            }
            
            statement.setInt(7, quiz.getDifficulty());
            statement.setString(8, quiz.getSkillCodes() != null ? quiz.getSkillCodes() : "[]");
            statement.setInt(9, quiz.getId());

            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                System.out.println("Quiz mis à jour avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du quiz : " + e.getMessage());
        }
    }

    // 5. DELETE
    public void deleteQuiz(int id) {
        String sql = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement statement = DatabaseConnection.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            int deletedRows = statement.executeUpdate();
            if (deletedRows > 0) {
                System.out.println("Quiz supprimé avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du quiz : " + e.getMessage());
        }
    }
}
