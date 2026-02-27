package Services;

import Entities.Reaction;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Reaction_Services
 * Ordre : Ajouter → Modifier → Supprimer
 * Nettoyage automatique après chaque test via @AfterEach

 * IMPORTANT : Ces tests nécessitent qu'un Post avec id_post = 1 et
 * un Commentaire avec id_commentaire = 1 existent déjà en BD.
 * Adaptez ID_POST_REF et ID_COMMENTAIRE_REF si besoin.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReactionServicesTest {

    private static Reaction_Services service;

    /**
     * Références vers des entités existantes en BD (clés étrangères).
     * Changez ces valeurs selon votre base de données.
     */
    private static final int ID_POST_REF         = 29;
    private static final int ID_COMMENTAIRE_REF  = 26;

    // ID partagé entre les tests
    private static int idReactionTest;

    // ------------------------------------------------------------------ //
    //  Setup & Teardown
    // ------------------------------------------------------------------ //

    @BeforeAll
    static void setUp() {
        service = new Reaction_Services();
    }

    /**
     * Nettoyage automatique après chaque test.
     * Supprime la réaction de test si elle existe encore en BD.
     */
    @AfterEach
    void cleanUp() {
        if (idReactionTest != 0) {
            Reaction existante = service.getReactionById(idReactionTest);
            if (existante != null) {
                service.supprimerReaction(idReactionTest);
                System.out.println("[CleanUp] Réaction de test supprimée (id=" + idReactionTest + ")");
            }
            idReactionTest = 0;
        }
    }

    // ------------------------------------------------------------------ //
    //  Test 1 : Ajouter une Réaction sur un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(1)
    @DisplayName("Test 1 : Ajouter une Réaction sur un Post")
    void testAjouterReactionSurPost() {
        Reaction reaction = new Reaction("LIKE", ID_POST_REF);
        service.ajouterReaction(reaction);

        // L'ID doit avoir été alimenté par la BD
        assertTrue(reaction.getIdReaction() > 0,
                "L'ID de la réaction doit être > 0 après insertion");

        idReactionTest = reaction.getIdReaction();

        // Vérification via afficher
        List<Reaction> reactions = service.afficherReactions();
        assertFalse(reactions.isEmpty(), "La liste ne doit pas être vide");
        assertTrue(reactions.stream()
                        .anyMatch(r -> r.getIdReaction() == idReactionTest),
                "La réaction insérée doit être présente dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 2 : Ajouter une Réaction sur un Commentaire
    // ------------------------------------------------------------------ //

    @Test
    @Order(2)
    @DisplayName("Test 2 : Ajouter une Réaction sur un Commentaire")
    void testAjouterReactionSurCommentaire() {
        Reaction reaction = new Reaction("LOVE", null, ID_COMMENTAIRE_REF);
        service.ajouterReaction(reaction);

        assertTrue(reaction.getIdReaction() > 0,
                "L'ID de la réaction doit être > 0 après insertion");

        idReactionTest = reaction.getIdReaction();

        Reaction recuperee = service.getReactionById(idReactionTest);
        assertNotNull(recuperee, "La réaction doit être retrouvée en BD");
        assertEquals("LOVE", recuperee.getType());
        assertNull(recuperee.getIdPost(),
                "idPost doit être null pour une réaction sur commentaire");
        assertEquals(ID_COMMENTAIRE_REF, recuperee.getIdCommentaire());
    }

    // ------------------------------------------------------------------ //
    //  Test 3 : Modifier une Réaction
    // ------------------------------------------------------------------ //

    @Test
    @Order(3)
    @DisplayName("Test 3 : Modifier une Réaction")
    void testModifierReaction() {
        // Pré-requis : insérer une réaction
        Reaction reaction = new Reaction("HAHA", ID_POST_REF);
        service.ajouterReaction(reaction);
        idReactionTest = reaction.getIdReaction();
        assertTrue(idReactionTest > 0, "La réaction de test doit être insérée");

        // Modification du type
        reaction.setType("WOW");
        service.modifierReaction(reaction);

        // Vérification
        Reaction modifiee = service.getReactionById(idReactionTest);
        assertNotNull(modifiee, "La réaction modifiée doit exister en BD");
        assertEquals("WOW", modifiee.getType(),
                "Le type de la réaction doit être mis à jour");
    }

    // ------------------------------------------------------------------ //
    //  Test 4 : Supprimer une Réaction
    // ------------------------------------------------------------------ //

    @Test
    @Order(4)
    @DisplayName("Test 4 : Supprimer une Réaction")
    void testSupprimerReaction() {
        // Pré-requis : insérer une réaction
        Reaction reaction = new Reaction("SAD", ID_POST_REF);
        service.ajouterReaction(reaction);
        idReactionTest = reaction.getIdReaction();
        assertTrue(idReactionTest > 0, "La réaction de test doit être insérée");

        // Suppression
        service.supprimerReaction(idReactionTest);

        // Vérification : ne doit plus exister
        Reaction supprimee = service.getReactionById(idReactionTest);
        assertNull(supprimee, "La réaction supprimée ne doit plus exister en BD");

        // Déjà supprimé
        idReactionTest = 0;
    }

    // ------------------------------------------------------------------ //
    //  Test 5 : Afficher toutes les Réactions
    // ------------------------------------------------------------------ //

    @Test
    @Order(5)
    @DisplayName("Test 5 : Afficher toutes les Réactions")
    void testAfficherReactions() {
        Reaction reaction = new Reaction("ANGRY", ID_POST_REF);
        service.ajouterReaction(reaction);
        idReactionTest = reaction.getIdReaction();

        List<Reaction> reactions = service.afficherReactions();

        assertNotNull(reactions, "La liste ne doit pas être null");
        assertFalse(reactions.isEmpty(), "La liste doit contenir au moins une réaction");
        assertTrue(reactions.stream()
                        .anyMatch(r -> r.getIdReaction() == idReactionTest),
                "La réaction insérée doit figurer dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 6 : Récupérer les Réactions d'un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(6)
    @DisplayName("Test 6 : Récupérer les Réactions d'un Post")
    void testGetReactionsByPost() {
        Reaction reaction = new Reaction("LIKE", ID_POST_REF);
        service.ajouterReaction(reaction);
        idReactionTest = reaction.getIdReaction();

        List<Reaction> reactionsDuPost = service.getReactionsByPost(ID_POST_REF);

        assertNotNull(reactionsDuPost, "La liste ne doit pas être null");
        assertTrue(reactionsDuPost.stream()
                        .anyMatch(r -> r.getIdReaction() == idReactionTest),
                "La réaction insérée doit apparaître dans la liste du post");
        assertTrue(reactionsDuPost.stream()
                        .allMatch(r -> r.getIdPost() != null && r.getIdPost() == ID_POST_REF),
                "Toutes les réactions doivent appartenir au post " + ID_POST_REF);
    }

    // ------------------------------------------------------------------ //
    //  Test 7 : Récupérer les Réactions d'un Commentaire
    // ------------------------------------------------------------------ //

    @Test
    @Order(7)
    @DisplayName("Test 7 : Récupérer les Réactions d'un Commentaire")
    void testGetReactionsByCommentaire() {
        Reaction reaction = new Reaction("LOVE", null, ID_COMMENTAIRE_REF);
        service.ajouterReaction(reaction);
        idReactionTest = reaction.getIdReaction();

        List<Reaction> reactionsDuCommentaire =
                service.getReactionsByCommentaire(ID_COMMENTAIRE_REF);

        assertNotNull(reactionsDuCommentaire, "La liste ne doit pas être null");
        assertTrue(reactionsDuCommentaire.stream()
                        .anyMatch(r -> r.getIdReaction() == idReactionTest),
                "La réaction insérée doit apparaître dans la liste du commentaire");
        assertTrue(reactionsDuCommentaire.stream()
                        .allMatch(r -> r.getIdCommentaire() != null
                                && r.getIdCommentaire() == ID_COMMENTAIRE_REF),
                "Toutes les réactions doivent appartenir au commentaire " + ID_COMMENTAIRE_REF);
    }

    // ------------------------------------------------------------------ //
    //  Test 8 : Compter les Réactions par Type sur un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(8)
    @DisplayName("Test 8 : Compter les Réactions par Type sur un Post")
    void testCompterReactionsByTypeSurPost() {
        // Insérer 2 réactions LIKE sur le post de référence
        Reaction r1 = new Reaction("LIKE", ID_POST_REF);
        Reaction r2 = new Reaction("LIKE", ID_POST_REF);
        service.ajouterReaction(r1);
        service.ajouterReaction(r2);

        int totalAvant = service.compterReactionsByType("LIKE", ID_POST_REF, null);
        assertTrue(totalAvant >= 2,
                "Il doit y avoir au moins 2 réactions LIKE sur le post");

        // Nettoyage manuel des deux réactions créées ici
        service.supprimerReaction(r1.getIdReaction());
        service.supprimerReaction(r2.getIdReaction());

        int totalApres = service.compterReactionsByType("LIKE", ID_POST_REF, null);
        assertEquals(totalAvant - 2, totalApres,
                "Le compteur doit avoir diminué de 2 après suppression");
    }
}