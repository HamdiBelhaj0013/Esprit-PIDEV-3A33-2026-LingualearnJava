# LinguaLearn — Desktop Application

LinguaLearn is a full-featured **JavaFX 21 desktop application** for language learning, built as a school project (PIDEV 3A33 — Esprit, 2025–2026). It covers user management, course and quiz delivery, a community forum, support ticketing, mock examinations with anti-cheat proctoring, Stripe-based premium subscriptions, and AI-powered features powered by Gemini, Groq, and a locally-hosted Ollama model. The application uses a MySQL database (`1lingualearn_db`) accessed via raw JDBC and Hibernate/JPA and ships with four embedded HTTP servers that handle Stripe webhooks, certificate verification, hCaptcha serving, and anti-cheat event collection.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Database Connection](#database-connection)
5. [Feature Status](#feature-status)
6. [External API Integration](#external-api-integration)
7. [Security Practices](#security-practices)
8. [How to Run](#how-to-run)
9. [Known Issues & Roadmap](#known-issues--roadmap)

---

## Tech Stack

| Technology | Version | Role in Project |
|-----------|---------|----------------|
| Java | 21 | Core language |
| JavaFX (`controls`, `fxml`, `web`, `media`) | 21 | UI framework — scene graph, FXML layouts, WebView for hCaptcha |
| Maven | 3.x | Build tool, fat-JAR packaging |
| MySQL | 8.x | Primary relational database (`1lingualearn_db`) |
| `mysql-connector-j` | 8.3.0 | Raw JDBC driver |
| `hibernate-core` | 6.4.4.Final | JPA/ORM (used for `User` + `LearningStats` entities via `EntityManagerFactory`) |
| `jakarta.validation-api` | 3.0.2 | Bean validation API interfaces |
| `hibernate-validator` | 8.0.1.Final | Bean validation implementation (`@NotBlank`, `@Size`, etc.) |
| `jbcrypt` | 0.4 | BCrypt password hashing (12 rounds) |
| `stripe-java` | 25.3.0 | Stripe payment processing and subscription management |
| `pusher-java-client` | 2.4.0 | Real-time push notifications (support-ticket responses) |
| `com.sun.mail` (jakarta.mail) | 2.0.1 | SMTP email for OTP and verification codes |
| `itext7-core` | 7.2.5 | PDF certificate generation |
| `zxing-core` + `zxing-javase` | 3.5.2 | QR code generation embedded in certificates |
| `poi` + `poi-ooxml` | 5.0.0 | Excel report export |
| `org.json` | 20240303 | JSON parsing for hCaptcha / Stripe responses |
| `gson` | 2.10.1 | JSON serialisation for AI payloads |
| `okhttp3` | 4.12.0 | HTTP client used by AI service calls |
| `ikonli-javafx` + `ikonli-fontawesome5-pack` | 12.3.1 | Icon library for JavaFX UI |
| `logback-classic` | 1.5.13 | SLF4J logging backend |
| `junit-jupiter-api` | 5.10.2 | Unit tests (test scope) |
| `javafx-maven-plugin` | 0.0.8 | `mvn javafx:run` launch |
| `maven-assembly-plugin` | 3.6.0 | Fat-JAR with all dependencies |

---

## Architecture Overview

The application is structured in a layered architecture inside the root package `org.example`.

```
┌────────────────────────────────────────────────────────────────┐
│                      JavaFX UI Layer                          │
│  70+ FXML files + CSS stylesheets  ←→  83 Controller classes  │
└───────────────────────────┬────────────────────────────────────┘
                            │ delegates to
┌───────────────────────────▼────────────────────────────────────┐
│                     Service Layer                              │
│  45 service classes (UserService, MockTestService,            │
│  GeminiService, GroqService, StripeService, …)                │
│  ValidationService (static helpers)                           │
└───────────────────────────┬────────────────────────────────────┘
                            │ reads / writes via
┌───────────────────────────▼────────────────────────────────────┐
│                   Repository / DAO Layer                       │
│  UserRepository (PreparedStatement + ResultSet)               │
│  ReclamationDAO, SupportResponseDAO, DatabaseConnection       │
│  JPA EntityManager (User, LearningStats entities)             │
└───────────────────────────┬────────────────────────────────────┘
                            │ uses
┌───────────────────────────▼────────────────────────────────────┐
│                  Infrastructure / Util                         │
│  MyDataBase (JDBC Singleton)                                  │
│  SessionManager, StageManager (navigation)                    │
│  Session (static userId + role holder)                        │
│  AppConfig (properties loader)                                │
└────────────────────────────────────────────────────────────────┘
```

**Design rules enforced across the project:**
- Controllers never contain SQL or business logic.
- Repositories / DAOs own all JDBC calls and ResultSet mapping.
- Services own validation, BCrypt, and multi-statement transactions.
- Entities are plain POJOs (user management) or JPA-annotated classes (test module).

**Four embedded HTTP servers** start at application launch and shut down on exit:

| Server class | Port | Purpose |
|---|---|---|
| `CaptchaServer` | 18081 | Serves `captcha/captcha.html` over HTTP so JavaFX `WebView` can load it via `localhost` |
| `AntiCheatApiServer` | 9091 | Receives proctoring events during mock tests |
| `CertificateApiServer` | 9090 | Verifies certificate UUIDs for QR-code scanning |
| `StripeWebhookServer` | 8000 | Processes Stripe payment lifecycle webhooks |

---

## Project Structure

```
src/main/java/org/example/
├── App.java                          Application entry point
├── controller/                       83 JavaFX controllers
│   ├── LoginController.java
│   ├── admin/
│   │   ├── AdminMainController.java
│   │   ├── DashboardViewController.java
│   │   ├── UserListController.java
│   │   ├── UserDetailController.java
│   │   ├── UserFormController.java
│   │   ├── StatsController.java
│   │   ├── NotificationController.java
│   │   ├── user_managment/           (6 controllers)
│   │   ├── forum/                    (7 controllers)
│   │   └── support-managment/        (3 controllers)
│   └── user/
│       ├── UserMainController.java
│       ├── UserProfileController.java
│       ├── user_managment/           (8 controllers)
│       ├── forum/                    (7 controllers)
│       └── support-managment/        (2 controllers)
│   └── tests/                        (13 controllers)
├── entity/
│   ├── User.java                     JPA + POJO — users table
│   ├── LearningStats.java            JPA — learning_stats table
│   └── tests/
│       ├── MockTest.java, TestQuestion.java, TestAnswer.java
│       ├── TestResult.java, Certificate.java, PlatformLanguage.java
├── entities/                         Non-JPA POJOs for other modules
│   ├── Quiz.java, Exercice.java, Lesson.java, Course.java
│   ├── FAQ.java, Rating.java
│   ├── Reclamation.java, SupportResponse.java
│   └── forum/  (Publication.java, Commentaire.java, Notification.java)
├── repository/
│   ├── user-managment/UserRepository.java   PreparedStatement DAO
│   ├── support-managment/
│   │   ├── DatabaseConnection.java
│   │   ├── ReclamationDAO.java
│   │   └── SupportResponseDAO.java
│   └── tests/                        (6 JPA-based repositories)
├── service/                          45 service classes
│   ├── user_managment/
│   │   ├── UserService.java, IUserService.java
│   │   ├── EmailService.java, EmailVerificationService.java
│   │   ├── PasswordResetService.java
│   │   ├── StripeService.java, NotificationService.java
│   ├── support-managment/            (10 classes inc. PusherService)
│   ├── forum/                        (6 classes inc. GroqService)
│   ├── tests/
│   │   ├── MockTestService.java, CertificateService.java
│   │   ├── AntiCheatGuard.java, AntiCheatApiServer.java
│   │   ├── TestPerformanceAnalyzer.java
│   │   ├── GroqSpeakingService.java, GroqWritingService.java
│   │   └── (6 more)
│   ├── ai/
│   │   ├── OllamaService.java, AdminAiService.java
│   │   ├── AiActionExecutor.java, UserDatasetBuilder.java
│   │   └── AdminAiNotificationWriter.java
│   ├── GeminiService.java
│   ├── HCaptchaService.java
│   └── QuizStripeService.java
├── service_layer/                    CourseService, LessonService, etc.
├── util/
│   ├── MyDataBase.java               JDBC Singleton
│   ├── Session.java                  Static userId + role holder
│   ├── AppConfig.java                .properties loader
│   └── user-managment/
│       ├── SessionManager.java
│       └── StageManager.java
├── validation/
│   └── user-managment/ValidationService.java
└── webhook/
    └── StripeWebhookServer.java

src/main/resources/
├── META-INF/persistence.xml          JPA persistence unit config
├── fxml/                             70+ FXML screens
│   ├── login.fxml, Register.fxml
│   ├── ForgotPassword.fxml, NewPassword.fxml
│   ├── VerifyEmail.fxml, VerifyOTP.fxml
│   ├── admin/                        (12 FXML files)
│   ├── user/                         (6 FXML files)
│   ├── tests/                        (11 FXML files)
│   └── modules/                      (6 FXML files)
├── css/
│   ├── style.css, styles.css
│   ├── admin.css, user.css
│   ├── backoffice.css, frontoffice.css
│   └── ai-assistant.css
└── captcha/captcha.html              hCaptcha HTML page
```

---

## Database Connection

### Dual-access pattern

The project uses **two parallel DB access strategies**:

**1. JDBC Singleton (`MyDataBase.java`)** — used by UserRepository, all DAOs, and the support-management module:

```java
// org.example.util.MyDataBase
public class MyDataBase {
    private static MyDataBase instance;
    private Connection connection;

    private final String URL      = "jdbc:mysql://localhost:3306/1lingualearn_db";
    private final String USER     = "root";
    private final String PASSWORD = "";         // empty for local dev

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion établie !");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static MyDataBase getInstance() {
        if (instance == null) instance = new MyDataBase();
        return instance;
    }

    public Connection getConnection() { return connection; }
    public void closeConnection() throws SQLException { connection.close(); }
}
```

**2. JPA / Hibernate (`persistence.xml`)** — used by the tests module repositories:

```xml
<!-- src/main/resources/META-INF/persistence.xml -->
<persistence-unit name="lingualearn" transaction-type="RESOURCE_LOCAL">
  <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
  <class>org.example.entity.User</class>
  <class>org.example.entity.LearningStats</class>
  <property name="jakarta.persistence.jdbc.url"
            value="jdbc:mysql://localhost:3306/1lingualearn_db"/>
  <property name="jakarta.persistence.jdbc.user"    value="root"/>
  <property name="jakarta.persistence.jdbc.password" value=""/>
  <property name="hibernate.dialect"
            value="org.hibernate.dialect.MySQLDialect"/>
  <property name="hibernate.hbm2ddl.auto" value="none"/>
</persistence-unit>
```

**Connection string:** `jdbc:mysql://localhost:3306/1lingualearn_db`  
**Credentials:** `root` / *(empty — development only)*  
**Pooling:** None. Single `java.sql.Connection` for the JDBC path; Hibernate manages its own internal pool for the JPA path.  
**Lifecycle:** `App.init()` opens the JDBC connection; `App.stop()` closes it.

---

## Feature Status

### User Management

| Feature | Status | Notes |
|---------|--------|-------|
| Registration (name, email, password) | ✅ Done | BCrypt hash, email uniqueness check |
| Login + role-based routing | ✅ Done | Routes to `AdminMain.fxml` or `UserMain.fxml` |
| Logout | ✅ Done | `SessionManager.clearSession()` |
| Password reset via OTP email | ✅ Done | 6-digit code, 30-min expiry, SMTP via Gmail |
| Email verification on signup | ✅ Done | `isVerified` flag; currently auto-verified |
| Profile view & edit (name, email) | ✅ Done | Inline edit mode toggle |
| Password change from profile | ✅ Done | Strength bar + match indicator |
| Admin: user list (paginated, sortable, filterable) | ✅ Done | `UserListController` + `UserRepository.findAdvanced()` |
| Admin: create / edit user | ✅ Done | `UserFormController` |
| Admin: activate / suspend / delete user | ✅ Done | `UserDetailController` |
| Admin: edit learning stats (XP, words, minutes) | ✅ Done | `StatsController` |
| Admin: send in-app notification | ✅ Done | `NotificationController` → `NotificationService` |
| Admin: ban user with reason | ✅ Done | `isBanned` + `banReason` fields |
| AI-powered user search (natural language) | ✅ Done | `AdminAiService` → Ollama |
| AI user insights & platform trends | ✅ Done | `AdminAiService` → Ollama |
| hCaptcha on registration | ✅ Done | WebView loads `captcha.html` from `CaptchaServer:18081` |
| Premium subscription upgrade/downgrade | ✅ Done | `StripeService` checkout session flow |
| Stripe subscription management | ✅ Done | Cancel at period end, immediate cancel |
| Stripe webhook processing | ✅ Done | `StripeWebhookServer:8000` handles lifecycle events |

### Courses, Quizzes & Exercises

| Feature | Status | Notes |
|---------|--------|-------|
| Course listing | ✅ Done | `CourseService` + FXML views |
| Lesson display | ✅ Done | `LessonService` |
| Quiz listing | ✅ Done | `QuizController` |
| Quiz play with scoring | ✅ Done | `QuizPlayController` + `AntiCheatGuard` |
| AI quiz explanation (post-quiz) | ✅ Done | `GeminiService` — gemini-2.5-flash |
| Quiz result display | ✅ Done | `QuizResultController` |
| Exercise view | ✅ Done | `ExerciceController` |
| Premium quiz paywall (Stripe) | ✅ Done | `QuizStripeService` |

### Mock Tests & Certification

| Feature | Status | Notes |
|---------|--------|-------|
| Mock test CRUD (admin) | ✅ Done | `MockTestService` |
| Test question management | ✅ Done | `TestQuestionService` |
| Test taking with timer | ✅ Done | `MockTestTakingController` |
| Anti-cheat: focus-loss detection | ✅ Done | `AntiCheatGuard` → `AntiCheatApiServer:9091` |
| Anti-cheat: copy/paste blocking | ✅ Done | Ctrl+C / Ctrl+V intercepted |
| Anti-cheat: forced submission on 2nd violation | ✅ Done | −50% score penalty |
| AI speaking evaluation (Groq) | ✅ Done | `GroqSpeakingService` |
| AI writing evaluation (Groq) | ✅ Done | `GroqWritingService` |
| Test performance analysis | ✅ Done | `TestPerformanceAnalyzer` |
| PDF certificate generation (iText7 + QR code) | ✅ Done | `CertificateService` → `CertificateApiServer:9090` |
| Multi-language support selection | ✅ Done | `PlatformLanguage` entity |

### Forum

| Feature | Status | Notes |
|---------|--------|-------|
| Create / edit / delete posts | ✅ Done | `ServicePublication` |
| Comments | ✅ Done | `ServiceCommentaire` |
| AI response suggestion (Groq) | ✅ Done | `GroqService` (forum) |
| Bad word filtering | ✅ Done | `BadWordChecker` |
| Forum notifications | ✅ Done | `NotificationManager` |
| AI moderation suggestions (Gemini) | ✅ Done | Forum-specific `GeminiService` |

### Support / Reclamations

| Feature | Status | Notes |
|---------|--------|-------|
| Submit support ticket (with image) | ✅ Done | `ReclamationDAO` — stores image path in `uploads/reclamations/` |
| Admin: view & respond to tickets | ✅ Done | `SupportResponseDAO` |
| Real-time push notification on response | ✅ Done | `PusherService` (Pusher.com, cluster `eu`) |
| Priority auto-detection | ✅ Done | `PriorityDetector` |
| Bad word filtering on tickets | ✅ Done | `BadWordsFilter` |
| SLA deadline tracking | ✅ Done | `sla_deadline` column in `reclamation` table |
| FAQ management | ✅ Done | `FAQ` entity + CRUD |

---

## External API Integration

### hCaptcha

hCaptcha is integrated on the **registration screen** to prevent bot sign-ups.

**Architecture:** Because JavaFX `WebView` cannot submit cross-origin forms, the application embeds a local HTTP server (`CaptchaServer`) that serves `captcha/captcha.html` on `http://localhost:18081`. The FXML `WebView` loads this URL, renders the hCaptcha widget (using the public site key embedded in the HTML), and on challenge completion executes a JavaScript bridge that posts the `h-captcha-response` token back to the JavaFX controller via `WebEngine.executeScript()`.

**Verification:** `HCaptchaService.verify(String token)` POSTs the token to `https://api.hcaptcha.com/siteverify` along with the secret (read from `System.getenv("HCAPTCHA_SECRET")`). A JSON response of `{"success":true}` allows registration to proceed.

**Status:** Fully implemented and correctly secured — secret key is read from an environment variable, not hardcoded.

### Stripe Payments

- **Library:** `stripe-java` 25.3.0
- **Flow:** `StripeService.createCheckoutSession(user, plan)` creates a hosted Stripe Checkout session and returns the URL. A JavaFX `WebView` opens this URL. On completion, Stripe calls the embedded webhook server at `http://127.0.0.1:8000/stripe/webhook`.
- **Webhook signature validation:** `Webhook.constructEvent(body, sig, secret)` — HMAC-SHA256.
- **Events handled:** `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_succeeded`.
- **Secret key:** `System.getenv("STRIPE_SECRET_KEY")` — correctly secured in `StripeService`.
- **Known issue:** `QuizStripeService` hardcodes a test key directly in source (see Known Issues).

### Gemini (Google Generative Language API)

- **Model:** `gemini-2.5-flash`
- **Used for:** Quiz answer explanations, forum content moderation suggestions.
- **Known issue:** API key is hardcoded in `GeminiService.java` (see Known Issues).

### Groq

- **Endpoint:** `https://api.groq.com/openai/v1/chat/completions`
- **Used for:** AI writing evaluation (`GroqWritingService`), AI speaking evaluation (`GroqSpeakingService`), forum reply suggestions (`GroqService`).
- **Key resolution:** `GroqSpeakingService` and `GroqWritingService` check `GROQ_API_KEY` then `GROK_API_KEY` environment variables, then `System.getProperty("GROQ_API_KEY")`.

### Ollama (Local LLM)

- **Endpoint:** `http://127.0.0.1:11434/api/chat`
- **Used for:** Admin AI assistant (`AdminAiService`) — natural language user search, user insights, platform trend analysis.
- **Requires:** Ollama running locally with a model loaded (e.g. `mistral`).

### Pusher

- **Cluster:** `eu`
- **Used for:** Real-time notification to users when an admin responds to a support ticket. `PusherService.notifierUser(int userId, String subject)` sends a signed HTTP POST to the Pusher API on channel `user-{userId}` with event `nouvelle-reponse`.
- **Known issue:** App ID, key, and secret are hardcoded in `PusherService.java` (see Known Issues).

---

## Security Practices

### Password Hashing

BCrypt via `org.mindrot.jbcrypt` (version 0.4), 12 rounds:

```java
// org.example.service.user_managment.UserService
private static final int BCRYPT_ROUNDS = 12;

public String hashPassword(String plain) {
    return BCrypt.hashpw(plain, BCrypt.gensalt(BCRYPT_ROUNDS));
}

public boolean verifyPassword(String plain, String hash) {
    // Handles both $2a$ and $2y$ BCrypt prefixes
    if (hash != null && hash.startsWith("$2y$"))
        hash = "$2a$" + hash.substring(4);
    return BCrypt.checkpw(plain, hash);
}
```

### SQL Injection Prevention

`UserRepository` and the support-management DAOs use `PreparedStatement` for all parameterised queries including dynamic search (`LIKE ?`) and multi-column sorts. No user input is concatenated directly into SQL strings.

```java
// UserRepository.search()
String sql = SELECT_BASE +
    " WHERE LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, "%" + term.toLowerCase() + "%");
ps.setString(2, "%" + term.toLowerCase() + "%");
ps.setString(3, "%" + term.toLowerCase() + "%");
```

### Input Validation

`ValidationService` provides static validators called from the service layer before any DB write:

| Validator | Rule |
|-----------|------|
| `requireNonBlank(value, field)` | not null, not whitespace-only |
| `requireMinLength(value, field, min)` | `value.length() >= min` |
| `requireValidEmail(email)` | regex `^[\w._%+\-]+@[\w.\-]+\.[a-zA-Z]{2,}$` |
| `requirePasswordsMatch(pw, confirm)` | equality check |
| `requireValidPlan(plan)` | one of `FREE`, `MONTHLY`, `YEARLY` |
| `requireValidRole(role)` | one of `ROLE_USER`, `ROLE_ADMIN`, `ROLE_TEACHER` |
| `validateOrThrow(entity)` | full Jakarta Bean Validation |

### hCaptcha

Bot-prevention on registration. Secret verified server-side via `HCaptchaService`. Secret key loaded from `System.getenv("HCAPTCHA_SECRET")` — not hardcoded.

### Stripe Webhook Verification

Every incoming webhook payload is verified via Stripe's HMAC-SHA256 signature before processing. Secret loaded from `System.getenv("STRIPE_WEBHOOK_SECRET")`.

### AI Data Privacy

`UserDatasetBuilder` strips `password`, `stripeCustomerId`, `stripeSubscriptionId`, and internal tokens from the user dataset before any AI API call.

---

## How to Run

### Prerequisites

| Requirement | Version |
|------------|---------|
| JDK | 21+ |
| Maven | 3.8+ |
| MySQL | 8.x, running on `localhost:3306` |
| Database | `1lingualearn_db` created and schema migrated |
| Ollama *(optional)* | Latest, with a model loaded (e.g. `mistral`) |

### Environment Variables

The following variables must be set before launch for full functionality:

```bash
export STRIPE_SECRET_KEY=sk_live_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export HCAPTCHA_SECRET=<your-hcaptcha-secret>
export GROQ_API_KEY=<your-groq-api-key>
```

### Create the database

```sql
CREATE DATABASE 1lingualearn_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Run (development)

```bash
mvn javafx:run
```

### Build a fat JAR

```bash
mvn clean package
java -jar target/lingualearn-1.0-jar-with-dependencies.jar
```

### Database credentials

Connection parameters are hardcoded in `src/main/java/org/example/util/MyDataBase.java` and in `src/main/resources/META-INF/persistence.xml`. Edit both files if your MySQL credentials differ from `root` / *(empty password)*.

---

## Known Issues & Roadmap

### Critical Security Issues (Fix Before Any Public Deployment)

| Issue | Location | Risk |
|-------|----------|------|
| Gemini API key hardcoded in source | `GeminiService.java` | Quota abuse, billing attack |
| Stripe test key hardcoded in source | `QuizStripeService.java` | Should be in environment variable |
| Gmail app password hardcoded in source | `EmailService.java` | Email account compromise |
| Pusher app ID / key / secret hardcoded | `PusherService.java` | Unauthorized notifications, channel hijacking |
| MySQL root / empty password | `MyDataBase.java`, `persistence.xml` | Acceptable for local dev only |

All of these credentials must be rotated and moved to environment variables before the project is deployed to any shared or internet-facing environment.

### Architecture & Quality

| Issue | Notes |
|-------|-------|
| Dual DB access strategies | `UserRepository` uses raw JDBC; `tests` module repositories use JPA. Should be unified |
| Static session variables | `Session.java` uses static fields — not thread-safe, not serialisable |
| In-memory OTP storage | Verification codes stored in a `HashMap` — lost on restart, no distributed-safe expiry |
| No connection pooling on JDBC path | Single `java.sql.Connection` shared across all queries |
| Blocking AI calls on UI thread | Some Ollama/Gemini calls must be wrapped in `Task<>` to avoid freezing the UI |
| Hardcoded localhost URLs | `SUCCESS_URL`/`CANCEL_URL` in `StripeService` and embedded server ports are fixed |

### Incomplete / Planned Features

| Item | Status |
|------|--------|
| Email verification enforced before login | `isVerified` is stored but currently auto-set to `true` on registration |
| Rate limiting on login attempts | Not implemented |
| Full teacher role UI | `ROLE_TEACHER` exists in validation but no dedicated teacher dashboard |
| Audit log for admin actions | Not implemented |
| Internationalisation (i18n) | UI strings are in French and English, mixed |

---

## Contributors

PIDEV 3A33 — Esprit School of Engineering, 2025–2026.  
5-person team. Repository: [HamdiBelhaj0013/Esprit-PIDEV-3A33-2026-LingualearnJava](https://github.com/HamdiBelhaj0013/Esprit-PIDEV-3A33-2026-LingualearnJava)
