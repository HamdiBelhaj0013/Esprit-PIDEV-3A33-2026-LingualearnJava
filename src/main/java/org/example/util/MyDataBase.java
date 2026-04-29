package org.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;

    private final String URL      = "jdbc:mysql://localhost:3306/1lingualearn_db?autoReconnect=true&useSSL=false";
    private final String USER     = "root";
    private final String PASSWORD = "";

    private MyDataBase() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion établie !");
        } catch (SQLException e) {
            System.out.println("Erreur connexion: " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    // ✅ Vérifie et reconnecte automatiquement si connexion fermée
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Reconnexion à la base de données...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification connexion: " + e.getMessage());
            connect();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion fermée !");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
