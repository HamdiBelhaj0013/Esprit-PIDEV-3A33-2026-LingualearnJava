package org.example.controller.admin;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.ai.AdminAiService;
import org.example.service.ai.AiActionExecutor;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for AdminAiView.fxml — AI Admin Assistant.
 *
 * Tabs (custom HBox-based, not JavaFX TabPane):
 *   1. User Search   — natural-language search with XP-sorted results  (BUG 1 fixed)
 *   2. User Insight  — click-to-select user list + AI insight panel    (Improvement 1)
 *   3. AI Chat       — conversational assistant with collapsible guide  (Improvement 2)
 *   4. Analytics     — platform trends with live stat cards             (BUG 3 fixed)
 *
 * All Ollama calls run on background Task threads.
 * All UI updates run on the JavaFX Application Thread.
 */
public class AdminAiController {

    // ── Custom tab bar ────────────────────────────────────────────────────────
    @FXML private Button tabBtnSearch;
    @FXML private Button tabBtnInsight;
    @FXML private Button tabBtnChat;
    @FXML private Button tabBtnAnalytics;
    @FXML private StackPane contentStack;

    // ── Content panes (declared as Region so any Pane subtype works) ─────────
    @FXML private Region searchPane;
    @FXML private Region insightPane;
    @FXML private Region chatPane;
    @FXML private Region analyticsPane;

    // ── Tab 1 — User Search ───────────────────────────────────────────────────
    @FXML private TextField                       searchField;
    @FXML private Button                          searchButton;
    @FXML private TableView<Map<String, Object>>  searchTable;
    @FXML private Label                           searchReasonLabel;
    @FXML private Label                           searchCountLabel;
    @FXML private ProgressIndicator              searchProgress;

    // ── Tab 2 — User Insight ──────────────────────────────────────────────────
    @FXML private TextField          insightSearchField;
    @FXML private TableView<UserRow> insightUserTable;
    @FXML private StackPane          insightRightPane;
    @FXML private VBox               insightPlaceholder;
    @FXML private VBox               insightLoadingPane;
    @FXML private ProgressIndicator  insightProgress;
    @FXML private VBox               insightCardPane;
    @FXML private Label              insightUserNameLabel;
    @FXML private Label              insightUserSubtitleLabel;
    @FXML private TextArea           insightTextArea;
    @FXML private Button             regenerateButton;

    // ── Tab 3 — AI Chat ───────────────────────────────────────────────────────
    @FXML private VBox       chatGuidePanel;
    @FXML private VBox       guideContent;
    @FXML private Button     guideToggleButton;
    @FXML private VBox       chatVBox;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField  chatInputField;
    @FXML private Button     sendButton;
    @FXML private Label      typingDotLabel;

    // ── Tab 4 — Analytics ─────────────────────────────────────────────────────
    @FXML private Label             statTotalUsers;
    @FXML private Label             statPremiumUsers;
    @FXML private Label             statAvgXP;
    @FXML private Button            analyzeButton;
    @FXML private TextArea          analyticsTextArea;
    @FXML private ProgressIndicator analyticsProgress;

    // ── Internal state ────────────────────────────────────────────────────────
    private AdminAiService                         aiService;
    private final List<AdminAiService.ChatMessage> chatHistory  = new ArrayList<>();
    private boolean guideVisible        = true;
    private boolean insightUsersLoaded  = false;
    private long    currentInsightUserId  = -1;
    private String  currentInsightName   = "";
    private String  currentInsightEmail  = "";
    private String  currentInsightPlan   = "";

    // XP column reference (needed for sort indicator — BUG 1 fix)
    private TableColumn<Map<String, Object>, Number> xpSearchCol;

    // Insight user list
    private ObservableList<UserRow> insightMasterList;
    private FilteredList<UserRow>   insightFilteredList;

    // Typing animation
    private Timeline typingTimeline;
    private int      typingDotState = 0;
    private static final String[] TYPING_DOTS = {"●○○", "○●○", "○○●"};

