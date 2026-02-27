package Services;

import Entities.Commentaire;
import Iservices.IcommentaireServices;
import Utils.BlogDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Commentaire_Services implements IcommentaireServices {

    private Connection connection;

    public Commentaire_Services() {
        connection = BlogDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouterCommentaire(Commentaire commentaire) {
        String requete = "INSERT INTO commentaires (contenu, id_post) VALUES (?, ?)";

        try {
            PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, commentaire.getContenu());
            pst.setInt(2, commentaire.getIdPost());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) {
                    commentaire.setIdCommentaire(generatedKeys.getInt(1));
                }
                System.out.println("Commentaire ajoute avec succes! ID: " + commentaire.getIdCommentaire());
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du commentaire!");
            e.printStackTrace();
        }
    }

    @Override
    public List<Commentaire> afficherCommentaires() {
        List<Commentaire> commentaires = new ArrayList<>();
        String requete = "SELECT * FROM commentaires ORDER BY date DESC";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(requete);

            while (rs.next()) {
                Commentaire commentaire = new Commentaire(
                        rs.getInt("id_commentaire"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        rs.getInt("id_post")
                );
                commentaires.add(commentaire);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage des commentaires!");
            e.printStackTrace();
        }

        return commentaires;
    }

    @Override
    public Commentaire getCommentaireById(int idCommentaire) {
        String requete = "SELECT * FROM commentaires WHERE id_commentaire = ?";
        Commentaire commentaire = null;

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idCommentaire);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                commentaire = new Commentaire(
                        rs.getInt("id_commentaire"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        rs.getInt("id_post")
                );
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation du commentaire!");
            e.printStackTrace();
        }

        return commentaire;
    }

    @Override
    public List<Commentaire> getCommentairesByPost(int idPost) {
        List<Commentaire> commentaires = new ArrayList<>();
        String requete = "SELECT * FROM commentaires WHERE id_post = ? ORDER BY date DESC";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Commentaire commentaire = new Commentaire(
                        rs.getInt("id_commentaire"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        rs.getInt("id_post")
                );
                commentaires.add(commentaire);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des commentaires du post!");
            e.printStackTrace();
        }

        return commentaires;
    }

    @Override
    public void modifierCommentaire(Commentaire commentaire) {
        String requete = "UPDATE commentaires SET contenu = ? WHERE id_commentaire = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, commentaire.getContenu());
            pst.setInt(2, commentaire.getIdCommentaire());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Commentaire modifie avec succes!");
            } else {
                System.out.println("Aucun commentaire trouve avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du commentaire!");
            e.printStackTrace();
        }
    }

    @Override
    public void supprimerCommentaire(int idCommentaire) {
        String requete = "DELETE FROM commentaires WHERE id_commentaire = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idCommentaire);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Commentaire supprime avec succes!");
            } else {
                System.out.println("Aucun commentaire trouve avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du commentaire!");
            e.printStackTrace();
        }
    }
}