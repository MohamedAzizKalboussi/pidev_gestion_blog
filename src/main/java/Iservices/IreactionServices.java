package Iservices;

import Entities.Reaction;
import java.util.List;

public interface IreactionServices {
    // Create
    void ajouterReaction(Reaction reaction);

    // Read
    List<Reaction> afficherReactions();
    Reaction getReactionById(int idReaction);
    List<Reaction> getReactionsByCommentaire(int idCommentaire);
    List<Reaction> getReactionsByPost(int idPost);

    // Update
    void modifierReaction(Reaction reaction);

    // Delete
    void supprimerReaction(int idReaction);

    // Méthodes supplémentaires
    int compterReactionsByType(String type, Integer idPost, Integer idCommentaire);
}