    // ── Colours (bubble palette) ──────────────────────────────────────────────
    private static final String C_BG_DEEP  = "#0d1117";
    private static final String C_BG_CARD  = "#161b22";
    private static final String C_BG_INPUT = "#1f3a5f";
    private static final String C_ACCENT   = "#e94560";
    private static final String C_TEXT     = "#c9d1d9";
    private static final String C_SUBTEXT  = "#8b949e";
    private static final String C_GREEN    = "#3fb950";
    private static final String C_RED      = "#f85149";
    private static final String C_BORDER   = "#30363d";

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        aiService = new AdminAiService(new UserRepository());

        // Tab list for bulk visibility toggling
        List<Node> panes   = List.of(searchPane, insightPane, chatPane, analyticsPane);
        List<Button> btns  = List.of(tabBtnSearch, tabBtnInsight, tabBtnChat, tabBtnAnalytics);

        // Wire tab buttons
        tabBtnSearch   .setOnAction(e -> switchTab(searchPane,    tabBtnSearch,    panes, btns));
        tabBtnInsight  .setOnAction(e -> switchTab(insightPane,   tabBtnInsight,   panes, btns));
        tabBtnChat     .setOnAction(e -> switchTab(chatPane,      tabBtnChat,      panes, btns));
        tabBtnAnalytics.setOnAction(e -> switchTab(analyticsPane, tabBtnAnalytics, panes, btns));

        // Tab 1 setup
        buildSearchTableColumns();
        searchField.setOnAction(e -> handleSearch());

        // Tab 2 setup
        buildInsightTableColumns();

        // Tab 3 setup
        chatInputField.setOnAction(e -> handleSend());
        initTypingAnimation();

