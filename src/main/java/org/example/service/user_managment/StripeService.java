package org.example.service.user_managment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.util.SessionManager;

import java.time.LocalDateTime;

public class StripeService {

    private static final String API_KEY = System.getenv("STRIPE_SECRET_KEY");
    private static final String STRIPE_PRICE_MONTHLY = "price_1T2u2lJdwesqnbiYpcxNdObE";
    private static final String STRIPE_PRICE_YEARLY  = "price_1T2u3IJdwesqnbiYOk47c9Wz";

    // ── Redirect URLs handled by the embedded webhook server ──────────────────
    // For local dev, these point to the embedded HTTP server on port 8000.
    // For production, change these to your real domain (e.g. https://lingualearn.app/...).
    private static final String SUCCESS_URL = "http://127.0.0.1:8000/payment/success";
    private static final String CANCEL_URL  = "http://127.0.0.1:8000/payment/cancel";

    static {
        Stripe.apiKey = API_KEY;
    }

    // ── 1. Get or create a Stripe Customer for the user ───────────────────────

    public static Customer getOrCreateCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null) {
            return Customer.retrieve(user.getStripeCustomerId());
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFullName())
                .putMetadata("user_id", String.valueOf(user.getId()))
                .build();

        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        new UserRepository().save(user);
        return customer;
    }

    // ── 2. Create a Stripe Checkout Session and return the hosted URL ─────────

    public static String createCheckoutSession(User user, String plan)
            throws StripeException {

        Customer customer = getOrCreateCustomer(user);
        String priceId = "YEARLY".equals(plan) ? STRIPE_PRICE_YEARLY : STRIPE_PRICE_MONTHLY;

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customer.getId())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(SUCCESS_URL + "?session_id={CHECKOUT_SESSION_ID}&plan="
                        + plan.toUpperCase())
                .setCancelUrl(CANCEL_URL)
                .putMetadata("user_id", String.valueOf(user.getId()))
                .putMetadata("plan", plan.toUpperCase())
                .build();

        return Session.create(params).getUrl();
    }

    // ── 3. Schedule cancellation at the end of the current billing period ─────

    public static void cancelAtPeriodEnd(User user) throws StripeException {
        if (user.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("User has no active Stripe subscription.");
        }

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();

        Subscription.retrieve(user.getStripeSubscriptionId()).update(params);
    }

    // ── 4. Cancel the subscription immediately ────────────────────────────────

    public static void cancelImmediately(User user) throws StripeException {
        if (user.getStripeSubscriptionId() == null) {
            throw new IllegalStateException("User has no active Stripe subscription.");
        }

        Subscription.retrieve(user.getStripeSubscriptionId()).cancel();
        user.setStripeSubscriptionId(null);
        new UserRepository().save(user);
        new UserService().downgradeToFree(user);
    }

    // ── 5. Retrieve a Checkout Session with the subscription expanded ─────────

    public static Session retrieveSession(String sessionId) throws StripeException {
        SessionRetrieveParams params = SessionRetrieveParams.builder()
                .addExpand("subscription")
                .build();
        return Session.retrieve(sessionId, params, null);
    }

    // ── 6. Handle a successful Checkout Session (called after Stripe redirect) ─
    // This is called from StripeWebhookServer.handlePaymentSuccess()
    // after the browser is redirected to /payment/success.

    public static void handleSuccessfulPayment(User user, String sessionId, String plan)
            throws StripeException {

        Session session = retrieveSession(sessionId);
        String subscriptionId = session.getSubscription();
        if (subscriptionId != null) {
            user.setStripeSubscriptionId(subscriptionId);
        }

        LocalDateTime expiry = "YEARLY".equals(plan)
                ? LocalDateTime.now().plusYears(1)
                : LocalDateTime.now().plusMonths(1);

        new UserService().upgradeToPremium(user, plan.toUpperCase(), expiry);

        // Refresh the in-memory session user with the latest DB state
        SessionManager.setCurrentUser(
                new UserService().findById(user.getId()).orElse(user));
    }
}