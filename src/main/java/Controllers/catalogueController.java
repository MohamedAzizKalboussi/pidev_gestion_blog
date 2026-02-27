package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.io.IOException;

public class catalogueController {

    @FXML private TextField    txtRecherche;
    @FXML private Button       recherche;
    @FXML private MenuButton   btnGestionBlog;
    @FXML private Button       orgactivite;
    @FXML private Button       orgevent;
    @FXML private Tab          tabActivites;
    @FXML private Tab          tabEvents;
    @FXML private ScrollPane   scrollPaneActivites;
    @FXML private ScrollPane   scrollPaneEvents;
    @FXML private FlowPane     flowActivites;
    @FXML private FlowPane     flowEvents;

    @FXML
    public void initialize() {
        System.out.println("Catalogue initialized");
    }

    @FXML
    private void handleRecherche() {
        String searchText = txtRecherche.getText();
        System.out.println("Searching for: " + searchText);
    }

    @FXML private void ouvrirGestionPosts()        { navigate("/views/PostView.fxml",        "Gestion des Posts"); }
    @FXML private void ouvrirGestionCommentaires() { navigate("/views/CommentaireView.fxml", "Gestion des Commentaires"); }
    @FXML private void ouvrirGestionReactions()    { navigate("/views/ReactionView.fxml",    "Gestion des Réactions"); }
    @FXML private void ouvrirGestionActivites()    { System.out.println("Opening Activity Management..."); }
    @FXML private void ouvrirOrganisation()        { System.out.println("Opening Event Organization..."); }

    private void navigate(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) recherche.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 800);
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger: " + title);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}