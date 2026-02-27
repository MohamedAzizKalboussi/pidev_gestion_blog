package Entities;

import java.time.LocalDateTime;

public class Reaction {
    // Attributes
    private int idReaction;
    private String type; // LIKE, LOVE, HAHA, WOW, SAD, ANGRY
    private LocalDateTime date;
    private Integer idPost;        // Nullable (peut être null)
    private Integer idCommentaire; // Nullable (peut être null)

    // Constructeur par défaut
    public Reaction() {
        this.date = LocalDateTime.now();
    }

    // Constructeur SANS idReaction (pour création sur un Post)
    public Reaction(String type, int idPost) {
        this.type = type;
        this.idPost = idPost;
        this.idCommentaire = null;
        this.date = LocalDateTime.now();
    }

    // Constructeur SANS idReaction (pour création sur un Commentaire)
    public Reaction(String type, Integer idPost, Integer idCommentaire) {
        this.type = type;
        this.idPost = idPost;
        this.idCommentaire = idCommentaire;
        this.date = LocalDateTime.now();
    }

    // Constructeur AVEC idReaction (pour récupération depuis BD)
    public Reaction(int idReaction, String type, LocalDateTime date, Integer idPost, Integer idCommentaire) {
        this.idReaction = idReaction;
        this.type = type;
        this.date = date;
        this.idPost = idPost;
        this.idCommentaire = idCommentaire;
    }

    // Getters and Setters
    public int getIdReaction() {
        return idReaction;
    }

    public void setIdReaction(int idReaction) {
        this.idReaction = idReaction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getIdPost() {
        return idPost;
    }

    public void setIdPost(Integer idPost) {
        this.idPost = idPost;
    }

    public Integer getIdCommentaire() {
        return idCommentaire;
    }

    public void setIdCommentaire(Integer idCommentaire) {
        this.idCommentaire = idCommentaire;
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "idReaction=" + idReaction +
                ", type='" + type + '\'' +
                ", date=" + date +
                ", idPost=" + idPost +
                ", idCommentaire=" + idCommentaire +
                '}';
    }
}