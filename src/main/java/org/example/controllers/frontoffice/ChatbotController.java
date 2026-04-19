package org.example.controllers.frontoffice;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import org.example.services.GroqService;

import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur du chatbot IA (widget flottant style Facebook Messenger).
 */
public class ChatbotController {

    private static final String SYSTEM_PROMPT =
        "Tu es LinguaBot, l'assistant intelligent de LinguaLearn. " +
        "Tu aides les utilisateurs à apprendre des langues, comprendre des publications, " +
        "rédiger des commentaires, et naviguer sur la plateforme. " +
        "Réponds toujours en français, de façon concise, amicale et utile.";

    private final GroqService groqService = new GroqService();

    // Historique de la conversation
    private final List<String[]> history = new ArrayList<>(); // [role, content]

    // UI
    private VBox chatContainer;
    private VBox messageList;
    private ScrollPane scrollPane;
    private TextField inputField;
    private boolean isOpen = false;

    private VBox chatWidget;
    private Button toggleBtn;

    /**
     * Crée et retourne le widget chatbot complet (bouton flottant + fenêtre).
     */
    public StackPane buildWidget() {
        StackPane container = new StackPane();
        container.setPickOnBounds(false);
        container.setMouseTransparent(false);

        // ── BOUTON FLOTTANT ──────────────────────────────────────────────────
        toggleBtn = new Button("💬");
        toggleBtn.setStyle(
            "-fx-background-color: #3b5bdb;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 56px;" +
            "-fx-min-height: 56px;" +
            "-fx-max-width: 56px;" +
            "-fx-max-height: 56px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(59,91,219,0.5), 12, 0, 0, 4);"
        );
        toggleBtn.setOnMouseEntered(e -> toggleBtn.setStyle(
            "-fx-background-color: #2f4ac0;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 56px;" +
            "-fx-min-height: 56px;" +
            "-fx-max-width: 56px;" +
            "-fx-max-height: 56px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(59,91,219,0.7), 16, 0, 0, 6);"
        ));
        toggleBtn.setOnMouseExited(e -> toggleBtn.setStyle(
            "-fx-background-color: #3b5bdb;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 56px;" +
            "-fx-min-height: 56px;" +
            "-fx-max-width: 56px;" +
            "-fx-max-height: 56px;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(59,91,219,0.5), 12, 0, 0, 4);"
        ));
        toggleBtn.setOnAction(e -> toggleChat());

        StackPane.setAlignment(toggleBtn, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toggleBtn, new Insets(0, 20, 20, 0));

        // ── FENÊTRE CHAT ─────────────────────────────────────────────────────
        chatWidget = buildChatWindow();
        chatWidget.setVisible(false);
        chatWidget.setManaged(false);
        StackPane.setAlignment(chatWidget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chatWidget, new Insets(0, 20, 90, 0));

        container.getChildren().addAll(chatWidget, toggleBtn);
        return container;
    }

    private VBox buildChatWindow() {
        VBox window = new VBox(0);
        window.setPrefWidth(340);
        window.setPrefHeight(480);
        window.setMaxWidth(340);
        window.setMaxHeight(480);
        window.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0, 0, 6);" +
            "-fx-border-color: #e4e6eb;" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #3b5bdb, #6d28d9);" +
            "-fx-background-radius: 16 16 0 0;"
        );

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 22px;");

        VBox headerText = new VBox(2);
        Label botName = new Label("LinguaBot");
        botName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label botStatus = new Label("● En ligne");
        botStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #a5b4fc;");
        headerText.getChildren().addAll(botName, botStatus);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50; -fx-background-color: rgba(255,255,255,0.2);"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8; -fx-background-radius: 50;"));
        closeBtn.setOnMouseClicked(e -> toggleChat());

        header.getChildren().addAll(avatar, headerText, headerSpacer, closeBtn);

        // Messages
        messageList = new VBox(10);
        messageList.setPadding(new Insets(12));
        messageList.setFillWidth(true);

        scrollPane = new ScrollPane(messageList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: #f8fafc; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Message de bienvenue
        addBotMessage("👋 Bonjour ! Je suis **LinguaBot**, votre assistant IA sur LinguaLearn.\nComment puis-je vous aider aujourd'hui ?");

        // Input area
        HBox inputArea = new HBox(8);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(10, 12, 12, 12));
        inputArea.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 16 16; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");

        inputField = new TextField();
        inputField.setPromptText("Écrivez votre message...");
        inputField.setStyle(
            "-fx-background-radius: 20;" +
            "-fx-border-radius: 20;" +
            "-fx-border-color: #dde1e7;" +
            "-fx-padding: 9 14;" +
            "-fx-font-size: 13px;" +
            "-fx-background-color: #f0f2f5;"
        );
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
            "-fx-background-color: #3b5bdb;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-background-radius: 50;" +
            "-fx-min-width: 38px;" +
            "-fx-min-height: 38px;" +
            "-fx-max-width: 38px;" +
            "-fx-max-height: 38px;" +
            "-fx-cursor: hand;"
        );
        sendBtn.setOnAction(e -> sendMessage());

        inputArea.getChildren().addAll(inputField, sendBtn);
        window.getChildren().addAll(header, scrollPane, inputArea);
        return window;
    }

    private void toggleChat() {
        isOpen = !isOpen;
        chatWidget.setVisible(isOpen);
        chatWidget.setManaged(isOpen);
        toggleBtn.setText(isOpen ? "✕" : "💬");
        if (isOpen) {
            Platform.runLater(() -> inputField.requestFocus());
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        addUserMessage(text);

        // Message "en train d'écrire..."
        Label typing = new Label("🤖 LinguaBot est en train d'écrire...");
        typing.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b; -fx-padding: 4 12;");
        messageList.getChildren().add(typing);
        scrollToBottom();

        // Appel API en thread séparé
        // Appel API en thread séparé
        new Thread(() -> {
            String response = groqService.chat(SYSTEM_PROMPT, text);
            Platform.runLater(() -> {
                messageList.getChildren().remove(typing);
                addBotMessage(response);
            });
        }).start();
    }

    private void addUserMessage(String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(240);
        bubble.setStyle(
            "-fx-background-color: #3b5bdb;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 10 14;" +
            "-fx-background-radius: 18 18 4 18;"
        );
        wrapper.getChildren().add(bubble);
        messageList.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox wrapper = new HBox(8);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 16px;");

        Label bubble = new Label(text.replace("**", ""));
        bubble.setWrapText(true);
        bubble.setMaxWidth(240);
        bubble.setStyle(
            "-fx-background-color: #f0f2f5;" +
            "-fx-text-fill: #1c1e21;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 10 14;" +
            "-fx-background-radius: 18 18 18 4;"
        );
        wrapper.getChildren().addAll(avatar, bubble);
        messageList.getChildren().add(wrapper);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}

