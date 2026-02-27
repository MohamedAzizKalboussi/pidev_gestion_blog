package Entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    // Attributes
    private int idPost;
    private String contenu;
    private String media;
    private LocalDateTime dateCreation;
    private String statut;

    // Relationships
    private List<Commentaire> commentaires;
    private List<Reaction> reactions;

    // Constructeur par défaut
    public Post() {
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
        this.dateCreation = LocalDateTime.now();
    }

    // Constructeur SANS idPost (pour création)
    public Post(String contenu, String media, String statut) {
        this.contenu = contenu;
        this.media = media;
        this.statut = statut;
        this.dateCreation = LocalDateTime.now();
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
    }

    // Constructeur AVEC idPost (pour récupération depuis BD)
    public Post(int idPost, String contenu, String media, LocalDateTime dateCreation, String statut) {
        this.idPost = idPost;
        this.contenu = contenu;
        this.media = media;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
    }

    // Getters and Setters
    public int getIdPost() {
        return idPost;
    }

    public void setIdPost(int idPost) {
        this.idPost = idPost;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getMedia() {
        return media;
    }

    public void setMedia(String media) {
        this.media = media;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public List<Commentaire> getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires;
    }

    public List<Reaction> getReactions() {
        return reactions;
    }

    public void setReactions(List<Reaction> reactions) {
        this.reactions = new ArrayList<>(reactions);
    }

    @Override
    public String toString() {
        return "Post{" +
                "idPost=" + idPost +
                ", contenu='" + contenu + '\'' +
                ", media='" + media + '\'' +
                ", dateCreation=" + dateCreation +
                ", statut='" + statut + '\'' +
                '}';
    }
}