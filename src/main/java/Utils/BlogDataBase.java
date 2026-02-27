package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class BlogDataBase {

    private static BlogDataBase instance;
    private Connection connection;

    // Paramètres de connexion
    private static final String URL = "jdbc:mysql://localhost:3306/wonderlust_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    // Constructeur privé (Singleton)
    private BlogDataBase() {
        try {
            // Charger le driver MySQL (optionnel pour les versions récentes)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Établir la connexion
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion etablie avec succes a la base de donnees 'wonderlust_db'");

        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur de connexion a la base de donnees !");
            e.printStackTrace();
        }
    }

    // Méthode pour obtenir l'instance unique (Singleton)
    public static BlogDataBase getInstance() {
        if (instance == null) {
            synchronized (BlogDataBase.class) {
                if (instance == null) {
                    instance = new BlogDataBase();
                }
            }
        }
        return instance;
    }

    // Méthode pour obtenir la connexion
    public Connection getConnection() {
        try {
            // Vérifier si la connexion est fermée et la rouvrir si nécessaire
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Connexion retablie");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la verification de la connexion");
            e.printStackTrace();
        }
        return connection;
    }

    // Méthode pour fermer la connexion
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Connexion fermee");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion");
                e.printStackTrace();
            }
        }
    }

    // Méthode pour tester la connexion
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}