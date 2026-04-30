package org.example.service;

import org.example.entities.Lesson;
import org.example.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LessonService {

    public List<Lesson> getAllLessons() {
        List<Lesson> lessons = new ArrayList<>();
        String query = "SELECT id, title FROM lesson";
        
        Connection conn = DatabaseConnection.getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                lessons.add(new Lesson(
                    rs.getInt("id"),
                    rs.getString("title")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lessons;
    }
}
