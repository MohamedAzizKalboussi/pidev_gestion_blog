package Services;

import Entities.Post;
import Iservices.IpostServices;
import Utils.BlogDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Posting_Services implements IpostServices {

    private Connection connection;

    public Posting_Services() {
        connection = BlogDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouterPost(Post post) {
        String requete = "INSERT INTO posts (contenu, media, statut) VALUES (?, ?, ?)";

        try {
            PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, post.getContenu());
            pst.setString(2, post.getMedia());
            pst.setString(3, post.getStatut());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) {
                    post.setIdPost(generatedKeys.getInt(1));
                }
                System.out.println("Post ajoute avec succes! ID: " + post.getIdPost());
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du post!");
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> afficherPosts() {
        List<Post> posts = new ArrayList<>();
        String requete = "SELECT * FROM posts ORDER BY date_creation DESC";

        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(requete);

            while (rs.next()) {
                Post post = new Post(
                        rs.getInt("id_post"),
                        rs.getString("contenu"),
                        rs.getString("media"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut")
                );
                posts.add(post);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage des posts!");
            e.printStackTrace();
        }

        return posts;
    }

    @Override
    public Post getPostById(int idPost) {
        String requete = "SELECT * FROM posts WHERE id_post = ?";
        Post post = null;

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                post = new Post(
                        rs.getInt("id_post"),
                        rs.getString("contenu"),
                        rs.getString("media"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut")
                );
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation du post!");
            e.printStackTrace();
        }

        return post;
    }

    @Override
    public void modifierPost(Post post) {
        String requete = "UPDATE posts SET contenu = ?, media = ?, statut = ? WHERE id_post = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, post.getContenu());
            pst.setString(2, post.getMedia());
            pst.setString(3, post.getStatut());
            pst.setInt(4, post.getIdPost());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Post modifie avec succes!");
            } else {
                System.out.println("Aucun post trouve avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification du post!");
            e.printStackTrace();
        }
    }

    @Override
    public void supprimerPost(int idPost) {
        String requete = "DELETE FROM posts WHERE id_post = ?";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Post supprime avec succes!");
            } else {
                System.out.println("Aucun post trouve avec cet ID!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du post!");
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getPostsByStatut(String statut) {
        List<Post> posts = new ArrayList<>();
        String requete = "SELECT * FROM posts WHERE statut = ? ORDER BY date_creation DESC";

        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, statut);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Post post = new Post(
                        rs.getInt("id_post"),
                        rs.getString("contenu"),
                        rs.getString("media"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("statut")
                );
                posts.add(post);
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la recuperation des posts par statut!");
            e.printStackTrace();
        }

        return posts;
    }
}