        // Tab 4: load quick stats immediately (background)
        loadQuickStats();
    }

    // ── Custom tab switching ──────────────────────────────────────────────────

    private void switchTab(Node target, Button btn,
                           List<Node> allPanes, List<Button> allBtns) {
        for (Node p : allPanes) {
            p.setVisible(p == target);
            p.setManaged(p == target);
        }
        for (Button b : allBtns) {
            b.getStyleClass().remove("ai-tab-active");
        }
        btn.getStyleClass().add("ai-tab-active");

        // Lazy-load insight users on first visit
        if (target == insightPane && !insightUsersLoaded) {
            insightUsersLoaded = true;
            loadInsightUsers();
        }
    }

    @FXML private void switchToSearch()    { tabBtnSearch.fire(); }
    @FXML private void switchToInsight()   { tabBtnInsight.fire(); }
    @FXML private void switchToChat()      { tabBtnChat.fire(); }
    @FXML private void switchToAnalytics() { tabBtnAnalytics.fire(); }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 1 — USER SEARCH
    // ═════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private void buildSearchTableColumns() {
        TableColumn<Map<String, Object>, Long> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(55);
        idCol.setCellValueFactory(d ->
            new SimpleObjectProperty<>((Long) d.getValue().get("id")));

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(d ->
            new SimpleStringProperty(str(d.getValue(), "name")));

        TableColumn<Map<String, Object>, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(200);
        emailCol.setCellValueFactory(d ->
            new SimpleStringProperty(str(d.getValue(), "email")));

        // Status with colored badges
        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(95);
        statusCol.setCellValueFactory(d ->
            new SimpleStringProperty(str(d.getValue(), "status")));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = switch (v) {
                    case "active"    -> C_GREEN;
                    case "suspended" -> "#d29922";
                    case "deleted"   -> C_RED;
                    default          -> C_SUBTEXT;
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        TableColumn<Map<String, Object>, String> planCol = new TableColumn<>("Plan");
        planCol.setPrefWidth(85);
        planCol.setCellValueFactory(d ->
            new SimpleStringProperty(str(d.getValue(), "plan").toUpperCase()));

        // Premium checkmark
        TableColumn<Map<String, Object>, String> premiumCol = new TableColumn<>("Premium");
        premiumCol.setPrefWidth(70);
        premiumCol.setCellValueFactory(d -> {
            boolean p = Boolean.TRUE.equals(d.getValue().get("isPremium"));
            return new SimpleStringProperty(p ? "✓" : "—");
        });
        premiumCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setText(null); return; }
                setText(v);
                setStyle("✓".equals(v)
                    ? "-fx-text-fill: " + C_GREEN + "; -fx-font-weight: bold; -fx-alignment: CENTER;"
                    : "-fx-text-fill: " + C_SUBTEXT + "; -fx-alignment: CENTER;");
            }
        });

        // XP column — kept as field for sort indicator (BUG 1)
        xpSearchCol = new TableColumn<>("XP");
        xpSearchCol.setPrefWidth(70);
        xpSearchCol.setCellValueFactory(d ->
            new SimpleObjectProperty<>(toInt(d.getValue().get("xp"))));
        xpSearchCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.valueOf(v.intValue()));
                setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
            }
        });

        searchTable.getColumns().addAll(
            idCol, nameCol, emailCol, statusCol, planCol, premiumCol, xpSearchCol);
        searchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        setSearchBusy(true);
        searchReasonLabel.setText("Searching…");
        searchCountLabel.setText("");
        searchTable.getItems().clear();

        Task<AdminAiService.SearchResult> task = new Task<>() {
            @Override protected AdminAiService.SearchResult call() {
                return aiService.naturalLanguageSearch(query);
            }
        };

        task.setOnSucceeded(e -> {
            AdminAiService.SearchResult r = task.getValue();

            // BUG 1 FIX: sort by XP descending before displaying
            ObservableList<Map<String, Object>> sorted =
                FXCollections.observableArrayList(r.users);
            sorted.sort((a, b) -> Integer.compare(toInt(b.get("xp")), toInt(a.get("xp"))));

            searchTable.setItems(sorted);

            // Set XP column as the visible sort indicator
            xpSearchCol.setSortType(TableColumn.SortType.DESCENDING);
            searchTable.getSortOrder().setAll(xpSearchCol);

            searchCountLabel.setText(sorted.size() + " user(s) found");
            searchReasonLabel.setText(r.reasoning + "\n\n" + r.summary);
            setSearchBusy(false);
        });

        task.setOnFailed(e -> {
            searchReasonLabel.setText(friendlyError(task.getException()));
            setSearchBusy(false);
        });

        new Thread(task, "ai-search").start();
    }

    private void setSearchBusy(boolean busy) {
        searchButton.setDisable(busy);
        searchProgress.setVisible(busy);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 2 — USER INSIGHT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Simple row model for the insight user table.
     */
    public static class UserRow {
        public final long   id;
        public final String name;
        public final String email;
        public final String plan;
        public final int    xp;

        public UserRow(long id, String name, String email, String plan, int xp) {
            this.id    = id;
            this.name  = name;
            this.email = email;
            this.plan  = plan;
            this.xp    = xp;
        }
    }

    private void buildInsightTableColumns() {
        // Avatar + Name column
        TableColumn<UserRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        nameCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); setText(null); return; }

                // Avatar circle with consistent color from name hash
                char firstChar = name.isEmpty() ? '?' : Character.toUpperCase(name.charAt(0));
                double hue     = Math.abs(name.hashCode()) % 360;
                Color  bg      = Color.hsb(hue, 0.55, 0.65);

                Circle circle = new Circle(14);
                circle.setFill(bg);

                Text letter = new Text(String.valueOf(firstChar));
                letter.setFill(Color.WHITE);
                letter.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                StackPane avatar = new StackPane(circle, letter);
                avatar.setPrefWidth(28);
                avatar.setPrefHeight(28);

                Label lbl = new Label(name);
                lbl.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-padding: 0 0 0 8;");

                HBox cell = new HBox(avatar, lbl);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
            }
        });

        TableColumn<UserRow, String> emailCol = new TableColumn<>("Email");
        emailCol.setPrefWidth(190);
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email));
        emailCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(v);
                setStyle("-fx-text-fill: " + C_SUBTEXT + "; -fx-font-size: 12px;");
            }
        });

        TableColumn<UserRow, String> planCol = new TableColumn<>("Plan");
        planCol.setPrefWidth(75);
        planCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().plan.toUpperCase()));

        TableColumn<UserRow, Number> xpCol = new TableColumn<>("XP");
        xpCol.setPrefWidth(60);
        xpCol.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().xp));
        xpCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); return; }
                setText(String.valueOf(v.intValue()));
                setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
            }
        });

        insightUserTable.getColumns().addAll(nameCol, emailCol, planCol, xpCol);
        insightUserTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Single-click triggers insight generation
        insightUserTable.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                UserRow sel = insightUserTable.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    generateInsightForUser(sel.id, sel.name, sel.email, sel.plan);
                }
            }
        });
    }

    /** Loads all users lazily when the Insight tab is first opened. */
    private void loadInsightUsers() {
        Task<List<UserRow>> task = new Task<>() {
            @Override protected List<UserRow> call() {
                return aiService.getAllUsers().stream()
                    .map(u -> new UserRow(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getSubscriptionPlan() != null ? u.getSubscriptionPlan() : "FREE",
                        u.getLearningStats() != null ? u.getLearningStats().getTotalXP() : 0))
                    .sorted(Comparator.comparing(r -> r.name))
                    .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> {
            insightMasterList   = FXCollections.observableArrayList(task.getValue());
            insightFilteredList = new FilteredList<>(insightMasterList, p -> true);

            // Wire search field to filter
            insightSearchField.textProperty().addListener((obs, old, val) ->
                insightFilteredList.setPredicate(row ->
                    val == null || val.isBlank() ||
                    row.name.toLowerCase().contains(val.toLowerCase()) ||
                    row.email.toLowerCase().contains(val.toLowerCase())));

            SortedList<UserRow> sorted = new SortedList<>(insightFilteredList);
            sorted.comparatorProperty().bind(insightUserTable.comparatorProperty());
            insightUserTable.setItems(sorted);
        });

        task.setOnFailed(e ->
            System.err.println("[InsightUsers] load failed: " + task.getException().getMessage()));

        new Thread(task, "load-insight-users").start();
    }

    private void generateInsightForUser(long userId, String name, String email, String plan) {
        currentInsightUserId = userId;
        currentInsightName   = name;
        currentInsightEmail  = email;
        currentInsightPlan   = plan;

        showInsightState("loading");

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return aiService.generateUserInsight(userId);
            }
        };

        task.setOnSucceeded(e -> {
            insightUserNameLabel.setText(name);
            insightUserSubtitleLabel.setText(email + "  ·  " + plan.toUpperCase() + " plan");
            insightTextArea.setText(task.getValue());
            showInsightState("card");
        });

        task.setOnFailed(e -> {
            insightUserNameLabel.setText(name);
            insightUserSubtitleLabel.setText(email);
            insightTextArea.setText(friendlyError(task.getException()));
            showInsightState("card");
        });

        new Thread(task, "ai-insight").start();
    }

    @FXML
    private void handleRegenerateInsight() {
        if (currentInsightUserId < 0) return;
        generateInsightForUser(currentInsightUserId,
            currentInsightName, currentInsightEmail, currentInsightPlan);
    }

    /**
     * Controls which state the right-side insight panel shows.
     * @param state "placeholder" | "loading" | "card"
     */
    private void showInsightState(String state) {
        insightPlaceholder.setVisible("placeholder".equals(state));
        insightPlaceholder.setManaged("placeholder".equals(state));
        insightLoadingPane.setVisible("loading".equals(state));
        insightLoadingPane.setManaged("loading".equals(state));
        insightCardPane.setVisible("card".equals(state));
        insightCardPane.setManaged("card".equals(state));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 3 — AI CHAT
    // ═════════════════════════════════════════════════════════════════════════

    private void initTypingAnimation() {
        typingTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            typingDotState = (typingDotState + 1) % 3;
            typingDotLabel.setText("AI is thinking  " + TYPING_DOTS[typingDotState]);
        }));
        typingTimeline.setCycleCount(Animation.INDEFINITE);
    }

    /** Toggles the collapsible guide panel. */
    @FXML
    private void toggleGuide() {
        guideVisible = !guideVisible;
        guideContent.setVisible(guideVisible);
        guideContent.setManaged(guideVisible);
        guideToggleButton.setText(guideVisible ? "Hide guide" : "Show guide");
    }

    @FXML
    private void handleSend() {
        String msg = chatInputField.getText().trim();
        if (msg.isEmpty()) return;

        chatInputField.clear();
        addUserBubble(msg);
        chatHistory.add(new AdminAiService.ChatMessage("user", msg));

        setChatBusy(true);
        scrollToBottom();

        // History snapshot excludes the just-added message (service appends it)
        final List<AdminAiService.ChatMessage> snap =
            new ArrayList<>(chatHistory.subList(0, chatHistory.size() - 1));

        Task<AdminAiService.ChatResult> task = new Task<>() {
            @Override protected AdminAiService.ChatResult call() {
                return aiService.chat(snap, msg);
            }
        };

        task.setOnSucceeded(e -> {
            AdminAiService.ChatResult result = task.getValue();
            chatHistory.add(new AdminAiService.ChatMessage("assistant", result.reply));
            setChatBusy(false);

            boolean isMutation = result.actionParams != null
                && !"READ_ONLY".equals(result.action)
                && !"EXPORT_USER_IDS".equals(result.action);

            if (isMutation) {
                addAiBubble(result.reply);
                scrollToBottom();
                Alert confirm = buildConfirmDialog(result.action, result.reply);
                confirm.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.OK) {
                        executeActionInBackground(result.action, result.actionParams);
                    } else {
                        addStatusLabel("Action cancelled by admin.", false);
                        scrollToBottom();
                    }
                });
            } else {
                addAiBubble(result.reply);
                if ("EXPORT_USER_IDS".equals(result.action) && result.actionParams != null) {
                    AiActionExecutor.ActionResult ar =
                        aiService.executeAction(result.action, result.actionParams);
                    addStatusLabel(ar.message, ar.success);
                }
                scrollToBottom();
            }
        });

        task.setOnFailed(e -> {
            addAiBubble(friendlyError(task.getException()));
            setChatBusy(false);
            scrollToBottom();
        });

        new Thread(task, "ai-chat").start();
    }

    private void executeActionInBackground(String action, JSONObject params) {
        Task<AiActionExecutor.ActionResult> t = new Task<>() {
            @Override protected AiActionExecutor.ActionResult call() {
                return aiService.executeAction(action, params);
            }
        };
        t.setOnSucceeded(e -> {
            addStatusLabel(t.getValue().message, t.getValue().success);
            scrollToBottom();
        });
        t.setOnFailed(e -> {
            addStatusLabel("Execution error: " + t.getException().getMessage(), false);
            scrollToBottom();
        });
        new Thread(t, "ai-exec").start();
    }

    // ── Chat bubble helpers ───────────────────────────────────────────────────

    private void addUserBubble(String text) {
        Label bubble = makeBubble(text,
            "-fx-background-color: " + C_BG_INPUT + ";"
          + "-fx-text-fill: " + C_TEXT + ";"
          + "-fx-background-radius: 12 12 2 12;"
          + "-fx-border-radius: 12 12 2 12;"
          + "-fx-padding: 10 14 10 14;"
          + "-fx-font-size: 13px;");

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 10, 3, 70));
        row.setAlignment(Pos.CENTER_RIGHT);
        chatVBox.getChildren().add(row);
    }

    private void addAiBubble(String text) {
        Label bubble = makeBubble(text,
            "-fx-background-color: " + C_BG_CARD + ";"
          + "-fx-text-fill: " + C_TEXT + ";"
          + "-fx-border-color: " + C_BORDER + ";"
          + "-fx-border-width: 1;"
          + "-fx-border-radius: 12 12 12 2;"
          + "-fx-background-radius: 12 12 12 2;"
          + "-fx-padding: 10 14 10 14;"
          + "-fx-font-size: 13px;");

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(3, 70, 3, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        chatVBox.getChildren().add(row);
    }

    private void addStatusLabel(String text, boolean success) {
        Label lbl = new Label((success ? "✓ " : "✗ ") + text);
        lbl.setWrapText(true);
        lbl.setStyle(
            "-fx-font-size: 11px;"
          + "-fx-font-style: italic;"
          + "-fx-text-fill: " + (success ? C_GREEN : C_RED) + ";"
          + "-fx-padding: 0 10 4 14;");
        chatVBox.getChildren().add(lbl);
    }

    private Label makeBubble(String text, String style) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(560);
        lbl.setStyle(style);
        return lbl;
    }

    private Alert buildConfirmDialog(String action, String reply) {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
        dlg.setTitle("Confirm AI Action");
        dlg.setHeaderText("The AI wants to perform: " + action);
        dlg.setContentText(reply + "\n\nDo you want to proceed?");
        DialogPane dp = dlg.getDialogPane();
        dp.setStyle(
            "-fx-background-color: " + C_BG_CARD + ";"
          + "-fx-border-color: " + C_BORDER + ";"
          + "-fx-border-width: 1;");
        dp.lookupAll(".label").forEach(n ->
            n.setStyle("-fx-text-fill: " + C_TEXT + ";"));
        return dlg;
    }

    private void setChatBusy(boolean busy) {
        sendButton.setDisable(busy);
        chatInputField.setDisable(busy);
        typingDotLabel.setVisible(busy);
        typingDotLabel.setManaged(busy);
        if (busy) { typingTimeline.play(); }
        else      { typingTimeline.stop(); typingDotLabel.setText("AI is thinking  ●○○"); }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TAB 4 — ANALYTICS
    // ═════════════════════════════════════════════════════════════════════════

    /** Loads quick stats in background and updates the 3 stat cards immediately. */
    private void loadQuickStats() {
        Task<AdminAiService.QuickStats> task = new Task<>() {
            @Override protected AdminAiService.QuickStats call() {
                return aiService.getQuickStats();
            }
        };
        task.setOnSucceeded(e -> {
            AdminAiService.QuickStats qs = task.getValue();
            statTotalUsers  .setText(String.valueOf(qs.totalUsers));
            statPremiumUsers.setText(String.valueOf(qs.premiumCount));
            statAvgXP       .setText(String.valueOf(Math.round(qs.avgXP)));
        });
        task.setOnFailed(e ->
            System.err.println("[QuickStats] " + task.getException().getMessage()));
        new Thread(task, "quick-stats").start();
    }

    @FXML
    private void handleAnalyze() {
        setAnalyticsBusy(true);
        analyticsTextArea.setText("Analyzing platform data — this may take 30–60 seconds…");

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return aiService.analyzePlatformTrends();
            }
        };

        task.setOnSucceeded(e -> {
            analyticsTextArea.setText(task.getValue());
            setAnalyticsBusy(false);
            // Refresh stat cards too
            loadQuickStats();
        });

        task.setOnFailed(e -> {
            analyticsTextArea.setText(friendlyError(task.getException()));
            setAnalyticsBusy(false);
        });

        new Thread(task, "ai-analytics").start();
    }

    private void setAnalyticsBusy(boolean busy) {
        analyzeButton.setDisable(busy);
        analyticsProgress.setVisible(busy);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═════════════════════════════════════════════════════════════════════════

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString() : "";
    }

    private int toInt(Object v) {
        return (v instanceof Number n) ? n.intValue() : 0;
    }

    private String friendlyError(Throwable t) {
        if (t == null) return "Unknown error.";
        Throwable cause  = t.getCause() != null ? t.getCause() : t;
        String    msg    = cause.getMessage();
        if (msg == null) msg = cause.getClass().getSimpleName();
        if (msg.contains("Ollama") || msg.contains("Connection refused")
                || msg.contains("ConnectException") || msg.contains("unreachable")) {
            return "AI service unavailable. Make sure Ollama is running.\n\n"
                 + "  Start it with:  ollama serve\n"
                 + "  Model needed:   ollama pull llama3";
        }
        return "Error: " + msg;
    }
}
