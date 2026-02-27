package Services;

import Entities.Commentaire;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Commentaire_Services
 * Ordre : Ajouter → Modifier → Supprimer
 * Nettoyage automatique après chaque test via @AfterEach

 * IMPORTANT : Ces tests nécessitent qu'un Post avec id_post = 1 existe
 * déjà en base de données (clé étrangère). Adaptez ID_POST_REF si besoin.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentaireServicesTest {

    private static Commentaire_Services service;

    /**
     * Référence vers un post existant en BD (clé étrangère obligatoire).
     * Changez cette valeur selon votre base de données.
     */
    private static final int ID_POST_REF = 29;

    // ID partagé entre les tests
    private static int idCommentaireTest;

    // ------------------------------------------------------------------ //
    //  Setup & Teardown
    // ------------------------------------------------------------------ //

    @BeforeAll
    static void setUp() {
        service = new Commentaire_Services();
    }

    /**
     * Nettoyage automatique après chaque test.
     * Supprime le commentaire de test s'il existe encore en BD.
     */
    @AfterEach
    void cleanUp() {
        if (idCommentaireTest != 0) {
            Commentaire existant = service.getCommentaireById(idCommentaireTest);
            if (existant != null) {
                service.supprimerCommentaire(idCommentaireTest);
                System.out.println("[CleanUp] Commentaire de test supprimé (id=" + idCommentaireTest + ")");
            }
            idCommentaireTest = 0;
        }
    }

    // ------------------------------------------------------------------ //
    //  Test 1 : Ajouter un Commentaire
    // ------------------------------------------------------------------ //

    @Test
    @Order(1)
    @DisplayName("Test 1 : Ajouter un Commentaire")
    void testAjouterCommentaire() {
        Commentaire commentaire = new Commentaire("Commentaire JUnit Test", ID_POST_REF);
        service.ajouterCommentaire(commentaire);

        // L'ID doit avoir été alimenté par la BD
        assertTrue(commentaire.getIdCommentaire() > 0,
                "L'ID du commentaire doit être > 0 après insertion");

        idCommentaireTest = commentaire.getIdCommentaire();

        // Vérification via afficher
        List<Commentaire> commentaires = service.afficherCommentaires();
        assertFalse(commentaires.isEmpty(), "La liste ne doit pas être vide");
        assertTrue(commentaires.stream()
                        .anyMatch(c -> c.getIdCommentaire() == idCommentaireTest),
                "Le commentaire inséré doit être présent dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 2 : Modifier un Commentaire
    // ------------------------------------------------------------------ //

    @Test
    @Order(2)
    @DisplayName("Test 2 : Modifier un Commentaire")
    void testModifierCommentaire() {
        // Pré-requis : insérer un commentaire
        Commentaire commentaire = new Commentaire("Contenu Avant Modif", ID_POST_REF);
        service.ajouterCommentaire(commentaire);
        idCommentaireTest = commentaire.getIdCommentaire();
        assertTrue(idCommentaireTest > 0, "Le commentaire de test doit être inséré");

        // Modification
        commentaire.setContenu("Contenu Apres Modif");
        service.modifierCommentaire(commentaire);

        // Vérification
        Commentaire modifie = service.getCommentaireById(idCommentaireTest);
        assertNotNull(modifie, "Le commentaire modifié doit exister en BD");
        assertEquals("Contenu Apres Modif", modifie.getContenu(),
                "Le contenu du commentaire doit être mis à jour");
    }

    // ------------------------------------------------------------------ //
    //  Test 3 : Supprimer un Commentaire
    // ------------------------------------------------------------------ //

    @Test
    @Order(3)
    @DisplayName("Test 3 : Supprimer un Commentaire")
    void testSupprimerCommentaire() {
        // Pré-requis : insérer un commentaire à supprimer
        Commentaire commentaire = new Commentaire("Commentaire A Supprimer", ID_POST_REF);
        service.ajouterCommentaire(commentaire);
        idCommentaireTest = commentaire.getIdCommentaire();
        assertTrue(idCommentaireTest > 0, "Le commentaire de test doit être inséré");

        // Suppression
        service.supprimerCommentaire(idCommentaireTest);

        // Vérification : ne doit plus exister
        Commentaire supprime = service.getCommentaireById(idCommentaireTest);
        assertNull(supprime, "Le commentaire supprimé ne doit plus exister en BD");

        // Déjà supprimé, pas besoin de nettoyage
        idCommentaireTest = 0;
    }

    // ------------------------------------------------------------------ //
    //  Test 4 : Afficher tous les Commentaires
    // ------------------------------------------------------------------ //

    @Test
    @Order(4)
    @DisplayName("Test 4 : Afficher tous les Commentaires")
    void testAfficherCommentaires() {
        Commentaire commentaire = new Commentaire("Commentaire Affichage Test", ID_POST_REF);
        service.ajouterCommentaire(commentaire);
        idCommentaireTest = commentaire.getIdCommentaire();

        List<Commentaire> commentaires = service.afficherCommentaires();

        assertNotNull(commentaires, "La liste ne doit pas être null");
        assertFalse(commentaires.isEmpty(), "La liste doit contenir au moins un commentaire");
        assertTrue(commentaires.stream()
                        .anyMatch(c -> c.getIdCommentaire() == idCommentaireTest),
                "Le commentaire inséré doit figurer dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 5 : Récupérer un Commentaire par ID
    // ------------------------------------------------------------------ //

    @Test
    @Order(5)
    @DisplayName("Test 5 : Récupérer un Commentaire par ID")
    void testGetCommentaireById() {
        Commentaire commentaire = new Commentaire("Commentaire GetById Test", ID_POST_REF);
        service.ajouterCommentaire(commentaire);
        idCommentaireTest = commentaire.getIdCommentaire();

        Commentaire recupere = service.getCommentaireById(idCommentaireTest);

        assertNotNull(recupere, "Le commentaire doit être trouvé par son ID");
        assertEquals(idCommentaireTest, recupere.getIdCommentaire());
        assertEquals("Commentaire GetById Test", recupere.getContenu());
        assertEquals(ID_POST_REF, recupere.getIdPost());
    }

    // ------------------------------------------------------------------ //
    //  Test 6 : Récupérer les Commentaires d'un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(6)
    @DisplayName("Test 6 : Récupérer les Commentaires d'un Post")
    void testGetCommentairesByPost() {
        Commentaire commentaire = new Commentaire("Commentaire ByPost Test", ID_POST_REF);
        service.ajouterCommentaire(commentaire);
        idCommentaireTest = commentaire.getIdCommentaire();

        List<Commentaire> commentairesDuPost = service.getCommentairesByPost(ID_POST_REF);

        assertNotNull(commentairesDuPost, "La liste ne doit pas être null");
        assertTrue(commentairesDuPost.stream()
                        .anyMatch(c -> c.getIdCommentaire() == idCommentaireTest),
                "Le commentaire inséré doit apparaître dans la liste du post");
        assertTrue(commentairesDuPost.stream()
                        .allMatch(c -> c.getIdPost() == ID_POST_REF),
                "Tous les commentaires retournés doivent appartenir au post " + ID_POST_REF);
    }
}