package Entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commentaire {
    // Attributes
    private int idCommentaire;
    private String contenu;
    private LocalDateTime date;
    private int idPost;

    // Relationship: One Comment has many Reactions
    private List<Reaction> reactions;

    // Constructeur par défaut
    public Commentaire() {
        this.reactions = new ArrayList<>();
        this.date = LocalDateTime.now();
    }

    // Constructeur SANS idCommentaire (pour création)
    public Commentaire(String contenu, int idPost) {
        this.contenu = contenu;
        this.idPost = idPost;
        this.date = LocalDateTime.now();
        this.reactions = new ArrayList<>();
    }

    // Constructeur AVEC idCommentaire (pour récupération depuis BD)
    public Commentaire(int idCommentaire, String contenu, LocalDateTime date, int idPost) {
        this.idCommentaire = idCommentaire;
        this.contenu = contenu;
        this.date = date;
        this.idPost = idPost;
        this.reactions = new ArrayList<>();
    }

    // Getters and Setters
    public int getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(int idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public int getIdPost() {
        return idPost;
    }

    public void setIdPost(int idPost) {
        this.idPost = idPost;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = reactions;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "idCommentaire=" + idCommentaire +
                ", contenu='" + contenu + '\'' +
                ", date=" + date +
                ", idPost=" + idPost +
                '}';
    }
}