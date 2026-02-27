package Controllers;

import Entities.Commentaire;
import Entities.Post;
import Services.Commentaire_Services;
import Services.Posting_Services;
import Iservices.IcommentaireServices;
import Iservices.IpostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;

public class CommentaireController {

    @FXML private TableView<Commentaire>               tableView;
    @FXML private TableColumn<Commentaire, Integer>    colId;
    @FXML private TableColumn<Commentaire, String>     colContenu;
    @FXML private TableColumn<Commentaire, Integer>    colIdPost;
    @FXML private TableColumn<Commentaire, LocalDateTime> colDate;
    @FXML private TableColumn<Commentaire, Void>       colActions;

    @FXML private TextArea       txtContenu;
    @FXML private ComboBox<Post> cbPost;
    @FXML private Label          lblCharCount;
    @FXML private Label          lblValidation;
    @FXML private Label          lblTotalCommentaires;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnAnnuler;

    private IcommentaireServices        commentaireService;
    private IpostServices               postService;
    private ObservableList<Commentaire> commentairesList;
    private Commentaire                 selectedCommentaire = null;

    private static final String TA_DEFAULT =
            "-fx-background-color: #f8f9fa; -fx-text-fill: #111111;" +
                    "-fx-prompt-text-fill: #aaaaaa; -fx-border-color: #d0d0d0;" +
                    "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;" +
                    "-fx-padding: 12; -fx-font-size: 14px;";

    private static final String TA_OK =
            "-fx-background-color: #f8f9fa; -fx-text-fill: #111111;" +
                    "-fx-prompt-text-fill: #aaaaaa; -fx-border-color: #4caf50;" +
                    "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;" +
                    "-fx-padding: 12; -fx-font-size: 14px;";

    private static final String TA_ERROR =
            "-fx-background-color: #f8f9fa; -fx-text-fill: #111111;" +
                    "-fx-prompt-text-fill: #aaaaaa; -fx-border-color: #e53e3e;" +
                    "-fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;" +
                    "-fx-padding: 12; -fx-font-size: 14px;";

    @FXML
    public void initialize() {
        commentaireService = new Commentaire_Services();
        postService        = new Posting_Services();
        commentairesList   = FXCollections.observableArrayList();

        colId.setCellValueFactory(new PropertyValueFactory<>("idCommentaire"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colIdPost.setCellValueFactory(new PropertyValueFactory<>("idPost"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Post-ID badge
        colIdPost.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) { setGraphic(null); return; }
                Label badge = new Label("Post #" + id);
                badge.setStyle(
                        "-fx-background-color: #e8f5e9; -fx-text-fill: #2d5a2d;" +
                                "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 11px; -fx-font-weight: bold;");
                HBox box = new HBox(badge); box.setAlignment(Pos.CENTER); setGraphic(box);
            }
        });

        // Actions column
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            {
                btnEdit.setStyle(
                        "-fx-background-color: #1565c0; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;");
                btnDelete.setStyle(
                        "-fx-background-color: #e53e3e; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;");
                btnEdit.setOnAction(e ->
                        loadCommentaireToForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        supprimerCommentaire(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(8, btnEdit, btnDelete);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        // Post ComboBox
        ObservableList<Post> posts = FXCollections.observableArrayList(postService.afficherPosts());
        cbPost.setItems(posts);
        cbPost.setConverter(new javafx.util.StringConverter<Post>() {
            @Override public String toString(Post p) {
                return p != null ? "Post #" + p.getIdPost() + " – " +
                        (p.getContenu().length() > 50 ? p.getContenu().substring(0,50) + "…" : p.getContenu()) : "";
            }
            @Override public Post fromString(String s) { return null; }
        });

        // Character counter
        txtContenu.textProperty().addListener((obs, o, n) -> {
            int len = n.length();
            lblCharCount.setText(len + "/300");
            boolean ok = len >= 5 && len <= 300;
            lblCharCount.setTextFill(Color.web(ok ? "#4caf50" : "#e53e3e"));
            txtContenu.setStyle(ok ? TA_OK : TA_ERROR);
        });

        txtContenu.setStyle(TA_DEFAULT);
        refreshTable();
        updateStatistics();
    }

    // ── Navigation ──────────────────────────────────────────────────────

    @FXML private void retourCatalogue() { navigate("/views/Catalogue.fxml",       "Wanderlust - Catalogue"); }
    @FXML private void goToPosts()       { navigate("/views/PostView.fxml",         "Gestion des Posts"); }
    @FXML private void goToReactions()   { navigate("/views/ReactionView.fxml",     "Gestion des Réactions"); }

    private void navigate(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) btnAjouter.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
            showMsg("Erreur navigation: " + e.getMessage(), false);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────

    @FXML
    private void ajouterCommentaire() {
        if (validateForm()) {
            commentaireService.ajouterCommentaire(
                    new Commentaire(txtContenu.getText().trim(), cbPost.getValue().getIdPost()));
            showMsg("✅ Commentaire ajouté avec succès!", true);
            clearForm(); refreshTable(); updateStatistics();
        }
    }

    @FXML
    private void modifierCommentaire() {
        if (selectedCommentaire != null && validateForm()) {
            selectedCommentaire.setContenu(txtContenu.getText().trim());
            selectedCommentaire.setIdPost(cbPost.getValue().getIdPost());
            commentaireService.modifierCommentaire(selectedCommentaire);
            showMsg("✅ Commentaire modifié avec succès!", true);
            clearForm(); refreshTable(); updateStatistics();
        } else {
            showMsg("Veuillez sélectionner un commentaire à modifier", false);
        }
    }

    @FXML private void annuler() { clearForm(); }

    private void supprimerCommentaire(Commentaire c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le commentaire");
        alert.setContentText("Êtes-vous sûr ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                commentaireService.supprimerCommentaire(c.getIdCommentaire());
                refreshTable(); updateStatistics();
                showMsg("✅ Commentaire supprimé!", true);
            }
        });
    }

    private void loadCommentaireToForm(Commentaire c) {
        selectedCommentaire = c;
        txtContenu.setText(c.getContenu());
        cbPost.getItems().stream()
                .filter(p -> p.getIdPost() == c.getIdPost())
                .findFirst().ifPresent(cbPost::setValue);
        showMsg("Commentaire chargé pour modification", true);
    }

    // ── Validation ───────────────────────────────────────────────────────

    private boolean validateForm() {
        String t = txtContenu.getText().trim();
        if (t.isEmpty())       { showMsg("Le contenu est obligatoire!", false);    return false; }
        if (t.length() < 5)    { showMsg("Minimum 5 caractères requis!", false);   return false; }
        if (t.length() > 300)  { showMsg("Maximum 300 caractères!", false);        return false; }
        if (cbPost.getValue() == null) { showMsg("Sélectionnez un post!", false);  return false; }
        return true;
    }

    private void showMsg(String msg, boolean ok) {
        lblValidation.setText(msg);
        lblValidation.setTextFill(Color.web(ok ? "#4caf50" : "#e53e3e"));
    }

    private void clearForm() {
        txtContenu.clear();
        cbPost.setValue(null);
        lblValidation.setText("");
        lblCharCount.setText("0/300");
        lblCharCount.setTextFill(Color.web("#999999"));
        txtContenu.setStyle(TA_DEFAULT);
        selectedCommentaire = null;
    }

    private void refreshTable() {
        commentairesList.clear();
        commentairesList.addAll(commentaireService.afficherCommentaires());
        tableView.setItems(commentairesList);
    }

    private void updateStatistics() {
        lblTotalCommentaires.setText(String.valueOf(commentairesList.size()));
    }
}