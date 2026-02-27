package Controllers;

import Entities.Commentaire;
import Entities.Post;
import Entities.Reaction;
import Services.Commentaire_Services;
import Services.Posting_Services;
import Services.Reaction_Services;
import Iservices.IcommentaireServices;
import Iservices.IpostServices;
import Iservices.IreactionServices;
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
import java.util.Map;

public class ReactionController {

    @FXML private TableView<Reaction>               tableView;
    @FXML private TableColumn<Reaction, Integer>       colId;
    @FXML private TableColumn<Reaction, String>        colType;
    @FXML private TableColumn<Reaction, Integer>       colIdPost;
    @FXML private TableColumn<Reaction, Integer>       colIdCommentaire;
    @FXML private TableColumn<Reaction, LocalDateTime> colDate;
    @FXML private TableColumn<Reaction, Void>          colActions;

    @FXML private ComboBox<String>      cbType;
    @FXML private ComboBox<Post>        cbPost;
    @FXML private ComboBox<Commentaire> cbCommentaire;
    @FXML private RadioButton           rbPost;
    @FXML private RadioButton           rbCommentaire;
    @FXML private Label                 lblValidation;
    @FXML private Label                 lblTotalReactions;
    @FXML private Label                 lblLikes;
    @FXML private Label                 lblLoves;
    @FXML private Label                 lblHahas;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnAnnuler;

    private IreactionServices    reactionService;
    private IpostServices        postService;
    private IcommentaireServices commentaireService;
    private ObservableList<Reaction> reactionsList;
    private Reaction selectedReaction = null;

    private static final Map<String, String> EMOJI = Map.of(
            "LIKE","👍","LOVE","❤","HAHA","😂","WOW","😮","SAD","😢","ANGRY","😠");

