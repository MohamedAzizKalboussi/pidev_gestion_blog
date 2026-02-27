package Services;

import Entities.Post;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Posting_Services
 * Ordre : Ajouter → Modifier → Supprimer
 * Nettoyage automatique après chaque test via @AfterEach
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostingServicesTest {

    private static Posting_Services service;

    // ID partagé entre les tests (ajouter → modifier → supprimer)
    private static int idPostTest;

    // ------------------------------------------------------------------ //
    //  Setup & Teardown
    // ------------------------------------------------------------------ //

    @BeforeAll
    static void setUp() {
        service = new Posting_Services();
    }

    /**
     * Nettoyage automatique : supprime le dernier post inséré
     * si le test suivant ne l'a pas déjà supprimé.
     */
    @AfterEach
    void cleanUp() {
        if (idPostTest != 0) {
            List<Post> posts = service.afficherPosts();
            boolean existe = posts.stream()
                    .anyMatch(p -> p.getIdPost() == idPostTest);
            if (existe) {
                service.supprimerPost(idPostTest);
                System.out.println("[CleanUp] Post de test supprimé (id=" + idPostTest + ")");
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Test 1 : Ajouter un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(1)
    @DisplayName("Test 1 : Ajouter un Post")
    void testAjouterPost() {
        Post post = new Post("Contenu Test JUnit", "media_test.jpg", "actif");
        service.ajouterPost(post);

        // L'ID doit avoir été alimenté par la BD
        assertTrue(post.getIdPost() > 0,
                "L'ID du post doit être > 0 après insertion");

        idPostTest = post.getIdPost();

        // Vérification via afficher
        List<Post> posts = service.afficherPosts();
        assertFalse(posts.isEmpty(), "La liste ne doit pas être vide");
        assertTrue(posts.stream()
                        .anyMatch(p -> p.getIdPost() == idPostTest),
                "Le post inséré doit être présent dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 2 : Modifier un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(2)
    @DisplayName("Test 2 : Modifier un Post")
    void testModifierPost() {
        // Pré-requis : insérer d'abord un post
        Post post = new Post("Contenu Avant Modif", "media_avant.jpg", "actif");
        service.ajouterPost(post);
        idPostTest = post.getIdPost();
        assertTrue(idPostTest > 0, "Le post de test doit être inséré");

        // Modification
        post.setContenu("Contenu Apres Modif");
        post.setStatut("inactif");
        service.modifierPost(post);

        // Vérification
        Post postModifie = service.getPostById(idPostTest);
        assertNotNull(postModifie, "Le post modifié doit exister en BD");
        assertEquals("Contenu Apres Modif", postModifie.getContenu(),
                "Le contenu doit être mis à jour");
        assertEquals("inactif", postModifie.getStatut(),
                "Le statut doit être mis à jour");
    }

    // ------------------------------------------------------------------ //
    //  Test 3 : Supprimer un Post
    // ------------------------------------------------------------------ //

    @Test
    @Order(3)
    @DisplayName("Test 3 : Supprimer un Post")
    void testSupprimerPost() {
        // Pré-requis : insérer un post à supprimer
        Post post = new Post("Contenu A Supprimer", "media_supp.jpg", "actif");
        service.ajouterPost(post);
        idPostTest = post.getIdPost();
        assertTrue(idPostTest > 0, "Le post de test doit être inséré");

        // Suppression
        service.supprimerPost(idPostTest);

        // Vérification : le post ne doit plus exister
        List<Post> posts = service.afficherPosts();
        boolean existe = posts.stream()
                .anyMatch(p -> p.getIdPost() == idPostTest);
        assertFalse(existe, "Le post supprimé ne doit plus exister dans la liste");

        // Marquer comme déjà supprimé pour éviter double nettoyage
        idPostTest = 0;
    }

    // ------------------------------------------------------------------ //
    //  Test 4 : Afficher tous les Posts
    // ------------------------------------------------------------------ //

    @Test
    @Order(4)
    @DisplayName("Test 4 : Afficher tous les Posts")
    void testAfficherPosts() {
        // Ajouter un post de référence
        Post post = new Post("Contenu Affichage Test", "media_aff.jpg", "actif");
        service.ajouterPost(post);
        idPostTest = post.getIdPost();

        List<Post> posts = service.afficherPosts();

        assertNotNull(posts, "La liste retournée ne doit pas être null");
        assertFalse(posts.isEmpty(), "La liste doit contenir au moins un post");
        assertTrue(posts.stream()
                        .anyMatch(p -> p.getIdPost() == idPostTest),
                "Le post inséré doit figurer dans la liste");
    }

    // ------------------------------------------------------------------ //
    //  Test 5 : Récupérer un Post par ID
    // ------------------------------------------------------------------ //

    @Test
    @Order(5)
    @DisplayName("Test 5 : Récupérer un Post par ID")
    void testGetPostById() {
        Post post = new Post("Contenu GetById Test", "media_get.jpg", "actif");
        service.ajouterPost(post);
        idPostTest = post.getIdPost();

        Post recupere = service.getPostById(idPostTest);

        assertNotNull(recupere, "Le post doit être trouvé par son ID");
        assertEquals(idPostTest, recupere.getIdPost());
        assertEquals("Contenu GetById Test", recupere.getContenu());
    }

    // ------------------------------------------------------------------ //
    //  Test 6 : Récupérer Posts par Statut
    // ------------------------------------------------------------------ //

    @Test
    @Order(6)
    @DisplayName("Test 6 : Récupérer Posts par Statut")
    void testGetPostsByStatut() {
        Post post = new Post("Contenu Statut Test", "media_statut.jpg", "publie");
        service.ajouterPost(post);
        idPostTest = post.getIdPost();

        List<Post> postsPublies = service.getPostsByStatut("publie");

        assertNotNull(postsPublies, "La liste ne doit pas être null");
        assertTrue(postsPublies.stream()
                        .anyMatch(p -> p.getIdPost() == idPostTest),
                "Le post avec le statut 'publie' doit apparaître dans la liste filtrée");
        assertTrue(postsPublies.stream()
                        .allMatch(p -> "publie".equals(p.getStatut())),
                "Tous les posts retournés doivent avoir le statut 'publie'");
    }
}