package org.example.services;

import org.example.entities.Lesson;
import org.example.interfaces.CrudService;
import org.example.utils.MyDataBase;
import org.example.validators.LessonValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LessonService implements CrudService<Lesson> {

    private final Connection cnx;

    public LessonService() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Lesson lesson) throws SQLException {
        List<String> errors = LessonValidator.validate(lesson);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "INSERT INTO lesson(title, content, vocabulary_data, grammar_data, xp_reward, course_id, video_name, thumb_name, resource_name, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, lesson.getTitle());
        ps.setString(2, lesson.getContent());
        ps.setString(3, lesson.getVocabularyData());
        ps.setString(4, lesson.getGrammarData());
        ps.setInt(5, lesson.getXpReward());
        ps.setInt(6, lesson.getCourseId());
        ps.setString(7, lesson.getVideoName());
        ps.setString(8, lesson.getThumbName());
        ps.setString(9, lesson.getResourceName());

        if (lesson.getUpdatedAt() == null) {
            ps.setNull(10, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(10, lesson.getUpdatedAt());
        }

        ps.executeUpdate();
    }

    @Override
    public void update(Lesson lesson) throws SQLException {
        List<String> errors = LessonValidator.validate(lesson);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "UPDATE lesson SET title=?, content=?, vocabulary_data=?, grammar_data=?, xp_reward=?, course_id=?, video_name=?, thumb_name=?, resource_name=?, updated_at=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, lesson.getTitle());
        ps.setString(2, lesson.getContent());
        ps.setString(3, lesson.getVocabularyData());
        ps.setString(4, lesson.getGrammarData());
        ps.setInt(5, lesson.getXpReward());
        ps.setInt(6, lesson.getCourseId());
        ps.setString(7, lesson.getVideoName());
        ps.setString(8, lesson.getThumbName());
        ps.setString(9, lesson.getResourceName());

        if (lesson.getUpdatedAt() == null) {
            ps.setNull(10, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(10, lesson.getUpdatedAt());
        }

        ps.setInt(11, lesson.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM lesson WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Lesson> getAll() throws SQLException {
        List<Lesson> list = new ArrayList<>();
        String sql = "SELECT * FROM lesson";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Lesson lesson = new Lesson(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("vocabulary_data"),
                    rs.getString("grammar_data"),
                    rs.getInt("xp_reward"),
                    rs.getInt("course_id"),
                    rs.getString("video_name"),
                    rs.getString("thumb_name"),
                    rs.getString("resource_name"),
                    rs.getTimestamp("updated_at")
            );
            list.add(lesson);
        }

        return list;
    }

    @Override
    public Lesson getById(int id) throws SQLException {
        String sql = "SELECT * FROM lesson WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Lesson(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("vocabulary_data"),
                    rs.getString("grammar_data"),
                    rs.getInt("xp_reward"),
                    rs.getInt("course_id"),
                    rs.getString("video_name"),
                    rs.getString("thumb_name"),
                    rs.getString("resource_name"),
                    rs.getTimestamp("updated_at")
            );
        }

        return null;
    }
    public List<Lesson> getLessonsByCourse(int courseId) throws Exception {
        String sql = "SELECT * FROM lesson WHERE course_id = ?";
        List<Lesson> lessons = new ArrayList<>();

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, courseId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Lesson lesson = new Lesson();
            lesson.setId(rs.getInt("id"));
            lesson.setTitle(rs.getString("title"));
            lesson.setContent(rs.getString("content"));
            lesson.setVocabularyData(rs.getString("vocabulary_data"));
            lesson.setGrammarData(rs.getString("grammar_data"));
            lesson.setXpReward(rs.getInt("xp_reward"));
            lesson.setCourseId(rs.getInt("course_id"));
            lesson.setVideoName(rs.getString("video_name"));
            lesson.setThumbName(rs.getString("thumb_name"));
            lesson.setResourceName(rs.getString("resource_name"));
            lesson.setUpdatedAt(rs.getTimestamp("updated_at"));
            lessons.add(lesson);
        }

        return lessons;
    }
}