package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/orientation";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static DBConnection instance;
    private Connection cnx;

    private DBConnection() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            cnx = null;
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}


