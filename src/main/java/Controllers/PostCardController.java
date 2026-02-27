package Controllers;

import Entities.Post;
import Services.Posting_Services;
import Iservices.IpostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PostCardController {

    @FXML private FlowPane     flowPaneCards;
    @FXML private TextField    txtRecherche;
    @FXML private ToggleButton btnTous;
    @FXML private ToggleButton btnPublies;
    @FXML private ToggleButton btnBrouillons;
    @FXML private ToggleButton btnArchives;
    @FXML private Button       btnRetourTable;
    @FXML private Button       btnNavComments;
    @FXML private Button       btnNavReactions;

    private IpostServices postService;
    private List<Post>    allPosts;
    private String        currentFilter = "tous";

    @FXML
    public void initialize() {
        postService = new Posting_Services();

        ToggleGroup fg = new ToggleGroup();
        btnTous.setToggleGroup(fg);
        btnPublies.setToggleGroup(fg);
        btnBrouillons.setToggleGroup(fg);
        btnArchives.setToggleGroup(fg);
        btnTous.setSelected(true);

        txtRecherche.textProperty().addListener((obs, o, n) -> filterPosts());
        loadAllPosts();
    }

    // ── Navigation ──────────────────────────────────────────────────────

    @FXML private void goToComments()  { navigate("/views/CommentaireView.fxml", "Gestion des Commentaires"); }
    @FXML private void goToReactions() { navigate("/views/ReactionView.fxml",    "Gestion des Réactions"); }
    @FXML private void retourTable()   { navigate("/views/PostView.fxml",        "Gestion des Posts"); }

    private void navigate(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetourTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Filters ─────────────────────────────────────────────────────────

    @FXML private void filtrerTous()       { currentFilter = "tous";      filterPosts(); }
    @FXML private void filtrerPublies()    { currentFilter = "publie";    filterPosts(); }
    @FXML private void filtrerBrouillons() { currentFilter = "brouillon"; filterPosts(); }
    @FXML private void filtrerArchives()   { currentFilter = "archive";   filterPosts(); }

    private void filterPosts() {
        String search = txtRecherche.getText().toLowerCase().trim();
        List<Post> filtered = allPosts.stream()
                .filter(p -> currentFilter.equals("tous") || p.getStatut().equals(currentFilter))
                .filter(p -> search.isEmpty() || p.getContenu().toLowerCase().contains(search))
                .collect(Collectors.toList());
        displayPosts(filtered);
    }

    // ── Load & Display ───────────────────────────────────────────────────

    private void loadAllPosts() {
        allPosts = postService.afficherPosts();
        displayPosts(allPosts);
    }

    private void displayPosts(List<Post> posts) {
        flowPaneCards.getChildren().clear();
        if (posts.isEmpty()) {
            Label lbl = new Label("Aucun post trouvé");
            lbl.setStyle("-fx-font-size: 18px; -fx-text-fill: #bbb; -fx-padding: 60;");
            flowPaneCards.getChildren().add(lbl);
            return;
        }
        for (Post p : posts) flowPaneCards.getChildren().add(createPostCard(p));
    }

    // ── Card Builder ─────────────────────────────────────────────────────

    private VBox createPostCard(Post post) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-pref-width: 300; -fx-max-width: 300;");

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #f8fff8;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.35), 20, 0, 0, 6);" +
                        "-fx-pref-width: 300; -fx-max-width: 300; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 3);" +
                        "-fx-pref-width: 300; -fx-max-width: 300;"));

        // Status badge
        String badgeColor = switch (post.getStatut()) {
            case "publie"    -> "#4caf50";
            case "brouillon" -> "#ed8936";
            case "archive"   -> "#718096";
            default          -> "#999999";
        };
        Label statusBadge = new Label(post.getStatut().toUpperCase());
        statusBadge.setPadding(new Insets(4, 12, 4, 12));
        statusBadge.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        statusBadge.setTextFill(Color.WHITE);
        statusBadge.setStyle(
                "-fx-background-color:" + badgeColor + "; -fx-background-radius: 10;");

        // Image
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            try {
                File f = new File(post.getMedia());
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(260);
                    iv.setFitHeight(160);
                    iv.setPreserveRatio(true);
                    card.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        // Post ID
        Label idLabel = new Label("Post #" + post.getIdPost());
        idLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        idLabel.setTextFill(Color.web("#4caf50"));

        // Content
        String preview = post.getContenu().length() > 120
                ? post.getContenu().substring(0, 120) + "…"
                : post.getContenu();
        Label contentLabel = new Label(preview);
        contentLabel.setWrapText(true);
        contentLabel.setFont(Font.font("Arial", 13));
        contentLabel.setTextFill(Color.web("#111111"));
        contentLabel.setMaxHeight(80);

        // Date
        String dateStr = post.getDateCreation().toString().replace("T","  ");
        if (dateStr.length() > 16) dateStr = dateStr.substring(0, 16);
        Label dateLabel = new Label("📅  " + dateStr);
        dateLabel.setFont(Font.font("Arial", 11));
        dateLabel.setTextFill(Color.web("#999999"));

        // Divider
        Label divider = new Label();
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #f0f0f0;");

        // Button
        Button btnView = new Button("👁  Voir les détails");
        btnView.setMaxWidth(Double.MAX_VALUE);
        btnView.setStyle(
                "-fx-background-color: #4caf50; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-padding: 9 0; -fx-font-size: 13px; -fx-font-weight: bold;");
        btnView.setOnMouseEntered(e -> btnView.setStyle(
                "-fx-background-color: #388e3c; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-padding: 9 0; -fx-font-size: 13px; -fx-font-weight: bold;"));
        btnView.setOnMouseExited(e -> btnView.setStyle(
                "-fx-background-color: #4caf50; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-cursor: hand;" +
                        "-fx-padding: 9 0; -fx-font-size: 13px; -fx-font-weight: bold;"));
        btnView.setOnAction(e -> showDetail(post));

        card.getChildren().addAll(statusBadge, idLabel, contentLabel, dateLabel, divider, btnView);
        return card;
    }

    // ── Detail popup ─────────────────────────────────────────────────────

    private void showDetail(Post post) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails — Post #" + post.getIdPost());
        alert.setHeaderText("Post #" + post.getIdPost() + "  [" + post.getStatut().toUpperCase() + "]");
        alert.setContentText(
                "📝 Contenu:\n" + post.getContenu() + "\n\n" +
                        "📅 Date:\n"    + post.getDateCreation() + "\n\n" +
                        "🖼 Media:\n"   + (post.getMedia() != null ? post.getMedia() : "Aucun"));
        alert.showAndWait();
    }
}