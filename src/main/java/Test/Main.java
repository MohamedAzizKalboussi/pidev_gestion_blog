package Test;

import Entities.Post;
import Entities.Commentaire;
import Entities.Reaction;
import Services.Posting_Services;
import Services.Commentaire_Services;
import Services.Reaction_Services;
import Iservices.IpostServices;
import Iservices.IcommentaireServices;
import Iservices.IreactionServices;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== Test CRUD Complet ===\n");

        // Initialiser les services
        IpostServices postService = new Posting_Services();
        IcommentaireServices commentaireService = new Commentaire_Services();
        IreactionServices reactionService = new Reaction_Services();

        // ========== TEST POST ==========
        System.out.println("--- Test Post ---");
        Post post1 = new Post("Mon premier post!", "photo.jpg", "publie");
        postService.ajouterPost(post1);

        Post post2 = new Post("Deuxieme post de test", "video.mp4", "brouillon");
        postService.ajouterPost(post2);

        System.out.println("\nListe des posts:");
        for (Post p : postService.afficherPosts()) {
            System.out.println(p);
        }

        // ========== TEST COMMENTAIRE ==========
        System.out.println("\n--- Test Commentaire ---");
        Commentaire comment1 = new Commentaire("Super post!", post1.getIdPost());
        commentaireService.ajouterCommentaire(comment1);

        Commentaire comment2 = new Commentaire("Merci pour le partage!", post1.getIdPost());
        commentaireService.ajouterCommentaire(comment2);

        System.out.println("\nCommentaires du post " + post1.getIdPost() + ":");
        for (Commentaire c : commentaireService.getCommentairesByPost(post1.getIdPost())) {
            System.out.println(c);
        }

        // ========== TEST REACTION SUR POST ==========
        System.out.println("\n--- Test Reaction sur Post ---");
        Reaction reactionPost1 = new Reaction("LIKE", post1.getIdPost());
        reactionService.ajouterReaction(reactionPost1);

        Reaction reactionPost2 = new Reaction("LOVE", post1.getIdPost());
        reactionService.ajouterReaction(reactionPost2);

        System.out.println("\nReactions du post " + post1.getIdPost() + ":");
        for (Reaction r : reactionService.getReactionsByPost(post1.getIdPost())) {
            System.out.println(r);
        }

        // ========== TEST REACTION SUR COMMENTAIRE ==========
        System.out.println("\n--- Test Reaction sur Commentaire ---");
        Reaction reactionComment1 = new Reaction("LIKE", null, comment1.getIdCommentaire());
        reactionService.ajouterReaction(reactionComment1);

        Reaction reactionComment2 = new Reaction("HAHA", null, comment1.getIdCommentaire());
        reactionService.ajouterReaction(reactionComment2);

        System.out.println("\nReactions du commentaire " + comment1.getIdCommentaire() + ":");
        for (Reaction r : reactionService.getReactionsByCommentaire(comment1.getIdCommentaire())) {
            System.out.println(r);
        }

        // ========== TEST COMPTAGE REACTIONS ==========
        System.out.println("\n--- Test Comptage Reactions ---");
        int nbLikes = reactionService.compterReactionsByType("LIKE", post1.getIdPost(), null);
        System.out.println("Nombre de LIKE sur le post " + post1.getIdPost() + ": " + nbLikes);

        // ========== TEST MODIFICATION ==========
        System.out.println("\n--- Test Modification ---");
        post1.setContenu("Contenu modifie!");
        postService.modifierPost(post1);
        System.out.println("Post modifie: " + postService.getPostById(post1.getIdPost()));

        // ========== TEST SUPPRESSION ==========
        System.out.println("\n--- Test Suppression ---");
        postService.supprimerPost(post2.getIdPost());

        System.out.println("\nListe finale des posts:");
        for (Post p : postService.afficherPosts()) {
            System.out.println(p);
        }

        System.out.println("\n=== Fin des tests ===");
    }
}