package org.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;

    private final String URL      = "jdbc:mysql://localhost:3306/1lingualearn_db?autoReconnect=true&useSSL=false";
    private final String USER     = "root";
    private final String PASSWORD = "";

    private MyDataBase() {}

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    // ✅ Crée une NOUVELLE connexion à chaque appel — à utiliser dans try-with-resources
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Erreur connexion: " + e.getMessage());
            return null;
        }
    }

    // Gardé pour compatibilité si utilisé ailleurs
    public void closeConnection() {}
}