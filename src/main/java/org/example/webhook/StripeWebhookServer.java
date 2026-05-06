package org.example.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import org.example.repository.UserRepository;
import org.example.service.user_managment.StripeService;
import org.example.service.user_managment.UserService;
import org.example.util.SessionManager;
import org.example.util.StageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Embedded HTTP server that receives and processes Stripe webhook events.
 * Listens on http://127.0.0.1:8000/stripe/webhook.
 *
 * <p>Start with the Stripe CLI:
 * <pre>stripe listen --forward-to http://127.0.0.1:8000/stripe/webhook</pre>
 */
public class StripeWebhookServer {

    private static final Logger LOG = LoggerFactory.getLogger(StripeWebhookServer.class);
    private static final int    PORT = 8000;

    private final String    webhookSecret;
    private       HttpServer server;

    public StripeWebhookServer(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() throws IOException {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/stripe/webhook",  this::handleWebhook);
            server.createContext("/payment/success", this::handlePaymentSuccess);
            server.createContext("/payment/cancel",  this::handlePaymentCancel);
            server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "stripe-webhook");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            LOG.info("Stripe webhook server listening on http://127.0.0.1:{}/stripe/webhook", PORT);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                LOG.warn("Stripe webhook server port {} already in use — assuming previous instance is still running.", PORT);
                // Don't throw — let the app start normally
            } else {
                throw e; // re-throw unexpected errors
            }
        }
    }

    /** Called from Application.stop() — gives in-flight requests 2 s to finish. */
    public void stop() {
        if (server != null) {
            server.stop(2);
            LOG.info("Stripe webhook server stopped.");
        }
    }

    // ── /payment/success ──────────────────────────────────────────────────────

    private void handlePaymentSuccess(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);

        String sessionId = params.get("session_id");
        String plan      = params.get("plan");

        // 1. Send success HTML to browser immediately
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Payment Successful – LinguaLearn</title>
                <style>
                    body { font-family: sans-serif; text-align: center;
                           padding: 80px; background: #f0fdf4; }
                    h1   { color: #16a34a; font-size: 2rem; }
                    p    { font-size: 1.1rem; color: #374151; }
                    .badge { display:inline-block; background:#16a34a; color:white;
                             padding:6px 18px; border-radius:999px; font-weight:600; }
                </style>
            </head>
            <body>
                <h1>✅ Payment Successful!</h1>
                <p>Your <span class="badge">%s</span> subscription is now active.</p>
                <p>You can close this tab and return to <b>LinguaLearn</b>.</p>
            </body>
            </html>
            """.formatted(plan != null ? plan : "PREMIUM");

        sendHtml(exchange, 200, html);

        // 2. Upgrade user in DB and refresh UI on the JavaFX thread
        Platform.runLater(() -> {
            try {
                var currentUser = SessionManager.getCurrentUser();
                if (currentUser != null && sessionId != null && plan != null) {
                    StripeService.handleSuccessfulPayment(currentUser, sessionId, plan);
                    LOG.info("[Stripe] User {} upgraded to {} via success redirect", currentUser.getId(), plan);
                    // Refresh the current screen so the dashboard shows the new plan
                    try {
                        StageManager.refreshCurrentStage();
                    } catch (IOException ex) {
                        LOG.error("[Stripe] Failed to refresh stage after payment: {}", ex.getMessage(), ex);
                    }
                } else {
                    LOG.warn("[Stripe] handlePaymentSuccess: missing user or params (sessionId={}, plan={})", sessionId, plan);
                }
            } catch (Exception e) {
                LOG.error("[Stripe] Failed to upgrade user after payment: {}", e.getMessage(), e);
            }
        });
    }

    // ── /payment/cancel ───────────────────────────────────────────────────────

    private void handlePaymentCancel(HttpExchange exchange) throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Payment Cancelled – LinguaLearn</title>
                <style>
                    body { font-family: sans-serif; text-align: center;
                           padding: 80px; background: #fef2f2; }
                    h1   { color: #dc2626; font-size: 2rem; }
                    p    { font-size: 1.1rem; color: #374151; }
                </style>
            </head>
            <body>
                <h1>Payment Cancelled</h1>
                <p>No charge was made. You can close this tab and return to <b>LinguaLearn</b>.</p>
            </body>
            </html>
            """;
        sendHtml(exchange, 200, html);
        LOG.info("[Stripe] Payment cancelled by user.");
    }

    // ── /stripe/webhook ───────────────────────────────────────────────────────

    private void handleWebhook(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "Method Not Allowed");
            return;
        }

        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String payload   = new String(bodyBytes, StandardCharsets.UTF_8);
        String sigHeader = exchange.getRequestHeaders().getFirst("Stripe-Signature");

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            LOG.warn("Webhook signature verification failed: {}", e.getMessage());
            respond(exchange, 400, "Invalid signature");
            return;
        } catch (Exception e) {
            LOG.error("Webhook parse error: {}", e.getMessage(), e);
            respond(exchange, 400, "Bad request");
            return;
        }

        try {
            dispatch(event);
            respond(exchange, 200, "OK");
        } catch (Exception e) {
            LOG.error("Error processing event {}: {}", event.getType(), e.getMessage(), e);
            respond(exchange, 500, "Internal error");
        }
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    private void dispatch(Event event) {
        EventDataObjectDeserializer d = event.getDataObjectDeserializer();
        switch (event.getType()) {
            case "checkout.session.completed" ->
                    d.getObject().ifPresent(o -> handleCheckoutCompleted((Session) o));
            case "customer.subscription.created" ->
                    d.getObject().ifPresent(o -> handleSubscriptionCreated((Subscription) o));
            case "customer.subscription.updated" ->
                    d.getObject().ifPresent(o -> handleSubscriptionUpdated((Subscription) o));
            case "invoice.payment_succeeded" ->
                    d.getObject().ifPresent(o -> handleInvoicePaymentSucceeded((Invoice) o));
            default ->
                    LOG.debug("Ignored Stripe event: {}", event.getType());
        }
    }

    // ── checkout.session.completed ────────────────────────────────────────────

    private void handleCheckoutCompleted(Session session) {
        String userIdStr = session.getMetadata().get("user_id");
        String plan      = session.getMetadata().get("plan");
        if (userIdStr == null || plan == null) {
            LOG.warn("checkout.session.completed: missing user_id or plan in session metadata");
            return;
        }

        long userId = Long.parseLong(userIdStr);
        UserService svc = new UserService();
        svc.findById(userId).ifPresentOrElse(user -> {
            String subId = session.getSubscription();
            if (subId != null) user.setStripeSubscriptionId(subId);

            LocalDateTime expiry = "YEARLY".equals(plan)
                    ? LocalDateTime.now().plusYears(1)
                    : LocalDateTime.now().plusMonths(1);

            svc.upgradeToPremium(user, plan, expiry);

            // Also refresh SessionManager if this is the currently logged-in user
            Platform.runLater(() -> {
                var currentUser = SessionManager.getCurrentUser();
                if (currentUser != null && currentUser.getId() == userId) {
                    svc.findById(userId).ifPresent(updated -> {
                        SessionManager.setCurrentUser(updated);
                        try {
                            StageManager.refreshCurrentStage();
                        } catch (IOException ex) {
                            LOG.error("[Stripe] Failed to refresh stage after checkout: {}", ex.getMessage(), ex);
                        }
                    });
                }
            });

            LOG.info("checkout.session.completed: activated {} plan for user {}", plan, userId);
        }, () -> LOG.warn("checkout.session.completed: no user found for id={}", userId));
    }

    // ── customer.subscription.created ────────────────────────────────────────

    private void handleSubscriptionCreated(Subscription subscription) {
        String customerId = subscription.getCustomer();
        UserRepository repo = new UserRepository();
        repo.findByStripeCustomerId(customerId).ifPresentOrElse(user -> {
            user.setStripeSubscriptionId(subscription.getId());
            repo.save(user);
            LOG.info("subscription.created: stored sub {} for user {}", subscription.getId(), user.getId());
        }, () -> LOG.warn("subscription.created: no user found for customer={}", customerId));
    }

    // ── customer.subscription.updated ────────────────────────────────────────

    private void handleSubscriptionUpdated(Subscription subscription) {
        String customerId = subscription.getCustomer();
        String status     = subscription.getStatus();
        new UserRepository().findByStripeCustomerId(customerId).ifPresentOrElse(user -> {
            if ("canceled".equals(status)
                    || "unpaid".equals(status)
                    || "incomplete_expired".equals(status)) {
                new UserService().downgradeToFree(user);
                LOG.info("subscription.updated: downgraded user {} (status={})", user.getId(), status);
            } else {
                LOG.debug("subscription.updated: status={} for user {} — no action", status, user.getId());
            }
        }, () -> LOG.warn("subscription.updated: no user found for customer={}", customerId));
    }

    // ── invoice.payment_succeeded ─────────────────────────────────────────────

    private void handleInvoicePaymentSucceeded(Invoice invoice) {
        if ("subscription_create".equals(invoice.getBillingReason())) {
            LOG.debug("invoice.payment_succeeded: skipping initial invoice (handled by checkout.session.completed)");
            return;
        }

        String customerId = invoice.getCustomer();
        new UserRepository().findByStripeCustomerId(customerId).ifPresentOrElse(user -> {
            Long periodEnd = invoice.getLines() != null && invoice.getLines().getData() != null
                    ? invoice.getLines().getData().stream()
                    .filter(item -> item.getPeriod() != null)
                    .map(item -> item.getPeriod().getEnd())
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null)
                    : null;

            if (periodEnd != null) {
                LocalDateTime expiry = LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
                String plan = user.getSubscriptionPlan();
                if ("FREE".equals(plan)) plan = "MONTHLY";
                new UserService().upgradeToPremium(user, plan, expiry);
                LOG.info("invoice.payment_succeeded: renewed {} for user {} until {}", plan, user.getId(), expiry);
            } else {
                LOG.warn("invoice.payment_succeeded: no period_end found for customer={}", customerId);
            }
        }, () -> LOG.warn("invoice.payment_succeeded: no user found for customer={}", customerId));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isBlank()) return Map.of();
        return Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(
                        a -> URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                        a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)
                ));
    }

    private void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}