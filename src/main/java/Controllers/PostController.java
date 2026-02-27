package Controllers;

import Entities.Post;
import Entities.Commentaire;
import Entities.Reaction;
import Services.Posting_Services;
import Services.Commentaire_Services;
import Services.Reaction_Services;
import Iservices.IpostServices;
import Iservices.IcommentaireServices;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostController {

    @FXML private TableView<Post> tableView;
    @FXML private TableColumn<Post, Integer>       colId;
    @FXML private TableColumn<Post, String>        colContenu;
    @FXML private TableColumn<Post, String>        colMedia;
    @FXML private TableColumn<Post, LocalDateTime> colDate;
    @FXML private TableColumn<Post, String>        colStatut;
    @FXML private TableColumn<Post, Void>          colActions;

    @FXML private TextArea        txtContenu;
    @FXML private TextField       txtMedia;
    @FXML private TextField       txtSearch;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private Label           lblCharCount;
    @FXML private Label           lblValidation;
    @FXML private Label           lblTotalPosts;
    @FXML private Label           lblPublies;
    @FXML private Label           lblBrouillons;
    @FXML private Label           lblArchives;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnAnnuler;
    @FXML private Button btnBrowseImage;
    @FXML private Button btnVoirPosts;
    @FXML private Button btnNavComments;
    @FXML private Button btnNavReactions;
    @FXML private Button btnRetourCatalogue;

    private IpostServices        postService;
    private IcommentaireServices commentaireService;
    private IreactionServices    reactionService;
    private ObservableList<Post> postsList;
    private List<Post>           allPosts;
    private Post                 selectedPost = null;

    // ── Textarea style constants (light bg, BLACK text) ──────────────────
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
        postService        = new Posting_Services();
        commentaireService = new Commentaire_Services();
        reactionService    = new Reaction_Services();
        postsList          = FXCollections.observableArrayList();

        // ComboBoxes
        cbStatut.getItems().addAll("publie", "brouillon", "archive");
        cbStatut.setValue("brouillon");

        cbFilterStatut.getItems().addAll("Tous", "publie", "brouillon", "archive");
        cbFilterStatut.setValue("Tous");
        cbFilterStatut.setOnAction(e -> applyFilters());

        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("idPost"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colMedia.setCellValueFactory(new PropertyValueFactory<>("media"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        // Statut column — colored badge
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); return; }
                String color = switch (statut) {
                    case "publie"    -> "#4caf50";
                    case "brouillon" -> "#ed8936";
                    case "archive"   -> "#718096";
                    default          -> "#999";
                };
                Label badge = new Label(statut.toUpperCase());
                badge.setPadding(new Insets(3, 10, 3, 10));
                badge.setFont(Font.font("Arial", FontWeight.BOLD, 10));
                badge.setTextFill(Color.WHITE);
                badge.setStyle("-fx-background-color:" + color + "; -fx-background-radius:10;");
                HBox box = new HBox(badge);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        // Actions column — View + Edit + Delete
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView   = new Button("👁 Détails");
            private final Button btnEdit   = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            {
                btnView.setStyle(
                        "-fx-background-color: #4caf50; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 12px;");
                btnEdit.setStyle(
                        "-fx-background-color: #1565c0; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;");
                btnDelete.setStyle(
                        "-fx-background-color: #e53e3e; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10;");

                btnView.setOnAction(e ->
                        showPostDetailModal(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e ->
                        loadPostToForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        supprimerPost(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, btnView, btnEdit, btnDelete);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        // Character counter
        txtContenu.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            lblCharCount.setText(len + "/500");
            if (len < 10 || len > 500) {
                lblCharCount.setTextFill(Color.web("#e53e3e"));
                txtContenu.setStyle(TA_ERROR);
            } else {
                lblCharCount.setTextFill(Color.web("#4caf50"));
                txtContenu.setStyle(TA_OK);
            }
        });

        // Search
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        txtContenu.setStyle(TA_DEFAULT);
        refreshTable();
        updateStatistics();
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETAIL MODAL
    // ══════════════════════════════════════════════════════════════════════

    private void showPostDetailModal(Post post) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Détails — Post #" + post.getIdPost());
        modal.setResizable(true);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f4f0;");

        // ── Header ──
        HBox header = new HBox();
        header.setStyle(
                "-fx-background-color: #1a1d27; -fx-padding: 18 28;" +
                        "-fx-border-color: #2d5a2d; -fx-border-width: 0 0 2 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(12);

        String badgeColor = switch (post.getStatut()) {
            case "publie"    -> "#4caf50";
            case "brouillon" -> "#ed8936";
            case "archive"   -> "#718096";
            default          -> "#999";
        };
        Label statusBadge = new Label(post.getStatut().toUpperCase());
        statusBadge.setStyle(
                "-fx-background-color:" + badgeColor + "; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 12;");

        Label titleLabel = new Label("Post #" + post.getIdPost());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕ Fermer");
        closeBtn.setStyle(
                "-fx-background-color: #e53e3e; -fx-text-fill: white;" +
                        "-fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 8 16; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> modal.close());

        header.getChildren().addAll(titleLabel, statusBadge, spacer, closeBtn);
        root.setTop(header);

        // ── Scrollable body ──
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: #f0f4f0; -fx-background: #f0f4f0;");
        scrollPane.setFitToWidth(true);

        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 28, 24, 28));
        body.setStyle("-fx-background-color: #f0f4f0;");

        // Post image
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            try {
                File f = new File(post.getMedia());
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(640);
                    iv.setFitHeight(300);
                    iv.setPreserveRatio(true);
                    HBox imgBox = new HBox(iv);
                    imgBox.setAlignment(Pos.CENTER);
                    imgBox.setStyle(
                            "-fx-background-color: white; -fx-background-radius: 12;" +
                                    "-fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
                    body.getChildren().add(imgBox);
                }
            } catch (Exception ignored) {}
        }

        // Content card
        VBox contentCard = new VBox(10);
        contentCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        Label contentHeader = new Label("📝  Contenu");
        contentHeader.setStyle("-fx-font-size: 13px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");
        Label contentText = new Label(post.getContenu());
        contentText.setStyle("-fx-text-fill: #111111; -fx-font-size: 15px;");
        contentText.setWrapText(true);
        Label dateText = new Label("📅  " + post.getDateCreation().toString().replace("T", "   "));
        dateText.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
        contentCard.getChildren().addAll(contentHeader, contentText, dateText);
        body.getChildren().add(contentCard);

        // ── Reactions ──
        List<Reaction> reactions = reactionService.afficherReactions()
                .stream()
                .filter(r -> r.getIdPost() != null && r.getIdPost() == post.getIdPost())
                .collect(Collectors.toList());

        VBox reactCard = new VBox(12);
        reactCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        HBox reactHeader = new HBox(8);
        reactHeader.setAlignment(Pos.CENTER_LEFT);
        Label reactTitle = new Label("❤  Réactions");
        reactTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");
        Label reactCount = new Label("(" + reactions.size() + ")");
        reactCount.setStyle("-fx-text-fill: #999; -fx-font-size: 13px;");
        reactHeader.getChildren().addAll(reactTitle, reactCount);
        reactCard.getChildren().add(reactHeader);

        if (reactions.isEmpty()) {
            Label noReact = new Label("Aucune réaction pour ce post.");
            noReact.setStyle("-fx-text-fill: #bbb; -fx-font-size: 13px;");
            reactCard.getChildren().add(noReact);
        } else {
            HBox emojiRow = new HBox(12);
            emojiRow.setAlignment(Pos.CENTER_LEFT);
            Map<String, Long> grouped = reactions.stream()
                    .collect(Collectors.groupingBy(Reaction::getType, Collectors.counting()));
            Map<String, String> emojiMap = Map.of(
                    "LIKE","👍","LOVE","❤","HAHA","😂","WOW","😮","SAD","😢","ANGRY","😠");
            for (var entry : grouped.entrySet()) {
                VBox eb = new VBox(4);
                eb.setAlignment(Pos.CENTER);
                eb.setStyle(
                        "-fx-background-color: #f0f4f0; -fx-background-radius: 10; -fx-padding: 12 18;");
                Label em = new Label(emojiMap.getOrDefault(entry.getKey(), "?"));
                em.setStyle("-fx-font-size: 24px;");
                Label cnt = new Label(String.valueOf(entry.getValue()));
                cnt.setStyle("-fx-text-fill: #111; -fx-font-size: 14px; -fx-font-weight: bold;");
                Label tp = new Label(entry.getKey());
                tp.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
                eb.getChildren().addAll(em, cnt, tp);
                emojiRow.getChildren().add(eb);
            }
            reactCard.getChildren().add(emojiRow);
        }
        body.getChildren().add(reactCard);

        // ── Comments ──
        List<Commentaire> commentaires = commentaireService.afficherCommentaires()
                .stream()
                .filter(c -> c.getIdPost() == post.getIdPost())
                .collect(Collectors.toList());

        VBox commCard = new VBox(12);
        commCard.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        HBox commHeader = new HBox(8);
        commHeader.setAlignment(Pos.CENTER_LEFT);
        Label commTitle = new Label("💬  Commentaires");
        commTitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");
        Label commCount = new Label("(" + commentaires.size() + ")");
        commCount.setStyle("-fx-text-fill: #999; -fx-font-size: 13px;");
        commHeader.getChildren().addAll(commTitle, commCount);
        commCard.getChildren().add(commHeader);

        if (commentaires.isEmpty()) {
            Label noComm = new Label("Aucun commentaire pour ce post.");
            noComm.setStyle("-fx-text-fill: #bbb; -fx-font-size: 13px;");
            commCard.getChildren().add(noComm);
        } else {
            for (Commentaire c : commentaires) {
                HBox row = new HBox(12);
                row.setStyle(
                        "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12 16;");
                row.setAlignment(Pos.CENTER_LEFT);

                Label avatar = new Label("👤");
                avatar.setStyle(
                        "-fx-font-size: 20px; -fx-background-color: #e8f5e9;" +
                                "-fx-background-radius: 20; -fx-padding: 6 8;");

                VBox cc = new VBox(4);
                HBox.setHgrow(cc, Priority.ALWAYS);
                Label cid = new Label("Commentaire #" + c.getIdCommentaire());
                cid.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px; -fx-font-weight: bold;");
                Label ctxt = new Label(c.getContenu());
                ctxt.setStyle("-fx-text-fill: #111111; -fx-font-size: 13px;");
                ctxt.setWrapText(true);
                Label cdate = new Label("📅 " +
                        (c.getDate() != null ? c.getDate().toString().replace("T","  ") : "—"));
                cdate.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
                cc.getChildren().addAll(cid, ctxt, cdate);
                row.getChildren().addAll(avatar, cc);
                commCard.getChildren().add(row);
            }
        }
        body.getChildren().add(commCard);

        scrollPane.setContent(body);
        root.setCenter(scrollPane);

        modal.setScene(new Scene(root, 720, 680));
        modal.show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // FILTERS
    // ══════════════════════════════════════════════════════════════════════

    private void applyFilters() {
        if (allPosts == null) return;
        String search = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
        String filter = cbFilterStatut != null ? cbFilterStatut.getValue() : "Tous";
        if (filter == null) filter = "Tous";
        final String f = filter;

        postsList.clear();
        allPosts.stream()
                .filter(p -> f.equals("Tous") || p.getStatut().equals(f))
                .filter(p -> search.isEmpty() || p.getContenu().toLowerCase().contains(search))
                .forEach(postsList::add);
        tableView.setItems(postsList);
    }

    // ══════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════

    @FXML private void navigateToComments() { navigate("/views/CommentaireView.fxml", "Gestion des Commentaires"); }
    @FXML private void navigateToReactions(){ navigate("/views/ReactionView.fxml",    "Gestion des Réactions"); }
    @FXML private void retourCatalogue()    { navigate("/views/Catalogue.fxml",       "Wanderlust - Catalogue"); }
    @FXML private void voirPostsCards()     { navigate("/views/PostCard.fxml",        "Posts - Vue Cartes"); }

    private void navigate(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnAjouter.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
            showMsg("Erreur navigation: " + e.getMessage(), false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FILE CHOOSER
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void browseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images","*.jpg","*.jpeg","*.png","*.gif","*.bmp"));
        File file = fc.showOpenDialog(btnBrowseImage.getScene().getWindow());
        if (file != null) txtMedia.setText(file.getAbsolutePath());
    }

    // ══════════════════════════════════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════════════════════════════════

    @FXML
    private void ajouterPost() {
        if (validateForm()) {
            Post post = new Post(
                    txtContenu.getText().trim(),
                    txtMedia.getText().trim().isEmpty() ? null : txtMedia.getText().trim(),
                    cbStatut.getValue()
            );
            postService.ajouterPost(post);
            showMsg("✅ Post ajouté avec succès!", true);
            clearForm(); refreshTable(); updateStatistics();
        }
    }

    @FXML
    private void modifierPost() {
        if (selectedPost != null && validateForm()) {
            selectedPost.setContenu(txtContenu.getText().trim());
            selectedPost.setMedia(txtMedia.getText().trim().isEmpty() ? null : txtMedia.getText().trim());
            selectedPost.setStatut(cbStatut.getValue());
            postService.modifierPost(selectedPost);
            showMsg("✅ Post modifié avec succès!", true);
            clearForm(); refreshTable(); updateStatistics();
        } else {
            showMsg("Veuillez sélectionner un post à modifier", false);
        }
    }

    @FXML private void annuler() { clearForm(); }

    private void supprimerPost(Post post) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le post #" + post.getIdPost());
        alert.setContentText("Êtes-vous sûr ? Cette action est irréversible.");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                postService.supprimerPost(post.getIdPost());
                refreshTable(); updateStatistics();
                showMsg("✅ Post supprimé avec succès!", true);
            }
        });
    }

    private void loadPostToForm(Post post) {
        selectedPost = post;
        txtContenu.setText(post.getContenu());
        txtMedia.setText(post.getMedia() != null ? post.getMedia() : "");
        cbStatut.setValue(post.getStatut());
        showMsg("Post chargé pour modification", true);
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION & HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private boolean validateForm() {
        String c = txtContenu.getText().trim();
        if (c.isEmpty())       { showMsg("Le contenu est obligatoire!", false);         return false; }
        if (c.length() < 10)   { showMsg("Minimum 10 caractères requis!", false);        return false; }
        if (c.length() > 500)  { showMsg("Maximum 500 caractères!", false);              return false; }
        if (cbStatut.getValue() == null) { showMsg("Le statut est obligatoire!", false); return false; }
        return true;
    }

    private void showMsg(String msg, boolean ok) {
        lblValidation.setText(msg);
        lblValidation.setTextFill(Color.web(ok ? "#4caf50" : "#e53e3e"));
    }

    private void clearForm() {
        txtContenu.clear();
        txtMedia.clear();
        cbStatut.setValue("brouillon");
        lblValidation.setText("");
        lblCharCount.setText("0/500");
        lblCharCount.setTextFill(Color.web("#999999"));
        txtContenu.setStyle(TA_DEFAULT);
        selectedPost = null;
    }

    private void refreshTable() {
        postsList.clear();
        allPosts = postService.afficherPosts();
        postsList.addAll(allPosts);
        tableView.setItems(postsList);
    }

    private void updateStatistics() {
        lblTotalPosts.setText(String.valueOf(postsList.size()));
        lblPublies.setText(String.valueOf(
                postsList.stream().filter(p -> "publie".equals(p.getStatut())).count()));
        lblBrouillons.setText(String.valueOf(
                postsList.stream().filter(p -> "brouillon".equals(p.getStatut())).count()));
        lblArchives.setText(String.valueOf(
                postsList.stream().filter(p -> "archive".equals(p.getStatut())).count()));
    }
}