    @FXML
    public void initialize() {
        reactionService    = new Reaction_Services();
        postService        = new Posting_Services();
        commentaireService = new Commentaire_Services();
        reactionsList      = FXCollections.observableArrayList();

        cbType.getItems().addAll("LIKE","LOVE","HAHA","WOW","SAD","ANGRY");

        ToggleGroup tg = new ToggleGroup();
        rbPost.setToggleGroup(tg);
        rbCommentaire.setToggleGroup(tg);
        rbPost.setSelected(true);

        rbPost.selectedProperty().addListener((obs,o,n) -> {
            cbPost.setDisable(!n); cbCommentaire.setDisable(n); });
        rbCommentaire.selectedProperty().addListener((obs,o,n) -> {
            cbPost.setDisable(n); cbCommentaire.setDisable(!n); });

        // Posts
        ObservableList<Post> posts = FXCollections.observableArrayList(postService.afficherPosts());
        cbPost.setItems(posts);
        cbPost.setConverter(new javafx.util.StringConverter<Post>() {
            @Override public String toString(Post p) {
                return p != null ? "Post #" + p.getIdPost() + " – " +
                        (p.getContenu().length() > 40 ? p.getContenu().substring(0,40) + "…" : p.getContenu()) : "";
            }
            @Override public Post fromString(String s) { return null; }
        });

        // Commentaires
        ObservableList<Commentaire> comms =
                FXCollections.observableArrayList(commentaireService.afficherCommentaires());
        cbCommentaire.setItems(comms);
        cbCommentaire.setConverter(new javafx.util.StringConverter<Commentaire>() {
            @Override public String toString(Commentaire c) {
                return c != null ? "Comment #" + c.getIdCommentaire() + " – " +
                        (c.getContenu().length() > 40 ? c.getContenu().substring(0,40) + "…" : c.getContenu()) : "";
            }
            @Override public Commentaire fromString(String s) { return null; }
        });
        cbCommentaire.setDisable(true);

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("idReaction"));
        colIdPost.setCellValueFactory(new PropertyValueFactory<>("idPost"));
        colIdCommentaire.setCellValueFactory(new PropertyValueFactory<>("idCommentaire"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Type — emoji badge
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); return; }
                Label badge = new Label(EMOJI.getOrDefault(type,"?") + "  " + type);
                badge.setStyle(
                        "-fx-background-color: #e8f5e9; -fx-text-fill: #2d5a2d;" +
                                "-fx-background-radius: 10; -fx-padding: 3 10;" +
                                "-fx-font-size: 12px; -fx-font-weight: bold;");
                HBox box = new HBox(badge); box.setAlignment(Pos.CENTER); setGraphic(box);
            }
        });

        // Actions
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
                        loadReactionToForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        supprimerReaction(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(8, btnEdit, btnDelete);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        refreshTable();
        updateStatistics();
    }

    // ── Navigation ──────────────────────────────────────────────────────

    @FXML private void retourCatalogue() { navigate("/views/Catalogue.fxml",       "Wanderlust - Catalogue"); }
    @FXML private void goToPosts()       { navigate("/views/PostView.fxml",         "Gestion des Posts"); }
    @FXML private void goToComments()    { navigate("/views/CommentaireView.fxml",  "Gestion des Commentaires"); }

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
    private void ajouterReaction() {
        if (!validateForm()) return;
        Reaction r = rbPost.isSelected()
                ? new Reaction(cbType.getValue(), cbPost.getValue().getIdPost(), null)
                : new Reaction(cbType.getValue(), null, cbCommentaire.getValue().getIdCommentaire());
        reactionService.ajouterReaction(r);
        showMsg("✅ Réaction ajoutée!", true);
        clearForm(); refreshTable(); updateStatistics();
    }

    @FXML
    private void modifierReaction() {
        if (selectedReaction == null) {
            showMsg("Sélectionnez une réaction à modifier", false); return; }
        if (!validateForm()) return;
        selectedReaction.setType(cbType.getValue());
        if (rbPost.isSelected()) {
            selectedReaction.setIdPost(cbPost.getValue().getIdPost());
            selectedReaction.setIdCommentaire(null);
        } else {
            selectedReaction.setIdPost(null);
            selectedReaction.setIdCommentaire(cbCommentaire.getValue().getIdCommentaire());
        }
        reactionService.modifierReaction(selectedReaction);
        showMsg("✅ Réaction modifiée!", true);
        clearForm(); refreshTable(); updateStatistics();
    }

    @FXML private void annuler() { clearForm(); }

    private void supprimerReaction(Reaction r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la réaction");
        alert.setContentText("Êtes-vous sûr ?");
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                reactionService.supprimerReaction(r.getIdReaction());
                refreshTable(); updateStatistics();
                showMsg("✅ Réaction supprimée!", true);
            }
        });
    }

    private void loadReactionToForm(Reaction r) {
        selectedReaction = r;
        cbType.setValue(r.getType());
        if (r.getIdPost() != null) {
            rbPost.setSelected(true);
            cbPost.setDisable(false); cbCommentaire.setDisable(true);
            cbPost.getItems().stream()
                    .filter(p -> p.getIdPost() == r.getIdPost())
                    .findFirst().ifPresent(cbPost::setValue);
        } else if (r.getIdCommentaire() != null) {
            rbCommentaire.setSelected(true);
            cbPost.setDisable(true); cbCommentaire.setDisable(false);
            cbCommentaire.getItems().stream()
                    .filter(c -> c.getIdCommentaire() == r.getIdCommentaire())
                    .findFirst().ifPresent(cbCommentaire::setValue);
        }
        showMsg("Réaction chargée pour modification", true);
    }

    // ── Validation ───────────────────────────────────────────────────────

    private boolean validateForm() {
        if (cbType.getValue() == null) {
            showMsg("Le type est obligatoire!", false); return false; }
        if (rbPost.isSelected() && cbPost.getValue() == null) {
            showMsg("Sélectionnez un post!", false); return false; }
        if (rbCommentaire.isSelected() && cbCommentaire.getValue() == null) {
            showMsg("Sélectionnez un commentaire!", false); return false; }
        return true;
    }

    private void showMsg(String msg, boolean ok) {
        lblValidation.setText(msg);
        lblValidation.setTextFill(Color.web(ok ? "#4caf50" : "#e53e3e"));
    }

    private void clearForm() {
        cbType.setValue(null); cbPost.setValue(null); cbCommentaire.setValue(null);
        rbPost.setSelected(true); cbPost.setDisable(false); cbCommentaire.setDisable(true);
        lblValidation.setText(""); selectedReaction = null;
    }

    private void refreshTable() {
        reactionsList.clear();
        reactionsList.addAll(reactionService.afficherReactions());
        tableView.setItems(reactionsList);
    }

    private void updateStatistics() {
        lblTotalReactions.setText(String.valueOf(reactionsList.size()));
        lblLikes.setText(String.valueOf(
                reactionsList.stream().filter(r -> "LIKE".equals(r.getType())).count()));
        lblLoves.setText(String.valueOf(
                reactionsList.stream().filter(r -> "LOVE".equals(r.getType())).count()));
        lblHahas.setText(String.valueOf(
                reactionsList.stream().filter(r -> "HAHA".equals(r.getType())).count()));
    }
}