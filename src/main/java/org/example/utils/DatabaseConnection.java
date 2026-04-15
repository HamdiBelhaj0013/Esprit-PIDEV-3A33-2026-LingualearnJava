package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Les informations de connexion à la base de données XAMPP
    private static final String URL = "jdbc:mysql://localhost:3306/1lingualearn_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Par défaut sous XAMPP, le mot de passe est vide
    
    private static Connection connection;
    
    // Constructeur privé pour le design pattern Singleton
    private DatabaseConnection() {
        try {
            // Chargement explicite du driver JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base de données réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("Le driver MySQL est introuvable. Avez-vous pensé à recharger Maven (Reload Project) ?");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Échec de la connexion à la base de données.");
            e.printStackTrace();
        }
    }
    
    // Méthode pour obtenir l'instance unique de la connexion
    public static Connection getConnection() {
        try {
            // On vérifie si la connexion est fermée ou nulle avant de la recréer
            if (connection == null || connection.isClosed()) {
                new DatabaseConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
