package Controllers;

import Entities.Commentaire;
import Entities.Post;
import Entities.Reaction;
import Services.AI_ModerationService;
import Services.ModerationResult;
import Services.Commentaire_Services;
import Services.Posting_Services;
import Services.Reaction_Services;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlogController implements Initializable {

    // ═══════════════════════════════════════════
    // FXML Injections
    // ═══════════════════════════════════════════

    @FXML private TextArea         newPostContent;
    @FXML private TextField        newPostMediaPath;
    @FXML private Label            postErrorLabel;
    @FXML private Button           publishBtn;
    @FXML private ComboBox<String> visibilityCombo;
    @FXML private VBox             postsFeedContainer;
    @FXML private VBox             emptyStateBox;
    @FXML private Label            postCountLabel;
    @FXML private Label            currentUsernameLabel;
    @FXML private ScrollPane       mainScrollPane;
    @FXML private VBox             archiveView;
    @FXML private VBox             archiveFeedContainer;
    @FXML private VBox             archiveEmptyBox;
    @FXML private Label            archiveCountLabel;

    // ═══════════════════════════════════════════
    // Services
    // ═══════════════════════════════════════════

    private final Posting_Services     postService       = new Posting_Services();
    private final Commentaire_Services commentService    = new Commentaire_Services();
    private final Reaction_Services    reactionService   = new Reaction_Services();
    private final AI_ModerationService moderationService = new AI_ModerationService();

    // ═══════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════

    private final ObservableList<Post>  postsList       = FXCollections.observableArrayList();
    private final Map<Integer, Integer> reportCounts    = new HashMap<>();
    private final Set<Integer>          hiddenPostIds   = new HashSet<>();
    private final Set<Integer>          savedPostIds    = new HashSet<>();
    private final Set<Integer>          aiHiddenPostIds = new HashSet<>();

    private final List<LocalDateTime>   commentTimestamps      = new ArrayList<>();
    private static final int            MAX_COMMENTS_IN_WINDOW = 3;
    private static final int            COMMENT_WINDOW_MINUTES = 3;

    private static final int    MAX_REPORTS  = 10;
    private static final String CURRENT_USER = "CurrentUser";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── FIX: Pre-built gradient backgrounds using JavaFX API (not CSS) ───────
    // JavaFX inline CSS does NOT support linear-gradient(). Use Background API.
    private static final Background AVATAR_BG_LARGE = buildGradientBackground(
            Color.web("#4F46E5"), Color.web("#7C3AED"), 38);
    private static final Background AVATAR_BG_SMALL = buildGradientBackground(
            Color.web("#4F46E5"), Color.web("#7C3AED"), 28);

    private static Background buildGradientBackground(Color from, Color to, double radius) {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, from), new Stop(1, to));
        BackgroundFill fill = new BackgroundFill(
                gradient,
                new CornerRadii(radius),
                Insets.EMPTY);
        return new Background(fill);
    }
    // ─────────────────────────────────────────────────────────────────────────

    // ═══════════════════════════════════════════
    // Init
    // ═══════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUsernameLabel.setText(CURRENT_USER);
        visibilityCombo.setItems(
                FXCollections.observableArrayList("🌐 Public", "🔒 Private"));
        visibilityCombo.getSelectionModel().selectFirst();
        loadPosts();
    }

    // ═══════════════════════════════════════════
    // Data
    // ═══════════════════════════════════════════

    private void loadPosts() {
        postsList.setAll(postService.afficherPosts());
        rebuildFeed();
    }

    private void rebuildFeed() {
        postsFeedContainer.getChildren().clear();
        int visible = 0;
        for (Post post : postsList) {
            if (hiddenPostIds.contains(post.getIdPost()))   continue;
            if (aiHiddenPostIds.contains(post.getIdPost())) continue;

            VBox card = buildPostCard(post, false);
            card.setOpacity(0);
            postsFeedContainer.getChildren().add(card);
            FadeTransition ft = new FadeTransition(Duration.millis(250), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
            visible++;
        }
        boolean empty = visible == 0;
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        postCountLabel.setText("· " + visible + (visible == 1 ? " post" : " posts"));
    }

    private void rebuildArchive() {
        archiveFeedContainer.getChildren().clear();
        List<Post> saved = new ArrayList<>();
        for (Post p : postsList) {
            if (savedPostIds.contains(p.getIdPost())) saved.add(p);
        }
        for (Post post : saved) {
            archiveFeedContainer.getChildren().add(buildPostCard(post, true));
        }
        boolean empty = saved.isEmpty();
        archiveEmptyBox.setVisible(empty);
        archiveEmptyBox.setManaged(empty);
        archiveCountLabel.setText("· " + saved.size() + " saved");
    }

    // ═══════════════════════════════════════════
    // AI Moderation — async, never blocks UI
    // ═══════════════════════════════════════════

    private void runModerationAsync(Post post, boolean isEdit) {
        showToast("🤖 AI is analyzing your post…");
        CompletableFuture
                .supplyAsync(() -> moderationService.moderate(post.getContenu()))
                .thenAccept(result -> Platform.runLater(() ->
                        handleModerationResult(post, result, isEdit)));
    }

    private void handleModerationResult(Post post, ModerationResult result, boolean isEdit) {
        String action = isEdit ? "edited" : "published";
        if (result.shouldHide) {
            aiHiddenPostIds.add(post.getIdPost());
            post.setStatut("hidden");
            postService.modifierPost(post);
            rebuildFeed();

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("🤖 Post Auto-Hidden by AI Moderation");
            alert.setHeaderText("Your post has been automatically hidden.");
            alert.setContentText(
                    "Our AI moderation system flagged your " + action + " post.\n\n" +
                            "Reason : " + result.reason + "\n" +
                            "Risk score : " + String.format("%.0f%%", result.score * 100) + "\n\n" +
                            "The post is now marked as Hidden and is only\n" +
                            "visible to administrators for review.");
            alert.getDialogPane().setStyle(
                    "-fx-background-color:#FFFFFF;" +
                            "-fx-font-family:'Segoe UI',Arial;" +
                            "-fx-font-size:13px;");
            alert.show();
        } else {
            showToast(String.format(
                    "✅ AI check passed (%.0f%% risk) — post is live",
                    result.score * 100));
        }
    }

    // ═══════════════════════════════════════════
    // Post Card
    // ═══════════════════════════════════════════

    private VBox buildPostCard(Post post, boolean isArchive) {
        VBox card = new VBox(0);
        String baseStyle =
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),16,0,0,2);";
        String hoverStyle =
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(79,70,229,0.13),22,0,0,5);";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        VBox inner = new VBox(11);
        inner.setPadding(new Insets(18, 20, 4, 20));
        card.getChildren().add(inner);

        inner.getChildren().add(buildCardHeader(post, isArchive));

        Label contentLabel = new Label(post.getContenu());
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-font-size:13px; -fx-text-fill:#374151;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-line-spacing:2;");
        inner.getChildren().add(contentLabel);

        if (post.getMedia() != null && !post.getMedia().isBlank()) {
            try {
                Node mediaNode = buildMediaNode(post.getMedia());
                if (mediaNode != null) inner.getChildren().add(mediaNode);
            } catch (Exception ex) {
                Label warn = new Label("⚠ Could not load: " +
                        new File(post.getMedia()).getName());
                warn.setStyle("-fx-font-size:11px; -fx-text-fill:#F59E0B;");
                inner.getChildren().add(warn);
            }
        }

        List<Reaction>    reactions   = reactionService.getReactionsByPost(post.getIdPost());
        List<Commentaire> comments    = commentService.getCommentairesByPost(post.getIdPost());
        int               reportCount = reportCounts.getOrDefault(post.getIdPost(), 0);

        HBox stats = new HBox(6);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(2, 0, 0, 0));
        stats.getChildren().addAll(
                statLabel("♥ " + reactions.size()),
                statLabel("·"),
                statLabel("💬 " + comments.size()));
        if (reportCount > 0)
            stats.getChildren().addAll(statLabel("·"), statLabel("⚑ " + reportCount));
        inner.getChildren().add(stats);

        Separator sep = new Separator();
        sep.setPadding(new Insets(3, 0, 0, 0));
        inner.getChildren().add(sep);

        VBox commentsSection = buildCommentsSection(post, comments);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);

        HBox actionsRow = buildActionsRow(
                post, reactions.size(), commentsSection, inner, contentLabel, isArchive);
        inner.getChildren().add(actionsRow);
        inner.getChildren().add(commentsSection);

        return card;
    }

    // ─────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────

    private HBox buildCardHeader(Post post, boolean isArchive) {
        HBox header = new HBox(11);
        header.setAlignment(Pos.CENTER_LEFT);

        // ── FIX: Use JavaFX Background API for gradient avatar (not CSS) ─────
        Label avatar = new Label(CURRENT_USER.substring(0, 1).toUpperCase());
        avatar.setMinSize(38, 38);
        avatar.setMaxSize(38, 38);
        avatar.setAlignment(Pos.CENTER);
        avatar.setBackground(AVATAR_BG_LARGE);
        avatar.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");
        // ─────────────────────────────────────────────────────────────────────

        VBox meta = new VBox(1);
        Label nameL = new Label(CURRENT_USER);
        nameL.setStyle(
                "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-text-fill:#111827; -fx-font-family:'Segoe UI',Arial;");

        HBox dateLine = new HBox(6);
        dateLine.setAlignment(Pos.CENTER_LEFT);
        String ds = post.getDateCreation() != null
                ? post.getDateCreation().format(DATE_FMT) : "";
        Label dateL = new Label(ds);
        dateL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");

        boolean isPrivate = post.getStatut() != null
                && post.getStatut().toLowerCase().contains("private");
        Label visBadge = new Label(isPrivate ? "🔒 Private" : "🌐 Public");
        visBadge.setStyle(
                "-fx-background-color:" + (isPrivate ? "#FEF3C7" : "#ECFDF5") + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-text-fill:" + (isPrivate ? "#92400E" : "#065F46") + ";" +
                        "-fx-font-size:9px; -fx-padding:2 7 2 7;");
        dateLine.getChildren().addAll(dateL, visBadge);
        meta.getChildren().addAll(nameL, dateLine);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, meta, spacer, buildMoreMenu(post, isArchive));
        return header;
    }

    // ─────────────────────────────────────────
    // More Menu
    // ─────────────────────────────────────────

    private MenuButton buildMoreMenu(Post post, boolean isArchive) {
        MenuButton btn = new MenuButton("• • •");
        btn.setStyle(
                "-fx-background-color:transparent; -fx-background-radius:8;" +
                        "-fx-text-fill:#9CA3AF; -fx-font-size:11px;" +
                        "-fx-padding:4 8 4 8; -fx-cursor:HAND; -fx-border-color:transparent;");

        boolean isSaved = savedPostIds.contains(post.getIdPost());
        MenuItem saveItem = new MenuItem(isSaved ? "🗂  Remove from Saved" : "🗂  Save Post");
        saveItem.setOnAction(e -> {
            if (savedPostIds.contains(post.getIdPost())) {
                savedPostIds.remove(post.getIdPost());
                showToast("Removed from saved posts");
            } else {
                savedPostIds.add(post.getIdPost());
                showToast("Post saved ✓");
            }
            rebuildFeed();
            if (archiveView.isVisible()) rebuildArchive();
        });

        MenuItem hideItem = new MenuItem("🙈  Hide this post");
        hideItem.setOnAction(e -> {
            hiddenPostIds.add(post.getIdPost());
            rebuildFeed();
        });

        boolean currentlyPrivate = post.getStatut() != null
                && post.getStatut().toLowerCase().contains("private");
        MenuItem visItem = new MenuItem(
                currentlyPrivate ? "🌐  Make Public" : "🔒  Make Private");
        visItem.setOnAction(e -> {
            post.setStatut(currentlyPrivate ? "public" : "private");
            postService.modifierPost(post);
            rebuildFeed();
        });

        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem reportItem = new MenuItem("⚑  Report post");
        reportItem.setOnAction(e -> handleReport(post));

        if (isArchive) {
            btn.getItems().addAll(saveItem, visItem);
        } else {
            btn.getItems().addAll(saveItem, hideItem, visItem, sep, reportItem);
        }
        return btn;
    }

    // ─────────────────────────────────────────
    // Media
    // ─────────────────────────────────────────

    private Node buildMediaNode(String mediaPath) {
        if (mediaPath == null || mediaPath.isBlank()) return null;
        String lower = mediaPath.toLowerCase();
        boolean isVideo = lower.endsWith(".mp4")  || lower.endsWith(".avi")
                || lower.endsWith(".mov")  || lower.endsWith(".mkv")
                || lower.endsWith(".webm") || lower.endsWith(".flv");
        return isVideo ? buildVideoPlayer(mediaPath) : buildImageNode(mediaPath);
    }

    private Node buildImageNode(String path) {
        try {
            File f = new File(path);
            String url = f.exists() ? f.toURI().toString() : path;
            Image img = new Image(url, 660, 0, true, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(660);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    private Node buildVideoPlayer(String path) {
        try {
            File f = new File(path);
            String url = f.exists() ? f.toURI().toString() : path;

            Media media = new Media(url);
            MediaPlayer player = new MediaPlayer(media);
            MediaView view = new MediaView(player);
            view.setFitWidth(660);
            view.setPreserveRatio(true);

            Button playPause = new Button("▶");
            playPause.setStyle(
                    "-fx-background-color:#4F46E5; -fx-background-radius:8;" +
                            "-fx-text-fill:white; -fx-font-size:13px;" +
                            "-fx-min-width:36; -fx-min-height:32; -fx-cursor:HAND;");

            Slider progress = new Slider(0, 1, 0);
            HBox.setHgrow(progress, Priority.ALWAYS);
            progress.setStyle("-fx-padding:0 4 0 4;");

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

            Button muteBtn = new Button("🔊");
            muteBtn.setStyle("-fx-background-color:transparent; -fx-font-size:13px; -fx-cursor:HAND;");

            HBox controls = new HBox(8, playPause, progress, timeLabel, muteBtn);
            controls.setAlignment(Pos.CENTER_LEFT);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#1F2937;");

            final boolean[] playing = {false};
            playPause.setOnAction(e -> {
                if (playing[0]) { player.pause(); playPause.setText("▶"); }
                else            { player.play();  playPause.setText("⏸"); }
                playing[0] = !playing[0];
            });

            final boolean[] muted = {false};
            muteBtn.setOnAction(e -> {
                muted[0] = !muted[0];
                player.setMute(muted[0]);
                muteBtn.setText(muted[0] ? "🔇" : "🔊");
            });

            progress.setOnMousePressed(e -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null)
                    player.seek(javafx.util.Duration.seconds(progress.getValue() * total.toSeconds()));
            });
            progress.setOnMouseDragged(e -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null)
                    player.seek(javafx.util.Duration.seconds(progress.getValue() * total.toSeconds()));
            });

            player.currentTimeProperty().addListener((obs, old, now) -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null && total.toSeconds() > 0) {
                    progress.setValue(now.toSeconds() / total.toSeconds());
                    int secs = (int) now.toSeconds();
                    timeLabel.setText(secs / 60 + ":" + String.format("%02d", secs % 60));
                }
            });

            player.setOnEndOfMedia(() -> {
                player.seek(javafx.util.Duration.ZERO);
                playing[0] = false;
                playPause.setText("▶");
            });

            VBox videoCard = new VBox(0, view, controls);
            videoCard.setStyle("-fx-background-color:#000000; -fx-background-radius:10;");
            Rectangle clip = new Rectangle();
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            clip.widthProperty().bind(videoCard.widthProperty());
            clip.heightProperty().bind(videoCard.heightProperty());
            videoCard.setClip(clip);
            return videoCard;

        } catch (Exception e) {
            Label err = new Label("⚠ Could not load video: " + new File(path).getName());
            err.setStyle("-fx-font-size:11px; -fx-text-fill:#F59E0B;");
            return err;
        }
    }

    // ─────────────────────────────────────────
    // Actions Row
    // ─────────────────────────────────────────

    private HBox buildActionsRow(Post post, int reactionCount,
                                 VBox commentsSection, VBox cardInner,
                                 Label contentLabel, boolean isArchive) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 10, 0));

        Button reactBtn = ghostButton("♥  " + reactionCount, "#EF4444");
        reactBtn.setOnAction(e -> handleReact(post, reactBtn));

        Button commentBtn = ghostButton("💬 Comment", "#4F46E5");
        commentBtn.setOnAction(e -> {
            boolean showing = commentsSection.isVisible();
            commentsSection.setVisible(!showing);
            commentsSection.setManaged(!showing);
            commentBtn.setText(showing ? "💬 Comment" : "💬 Hide");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn   = outlineButton("✏", "#6B7280");
        Button deleteBtn = outlineButton("🗑", "#EF4444");
        editBtn.setOnAction(e   -> handleEditPost(post, cardInner, contentLabel));
        deleteBtn.setOnAction(e -> handleDeletePost(post));

        row.getChildren().addAll(reactBtn, commentBtn, spacer, editBtn, deleteBtn);
        return row;
    }

    // ─────────────────────────────────────────
    // Comments Section
    // ─────────────────────────────────────────

    private VBox buildCommentsSection(Post post, List<Commentaire> comments) {
        VBox section = new VBox(0);
        section.setStyle(
                "-fx-background-color:#F9FAFB;" +
                        "-fx-background-radius:0 0 12 12;" +
                        "-fx-padding:12 20 14 20;");

        Label spamLabel = new Label();
        spamLabel.setStyle(
                "-fx-text-fill:#EF4444; -fx-font-size:11px;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-padding:0 0 6 0;");
        spamLabel.setVisible(false);
        spamLabel.setManaged(false);

        VBox commentsList = new VBox(4);
        for (Commentaire c : comments) {
            commentsList.getChildren().add(buildCommentItem(c, post, spamLabel));
        }

        HBox addForm = buildAddCommentForm(post, commentsList, null, spamLabel);
        section.getChildren().addAll(spamLabel, commentsList, addForm);
        return section;
    }

    private VBox buildCommentItem(Commentaire comment, Post post, Label spamLabel) {
        VBox wrapper = new VBox(0);
        wrapper.setPadding(new Insets(6, 0, 4, 0));

        HBox mainRow = new HBox(10);
        mainRow.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label("U");
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color:#E5E7EB; -fx-background-radius:16;" +
                        "-fx-text-fill:#6B7280; -fx-font-size:12px; -fx-font-weight:bold;");

        VBox rightCol = new VBox(4);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        VBox bubble = new VBox(3);
        bubble.setStyle(
                "-fx-background-color:#FFFFFF; -fx-background-radius:4 14 14 14;" +
                        "-fx-padding:8 12 8 12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),5,0,0,1);");
        Label nameL = new Label("User");
        nameL.setStyle(
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                        "-fx-text-fill:#111827; -fx-font-family:'Segoe UI',Arial;");
        Label textL = new Label(comment.getContenu());
        textL.setWrapText(true);
        textL.setStyle("-fx-font-size:13px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        bubble.getChildren().addAll(nameL, textL);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(2, 0, 0, 4));

        String d = comment.getDate() != null ? comment.getDate().format(DATE_FMT) : "";
        Label dateL = new Label(d);
        dateL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");

        List<Reaction> cReacts =
                reactionService.getReactionsByCommentaire(comment.getIdCommentaire());
        Button cReactBtn = linkButton("♥ " + cReacts.size(), "#EF4444");
        cReactBtn.setOnAction(e -> handleCommentReact(comment, cReactBtn));

        Button replyToggle = linkButton("↩ Reply", "#6B7280");
        actionRow.getChildren().addAll(dateL, cReactBtn, replyToggle);

        VBox replyThread = new VBox(6);
        replyThread.setPadding(new Insets(6, 0, 0, 42));
        replyThread.setVisible(false);
        replyThread.setManaged(false);

        List<Commentaire> allC = commentService.getCommentairesByPost(post.getIdPost());
        String replyPrefix = "↩ [reply@" + comment.getIdCommentaire() + "] ";
        for (Commentaire c : allC) {
            if (c.getContenu().startsWith(replyPrefix)) {
                replyThread.getChildren().add(
                        buildReplyBubble(c.getContenu().replace(replyPrefix, "")));
            }
        }

        HBox replyInput = buildAddCommentForm(post, replyThread, comment, spamLabel);
        replyToggle.setOnAction(e -> {
            boolean showing = replyThread.isVisible();
            replyThread.setVisible(!showing);
            replyThread.setManaged(!showing);
            if (!showing && !replyThread.getChildren().contains(replyInput))
                replyThread.getChildren().add(replyInput);
            replyToggle.setText(showing ? "↩ Reply" : "↩ Hide");
        });
        replyThread.getChildren().add(replyInput);

        rightCol.getChildren().addAll(bubble, actionRow, replyThread);
        mainRow.getChildren().addAll(avatar, rightCol);
        wrapper.getChildren().add(mainRow);
        return wrapper;
    }

    private HBox buildReplyBubble(String replyText) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label("U");
        avatar.setMinSize(26, 26);
        avatar.setMaxSize(26, 26);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color:#D1D5DB; -fx-background-radius:13;" +
                        "-fx-text-fill:#6B7280; -fx-font-size:10px;");

        VBox col = new VBox(2);
        Label nameL = new Label("User");
        nameL.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#374151;");
        Label textL = new Label(replyText);
        textL.setWrapText(true);
        textL.setStyle(
                "-fx-background-color:#EEF2FF; -fx-background-radius:4 14 14 14;" +
                        "-fx-text-fill:#3730A3; -fx-font-size:12px; -fx-padding:7 11 7 11;");
        col.getChildren().addAll(nameL, textL);
        row.getChildren().addAll(avatar, col);
        return row;
    }

    private HBox buildAddCommentForm(Post post, VBox targetList,
                                     Commentaire parentComment, Label spamLabel) {
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(parentComment == null ? 10 : 6, 0, 0, 0));

        // ── FIX: Use JavaFX Background API for gradient avatar (not CSS) ─────
        Label selfAvatar = new Label(CURRENT_USER.substring(0, 1).toUpperCase());
        selfAvatar.setMinSize(28, 28);
        selfAvatar.setMaxSize(28, 28);
        selfAvatar.setAlignment(Pos.CENTER);
        selfAvatar.setBackground(AVATAR_BG_SMALL);
        selfAvatar.setStyle("-fx-text-fill:white; -fx-font-size:11px;");
        // ─────────────────────────────────────────────────────────────────────

        TextField field = new TextField();
        field.setPromptText(parentComment == null ? "Write a comment…" : "Write a reply…");
        field.setStyle(
                "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                        "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:#E5E7EB; -fx-border-radius:20; -fx-border-width:1.5;" +
                        "-fx-padding:7 12 7 12;");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button sendBtn = new Button("↵");
        sendBtn.setStyle(
                "-fx-background-color:#4F46E5; -fx-background-radius:16;" +
                        "-fx-text-fill:white; -fx-font-size:13px;" +
                        "-fx-min-width:32; -fx-min-height:32; -fx-cursor:HAND;");

        Runnable submit = () -> {
            String txt = field.getText().trim();
            if (txt.isEmpty()) return;
            if (!canPostComment(spamLabel)) return;

            commentTimestamps.add(LocalDateTime.now());
            hideError(spamLabel);

            if (parentComment == null) {
                Commentaire c = new Commentaire(txt, post.getIdPost());
                commentService.ajouterCommentaire(c);
                VBox item = buildCommentItem(c, post, spamLabel);
                item.setOpacity(0);
                int idx = Math.max(0, targetList.getChildren().size() - 1);
                targetList.getChildren().add(idx, item);
                fadeIn(item);
            } else {
                String replyText = "↩ [reply@" + parentComment.getIdCommentaire() + "] " + txt;
                Commentaire reply = new Commentaire(replyText, post.getIdPost());
                commentService.ajouterCommentaire(reply);
                HBox bubble = buildReplyBubble(txt);
                bubble.setOpacity(0);
                int idx = Math.max(0, targetList.getChildren().size() - 1);
                targetList.getChildren().add(idx, bubble);
                fadeIn(bubble);
            }
            field.clear();
        };

        sendBtn.setOnAction(e -> submit.run());
        field.setOnAction(e  -> submit.run());
        form.getChildren().addAll(selfAvatar, field, sendBtn);
        return form;
    }

    // ═══════════════════════════════════════════
    // Spam Protection
    // ═══════════════════════════════════════════

    private boolean canPostComment(Label spamLabel) {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(COMMENT_WINDOW_MINUTES);
        commentTimestamps.removeIf(t -> t.isBefore(cutoff));

        if (commentTimestamps.size() >= MAX_COMMENTS_IN_WINDOW) {
            LocalDateTime oldest    = commentTimestamps.get(0);
            LocalDateTime unblockAt = oldest.plusMinutes(COMMENT_WINDOW_MINUTES);
            long secondsLeft = java.time.Duration.between(now, unblockAt).toSeconds();
            long minsLeft    = secondsLeft / 60;
            long secsLeft    = secondsLeft % 60;
            String timeStr   = minsLeft > 0
                    ? minsLeft + " min " + secsLeft + " sec"
                    : secsLeft + " sec";
            showError(spamLabel,
                    "⚠ Too many comments! Wait " + timeStr + " before commenting again.");
            shakeNode(spamLabel);
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════
    // FXML Handlers
    // ═══════════════════════════════════════════

    @FXML
    private void handlePublishPost() {
        String content = newPostContent.getText().trim();
        String media   = newPostMediaPath.getText().trim();
        if (content.isEmpty()) {
            showError(postErrorLabel, "⚠  Content cannot be empty.");
            shakeNode(newPostContent);
            return;
        }
        hideError(postErrorLabel);

        publishBtn.setDisable(true);
        publishBtn.setText("Publishing…");

        String selected = visibilityCombo.getValue();
        String statut = (selected != null && selected.contains("Private"))
                ? "private" : "public";

        Post newPost = new Post(content, media.isEmpty() ? null : media, statut);
        postService.ajouterPost(newPost);
        postsList.add(0, newPost);
        handleClearPost();
        rebuildFeed();
        mainScrollPane.setVvalue(0);

        publishBtn.setDisable(false);
        publishBtn.setText("Publish");

        runModerationAsync(newPost, false);
    }

    @FXML
    private void handleClearPost() {
        newPostContent.clear();
        newPostMediaPath.clear();
        visibilityCombo.getSelectionModel().selectFirst();
        hideError(postErrorLabel);
    }

    @FXML
    private void handleBrowseMedia() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Photo or Video");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Media files",
                "*.png","*.jpg","*.jpeg","*.gif","*.bmp","*.webp",
                "*.mp4","*.avi","*.mov","*.mkv","*.webm","*.flv"));
        File file = chooser.showOpenDialog(newPostContent.getScene().getWindow());
        if (file != null) newPostMediaPath.setText(file.getAbsolutePath());
    }

    @FXML private void handleRefresh() { loadPosts(); }

    @FXML
    private void handleShowArchive() {
        rebuildArchive();
        archiveView.setVisible(true);
        archiveView.setManaged(true);
        mainScrollPane.setVisible(false);
        mainScrollPane.setManaged(false);
    }

    @FXML
    private void handleShowFeed() {
        archiveView.setVisible(false);
        archiveView.setManaged(false);
        mainScrollPane.setVisible(true);
        mainScrollPane.setManaged(true);
    }

    // ═══════════════════════════════════════════
    // Card Handlers
    // ═══════════════════════════════════════════

    private void handleReact(Post post, Button btn) {
        reactionService.ajouterReaction(new Reaction("LIKE", post.getIdPost()));
        int count = reactionService.getReactionsByPost(post.getIdPost()).size();
        btn.setText("♥  " + count);
        btn.setStyle(
                "-fx-background-color:#EF4444; -fx-background-radius:8;" +
                        "-fx-text-fill:white; -fx-font-size:12px;" +
                        "-fx-padding:6 12 6 12; -fx-cursor:HAND;");
    }

    private void handleCommentReact(Commentaire comment, Button btn) {
        reactionService.ajouterReaction(
                new Reaction("LIKE", null, comment.getIdCommentaire()));
        int count = reactionService.getReactionsByCommentaire(
                comment.getIdCommentaire()).size();
        btn.setText("♥ " + count);
    }

    private void handleReport(Post post) {
        int count = reportCounts.getOrDefault(post.getIdPost(), 0) + 1;
        reportCounts.put(post.getIdPost(), count);
        if (count > MAX_REPORTS) {
            postService.supprimerPost(post.getIdPost());
            postsList.removeIf(p -> p.getIdPost() == post.getIdPost());
            savedPostIds.remove(post.getIdPost());
            rebuildFeed();
            showToast("Post removed after " + MAX_REPORTS + "+ reports.");
        } else {
            showToast("Reported (" + count + "/" + MAX_REPORTS + ")");
            rebuildFeed();
        }
    }

    private void handleEditPost(Post post, VBox cardInner, Label contentLabel) {
        int idx = cardInner.getChildren().indexOf(contentLabel);
        if (idx < 0) return;

        TextArea area = new TextArea(post.getContenu());
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.setStyle(
                "-fx-font-size:13px; -fx-background-color:#F9FAFB;" +
                        "-fx-background-radius:8; -fx-border-color:#6366F1;" +
                        "-fx-border-radius:8; -fx-border-width:1.5; -fx-padding:9;");

        Button saveBtn = new Button("✓  Save");
        saveBtn.setStyle(
                "-fx-background-color:#22C55E; -fx-background-radius:8;" +
                        "-fx-text-fill:white; -fx-font-size:12px;" +
                        "-fx-padding:6 14 6 14; -fx-cursor:HAND;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#9CA3AF;" +
                        "-fx-font-size:12px; -fx-padding:6 12 6 12; -fx-cursor:HAND;");

        HBox editActions = new HBox(8, cancelBtn, saveBtn);
        editActions.setAlignment(Pos.CENTER_RIGHT);
        VBox editForm = new VBox(8, area, editActions);
        cardInner.getChildren().set(idx, editForm);

        saveBtn.setOnAction(e -> {
            String updated = area.getText().trim();
            if (!updated.isEmpty()) {
                post.setContenu(updated);
                postService.modifierPost(post);
                contentLabel.setText(updated);
                aiHiddenPostIds.remove(post.getIdPost());
                runModerationAsync(post, true);
            }
            cardInner.getChildren().set(idx, contentLabel);
        });

        cancelBtn.setOnAction(e -> cardInner.getChildren().set(idx, contentLabel));
    }

    private void handleDeletePost(Post post) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "This action cannot be undone.", ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Delete this post?");
        confirm.getDialogPane().setStyle(
                "-fx-background-color:#FFFFFF; -fx-font-family:'Segoe UI',Arial;");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                postService.supprimerPost(post.getIdPost());
                postsList.removeIf(p -> p.getIdPost() == post.getIdPost());
                savedPostIds.remove(post.getIdPost());
                aiHiddenPostIds.remove(post.getIdPost());
                rebuildFeed();
            }
        });
    }

    // ═══════════════════════════════════════════
    // Toast
    // ═══════════════════════════════════════════

    private void showToast(String message) {
        try {
            Label toast = new Label(message);
            toast.setStyle(
                    "-fx-background-color:rgba(17,24,39,0.85);" +
                            "-fx-background-radius:20; -fx-text-fill:white;" +
                            "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                            "-fx-padding:8 20 8 20;");
            toast.setMouseTransparent(true);
            Pane root = (Pane) postsFeedContainer.getScene().getRoot();
            toast.setLayoutX(root.getWidth() / 2 - 120);
            toast.setLayoutY(root.getHeight() - 80);
            root.getChildren().add(toast);
            FadeTransition ft = new FadeTransition(Duration.millis(1600), toast);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setDelay(Duration.millis(1200));
            ft.setOnFinished(e -> root.getChildren().remove(toast));
            ft.play();
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════
    // UI Helpers
    // ═══════════════════════════════════════════

    private Button ghostButton(String text, String color) {
        Button b = new Button(text);
        String base =
                "-fx-background-color:transparent; -fx-background-radius:8;" +
                        "-fx-text-fill:" + color + "; -fx-font-size:12px;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-padding:6 12 6 12; -fx-cursor:HAND;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(
                base.replace("-fx-background-color:transparent;",
                        "-fx-background-color:#F3F4F8;")));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button outlineButton(String text, String color) {
        Button b = new Button(text);
        String base =
                "-fx-background-color:transparent; -fx-background-radius:8;" +
                        "-fx-border-color:#E5E7EB; -fx-border-width:1; -fx-border-radius:8;" +
                        "-fx-text-fill:" + color + "; -fx-font-size:12px;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-padding:6 12 6 12; -fx-cursor:HAND;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(
                base.replace("-fx-background-color:transparent;",
                        "-fx-background-color:#F9FAFB;")));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private Button linkButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:" + color + ";" +
                        "-fx-font-size:11px; -fx-font-weight:bold;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-padding:2 6 2 0;" +
                        "-fx-cursor:HAND; -fx-border-color:transparent;");
        return b;
    }

    private Label statLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");
        return l;
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), label);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideError(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setByX(7);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    private void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}