package Services;

import Entities.Reaction;
import Iservices.IreactionServices;
import Utils.BlogDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Reaction_Services implements IreactionServices {

    private Connection connection;

    public Reaction_Services() {
        connection = BlogDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouterReaction(Reaction reaction) {
        String requete = "INSERT INTO reactions (type, id_post, id_commentaire) VALUES (?, ?, ?)";

        try {
            PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, reaction.getType());

            if (reaction.getIdPost() != null) {
                pst.setInt(2, reaction.getIdPost());
            } else {
                pst.setNull(2, Types.INTEGER);
            }

            if (reaction.getIdCommentaire() != null) {
                pst.setInt(3, reaction.getIdCommentaire());
            } else {
                pst.setNull(3, Types.INTEGER);
            }

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) {
                    reaction.setIdReaction(generatedKeys.getInt(1));
                }
                System.out.println("Reaction ajoutee avec succes! ID: " + reaction.getIdReaction());
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la reaction!");
            e.printStackTrace();
        }
    }

    @Override
    public List<Reaction> afficherReactions() {
        List<Reaction> reactions = new ArrayList<>();
        String requete = "SELECT * FROM reactions ORDER BY date DESC";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(requete);

            while (rs.next()) {
                Integer idPost = rs.getObject("id_post") != null ? rs.getInt("id_post") : null;
                Integer idCommentaire = rs.getObject("id_commentaire") != null ? rs.getInt("id_commentaire") : null;

                Reaction reaction = new Reaction(
                        rs.getInt("id_reaction"),
                        rs.getString("type"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        idPost,
                        idCommentaire
                );
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage des reactions!");
            e.printStackTrace();
        }

        return reactions;
    }

    @Override
    public Reaction getReactionById(int idReaction) {
        String requete = "SELECT * FROM reactions WHERE id_reaction = ?";
        Reaction reaction = null;

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idReaction);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                Integer idPost = rs.getObject("id_post") != null ? rs.getInt("id_post") : null;
                Integer idCommentaire = rs.getObject("id_commentaire") != null ? rs.getInt("id_commentaire") : null;

                reaction = new Reaction(
                        rs.getInt("id_reaction"),
                        rs.getString("type"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        idPost,
                        idCommentaire
                );
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation de la reaction!");
            e.printStackTrace();
        }

        return reaction;
    }

    @Override
    public List<Reaction> getReactionsByCommentaire(int idCommentaire) {
        List<Reaction> reactions = new ArrayList<>();
        String requete = "SELECT * FROM reactions WHERE id_commentaire = ? ORDER BY date DESC";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idCommentaire);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Integer idPost = rs.getObject("id_post") != null ? rs.getInt("id_post") : null;

                Reaction reaction = new Reaction(
                        rs.getInt("id_reaction"),
                        rs.getString("type"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        idPost,
                        rs.getInt("id_commentaire")
                );
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des reactions du commentaire!");
            e.printStackTrace();
        }

        return reactions;
    }

    @Override
    public List<Reaction> getReactionsByPost(int idPost) {
        List<Reaction> reactions = new ArrayList<>();
        String requete = "SELECT * FROM reactions WHERE id_post = ? ORDER BY date DESC";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Integer idCommentaire = rs.getObject("id_commentaire") != null ? rs.getInt("id_commentaire") : null;

                Reaction reaction = new Reaction(
                        rs.getInt("id_reaction"),
                        rs.getString("type"),
                        rs.getTimestamp("date").toLocalDateTime(),
                        rs.getInt("id_post"),
                        idCommentaire
                );
                reactions.add(reaction);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des reactions du post!");
            e.printStackTrace();
        }

        return reactions;
    }

    @Override
    public void modifierReaction(Reaction reaction) {
        String requete = "UPDATE reactions SET type = ? WHERE id_reaction = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, reaction.getType());
            pst.setInt(2, reaction.getIdReaction());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Reaction modifiee avec succes!");
            } else {
                System.out.println("Aucune reaction trouvee avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification de la reaction!");
            e.printStackTrace();
        }
    }

    @Override
    public void supprimerReaction(int idReaction) {
        String requete = "DELETE FROM reactions WHERE id_reaction = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idReaction);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Reaction supprimee avec succes!");
            } else {
                System.out.println("Aucune reaction trouvee avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la reaction!");
            e.printStackTrace();
        }
    }

    @Override
    public int compterReactionsByType(String type, Integer idPost, Integer idCommentaire) {
        String requete;

        if (idPost != null) {
            requete = "SELECT COUNT(*) as total FROM reactions WHERE id_post = ? AND type = ?";
        } else if (idCommentaire != null) {
            requete = "SELECT COUNT(*) as total FROM reactions WHERE id_commentaire = ? AND type = ?";
        } else {
            return 0;
        }

        int total = 0;

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost != null ? idPost : idCommentaire);
            pst.setString(2, type);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                total = rs.getInt("total");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des reactions!");
            e.printStackTrace();
        }

        return total;
    }
}