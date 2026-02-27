package Iservices;

import Entities.Commentaire;
import java.util.List;

public interface IcommentaireServices {
    // Create
    void ajouterCommentaire(Commentaire commentaire);

    // Read
    List<Commentaire> afficherCommentaires();
    Commentaire getCommentaireById(int idCommentaire);
    List<Commentaire> getCommentairesByPost(int idPost);

    // Update
    void modifierCommentaire(Commentaire commentaire);

    // Delete
    void supprimerCommentaire(int idCommentaire);
}