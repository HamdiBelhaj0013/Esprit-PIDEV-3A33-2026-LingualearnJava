package org.example.services;

import org.example.entities.Course;
import org.example.interfaces.CrudService;
import org.example.utils.MyDataBase;
import org.example.validators.CourseValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements CrudService<Course> {

    private final Connection cnx;

    public CourseService() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Course course) throws SQLException {
        List<String> errors = CourseValidator.validate(course);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "INSERT INTO course(title, level, status, published_at, author_id, platform_language_id) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, course.getTitle());
        ps.setString(2, course.getLevel());
        ps.setString(3, course.getStatus());
        ps.setTimestamp(4, course.getPublishedAt());

        if (course.getAuthorId() == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, course.getAuthorId());
        }

        ps.setInt(6, course.getPlatformLanguageId());

        ps.executeUpdate();
    }

    @Override
    public void update(Course course) throws SQLException {
        List<String> errors = CourseValidator.validate(course);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "UPDATE course SET title=?, level=?, status=?, published_at=?, author_id=?, platform_language_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, course.getTitle());
        ps.setString(2, course.getLevel());
        ps.setString(3, course.getStatus());
        ps.setTimestamp(4, course.getPublishedAt());

        if (course.getAuthorId() == null) {
            ps.setNull(5, Types.INTEGER);
        } else {
            ps.setInt(5, course.getAuthorId());
        }

        ps.setInt(6, course.getPlatformLanguageId());
        ps.setInt(7, course.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM course WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Course> getAll() throws SQLException {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT * FROM course";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Integer authorId = rs.getObject("author_id") != null ? rs.getInt("author_id") : null;

            Course course = new Course(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("level"),
                    rs.getString("status"),
                    rs.getTimestamp("published_at"),
                    authorId,
                    rs.getInt("platform_language_id")
            );
            list.add(course);
        }

        return list;
    }

    @Override
    public Course getById(int id) throws SQLException {
        String sql = "SELECT * FROM course WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Integer authorId = rs.getObject("author_id") != null ? rs.getInt("author_id") : null;

            return new Course(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("level"),
                    rs.getString("status"),
                    rs.getTimestamp("published_at"),
                    authorId,
                    rs.getInt("platform_language_id")
            );
        }

        return null;
    }
    public List<Course> getCoursesByLanguage(int languageId) throws Exception {
        String sql = "SELECT * FROM course WHERE platform_language_id = ?";
        List<Course> courses = new ArrayList<>();

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, languageId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Course course = new Course();
            course.setId(rs.getInt("id"));
            course.setTitle(rs.getString("title"));
            course.setLevel(rs.getString("level"));
            course.setStatus(rs.getString("status"));
            course.setPublishedAt(rs.getTimestamp("published_at"));

            int authorId = rs.getInt("author_id");
            if (rs.wasNull()) {
                course.setAuthorId(null);
            } else {
                course.setAuthorId(authorId);
            }

            course.setPlatformLanguageId(rs.getInt("platform_language_id"));
            courses.add(course);
        }

        return courses;
    }
}