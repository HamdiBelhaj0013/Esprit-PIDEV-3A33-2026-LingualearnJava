# LinguaLearn — Desktop Application

A JavaFX 21 desktop client for the LinguaLearn language-learning platform.
It mirrors the data model of the companion Symfony web application and shares
the same MySQL database (`1lingualearn_db`).

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [File Reference](#file-reference)
4. [Database Schema](#database-schema)
5. [Architecture](#architecture)
6. [Connection Flow](#connection-flow)
7. [Authentication & Role Routing](#authentication--role-routing)
8. [Feature Status](#feature-status)
9. [How to Run](#how-to-run)

---

## Tech Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Language | Java | 21 | Core language (records, pattern switch, sealed classes available) |
| UI framework | JavaFX | 21 | FXML-based scene graph, CSS styling |
| Build tool | Maven | 3.x | Dependency management, fat-JAR packaging |
| Database | MySQL | 8.x | Persistent storage, shared with Symfony web app |
| JDBC driver | mysql-connector-j | 8.3.0 | Raw JDBC connection to MySQL |
| Password hashing | jbcrypt | 0.4 | BCrypt with 12 rounds; compatible with Symfony's `$2y$` hashes |
| Bean Validation | hibernate-validator | 8.0.1.Final | `@NotBlank`, `@Size`, etc. on service-layer DTOs (no ORM) |
| Validation API | jakarta.validation-api | 3.0.2 | API interfaces for bean validation |
| EL support | jakarta.el | 4.0.2 | Required by Hibernate Validator for message expressions |
| JPA API (stub) | jakarta.persistence-api | 3.1.0 | **Compile-time only** — admin controllers import JPA types; no Hibernate runtime is present |
| Logging | logback-classic | 1.5.13 | SLF4J backend |
| Testing | junit-jupiter-api | 5.10.2 | Unit tests (test scope) |

> **Important:** `hibernate-core` has been fully removed. The only JPA artifact
> on the classpath is the thin `jakarta.persistence-api` interfaces jar, kept
> solely so the (not-yet-migrated) admin controllers compile. No JPA provider
> runs at runtime.

---

## Project Structure

```
LingualearnJava/
├── pom.xml                          Maven build descriptor
├── README.md                        This file
└── src/
    └── main/
        ├── java/org/example/
        │   ├── App.java
        │   ├── controller/
        │   │   ├── LoginController.java
        │   │   ├── RegisterController.java
        │   │   ├── admin/
        │   │   │   ├── AdminMainController.java
        │   │   │   ├── DashboardViewController.java
        │   │   │   ├── NotificationController.java
        │   │   │   ├── StatsController.java
        │   │   │   ├── UserDetailController.java
        │   │   │   ├── UserFormController.java
        │   │   │   └── UserListController.java
        │   │   └── user/
        │   │       ├── UserMainController.java
        │   │       └── UserProfileController.java
        │   ├── entity/
        │   │   ├── User.java
        │   │   └── LearningStats.java
        │   ├── repository/
        │   │   └── UserRepository.java
        │   ├── service/
        │   │   ├── IUserService.java
        │   │   ├── UserService.java
        │   │   └── NotificationService.java
        │   ├── util/
        │   │   ├── MyDataBase.java
        │   │   ├── SessionManager.java
        │   │   ├── StageManager.java
        │   │   └── Menu.java
        │   └── validation/
        │       └── ValidationService.java
        └── resources/
            ├── fxml/
            │   ├── login.fxml
            │   ├── Register.fxml
            │   ├── admin/
            │   │   ├── AdminMain.fxml
            │   │   ├── DashboardView.fxml
            │   │   ├── NotificationDialog.fxml
            │   │   ├── StatsDialog.fxml
            │   │   ├── UserDetailDialog.fxml
            │   │   ├── UserFormDialog.fxml
            │   │   └── UserListView.fxml
            │   └── user/
            │       ├── UserMain.fxml
            │       └── UserProfileView.fxml
            └── css/
                ├── style.css
                ├── admin.css
                └── user.css
```

---

## File Reference

### Entry Point

#### `App.java`
JavaFX `Application` subclass. Responsibilities:
- `init()` — calls `MyDataBase.getInstance()` to open the JDBC connection at
  startup so any configuration error surfaces immediately.
- `start(Stage)` — registers the primary stage with `StageManager`, loads
  `login.fxml`, and shows the window.
- `getEmf()` — **stub that returns `null`**. Kept only so admin controllers
  that have not yet been migrated compile. Do not call at runtime.

---

### `util/` Package

#### `MyDataBase.java`
**Singleton JDBC connection.**

```
package org.example.util
Pattern : classic Singleton (private constructor, static getInstance())
Connection: DriverManager.getConnection(URL, USER, PASSWORD)
URL  : jdbc:mysql://localhost:3306/1lingualearn_db
User : root
Pass : (empty)
```

- `getInstance()` — returns or creates the single instance.
- `getConnection()` — returns the raw `java.sql.Connection`.
- Prints `"Connexion établie !"` to stdout on successful connect.
- No connection pooling; no auto-reconnect. For a desktop app with a single
  user this is intentional and sufficient.

#### `SessionManager.java`
**In-memory holder for the currently authenticated user.**

- `setCurrentUser(User)` / `getCurrentUser()` — set/get after successful login.
- `clearSession()` — called on logout; nulls the current user.
- No EntityManager or JPA dependency.

#### `StageManager.java`
**Centralised JavaFX scene switcher.**

- `setPrimaryStage(Stage)` — called once from `App.start()`.
- `switchScene(String fxmlPath)` — loads FXML, replaces current scene.
- `switchScene(String fxmlPath, Consumer<C> init)` — same but lets the caller
  initialise the controller before the scene becomes visible.

Used by: `LoginController` (→ Register), `RegisterController` (→ Login).
The dashboard navigation still uses direct `Stage` casting inside controllers.

#### `Menu.java`
Legacy console UI helper (print tables, prompt helpers). Retained for
reference; not called by any active code path.

---

### `entity/` Package

Both entities are **plain POJOs** — zero JPA annotations, zero Hibernate
dependencies. The repository is solely responsible for reading and writing
them via SQL.

#### `User.java`

| Field | Java type | DB column | Notes |
|-------|-----------|-----------|-------|
| `id` | `Long` | `id` BIGINT AUTO | Set by repository after INSERT |
| `email` | `String` | `email` | Unique; lowercased before save |
| `password` | `String` | `password` | BCrypt hash (`$2a$12$…`) |
| `roles` | `String` | `roles` JSON | Symfony JSON array: `["ROLE_USER"]` |
| `firstName` | `String` | `first_name` | |
| `lastName` | `String` | `last_name` | |
| `subscriptionPlan` | `String` | `subscription_plan` | `FREE` / `MONTHLY` / `YEARLY` |
| `subscriptionExpiry` | `LocalDateTime` | `subscription_expiry` | null when FREE |
| `isPremium` | `boolean` | `is_premium` | **Computed**, never set directly |
| `lastPaymentStatus` | `String` | `last_payment_status` | `success` / `failed` / null |
| `status` | `String` | `status` | `active` / `suspended` / `deleted` |
| `createdAt` | `LocalDateTime` | `created_at` | Set on first INSERT |
| `isVerified` | `boolean` | `is_verified` | |
| `stripeCustomerId` | `String` | `stripe_customer_id` | |
| `stripeSubscriptionId` | `String` | `stripe_subscription_id` | |
| `isBanned` | `boolean` | `is_banned` | |
| `banReason` | `String` | `ban_reason` | |
| `learningStats` | `LearningStats` | — | POJO reference, loaded by JOIN |

**Key methods:**
- `updatePremiumStatus()` — recalculates `isPremium` from plan + expiry.
  Called by `setSubscriptionPlan()`, `setSubscriptionExpiry()`, and explicitly
  by `UserRepository.mapUser()` after all fields are loaded from the ResultSet
  (replaces the old `@PostLoad` hook).
- `getRoles()` / `setRoles(List<String>)` — parse/serialize the Symfony
  JSON-array format stored in the `roles` column.
- `setRolesJson(String)` — sets the raw JSON string directly (used only by
  the repository mapper).
- `setId(Long)` / `setCreatedAt(LocalDateTime)` — needed by the repository
  after INSERT returns a generated key.

#### `LearningStats.java`

| Field | Java type | DB column |
|-------|-----------|-----------|
| `id` | `Long` | `id` BIGINT AUTO |
| `user` | `User` | `user_id` FK |
| `totalXP` | `int` | `total_xp` |
| `wordsLearned` | `int` | `words_learned` |
| `totalMinutesStudied` | `int` | `total_minutes_studied` |
| `lastStudySession` | `LocalDateTime` | `last_study_session` |

---

### `repository/` Package

#### `UserRepository.java`
**All database access for users and learning stats.**
Uses only `java.sql.PreparedStatement` and `java.sql.ResultSet`.
Obtains its connection from `MyDataBase.getInstance().getConnection()`.

| Method | SQL operation | Notes |
|--------|--------------|-------|
| `findById(Long)` | `SELECT … JOIN … WHERE u.id = ?` | Returns `Optional<User>` |
| `findByEmail(String)` | `SELECT … JOIN … WHERE u.email = ?` | Returns `Optional<User>` |
| `findAll()` | `SELECT … ORDER BY created_at DESC` | Full list |
| `search(String)` | `WHERE LOWER(email/first/last) LIKE ?` | Case-insensitive |
| `findByStatus(String)` | `WHERE u.status = ?` | |
| `findExpiredSubscriptions()` | `WHERE is_premium=1 AND expiry < NOW()` | |
| `findExpiringSubscriptions(LocalDateTime)` | `WHERE is_premium=1 AND expiry < ?` | Used by subscription checker |
| `countAll()` | `SELECT COUNT(*)` | |
| `countByStatus(String)` | `SELECT COUNT(*) WHERE status=?` | |
| `countPremium()` | `SELECT COUNT(*) WHERE is_premium=1` | |
| `register(User)` | duplicate check + `save()` | Throws `IllegalArgumentException` on duplicate email |
| `save(User)` | INSERT or UPDATE | Branches on `user.getId() == null` |
| `saveWithStats(User, LearningStats)` | INSERT user + INSERT stats | Single `setAutoCommit(false)` transaction |
| `saveLearningStats(LearningStats)` | INSERT or UPDATE learning_stats | Branches on `stats.getId() == null` |
| `delete(User)` | DELETE learning_stats + DELETE users | Single transaction |
| `findAdvanced(…)` | Dynamic SQL with LIKE / WHERE / ORDER BY / LIMIT OFFSET | 7-param overload (with role + sort) |
| `countAdvanced(…)` | Same filters, `COUNT(*)` only | Used for pagination |

**Internal helpers:**
- `mapUser(ResultSet)` — maps all `users` columns + LEFT JOIN `learning_stats`
  columns into a `User` (and nested `LearningStats`) POJO.
- `setTs(PreparedStatement, int, LocalDateTime)` — null-safe
  `LocalDateTime → Timestamp` helper.
- `bindParams(PreparedStatement, List<Object>)` — binds a dynamic parameter
  list for advanced search queries.

---

### `service/` Package

#### `IUserService.java`
Interface defining the public contract:
```java
void registerUser(String firstName, String lastName, String email, String password);
Optional<User> login(String email, String password);
List<User> getAllUsers();
```

#### `UserService.java`
Implements `IUserService`. All business logic lives here.

**Constructors:**
- `UserService()` — primary constructor; creates a `UserRepository`.
- `UserService(Object ignoredEntityManager)` — backward-compatible overload
  called by admin controllers that pass an `EntityManager`. The argument is
  completely ignored; all DB access goes through JDBC.

**Selected method groups:**

| Group | Methods |
|-------|---------|
| Registration | `registerUser()` — validates, hashes password with BCrypt, calls `repo.register()` |
| Authentication | `authenticate(email, pw)`, `login(email, pw)` — finds user, checks status = active, verifies BCrypt hash |
| Profile | `updateName()`, `updateProfile()`, `adminUpdateUser()` |
| Status | `activateUser()`, `suspendUser()`, `deleteUser()` |
| Subscription | `upgradeToPremium()`, `downgradeToFree()`, `checkExpiredSubscriptions()` |
| Roles | `changeRoles()` — validates against allowed role set |
| Password | `hashPassword()`, `verifyPassword()`, `adminResetPassword()` |
| Stats | `initLearningStats()`, `updateLearningStats()` |
| Counts | `countAll()`, `countByStatus()`, `countPremium()`, `countAdvanced()` |

**Password compatibility:** handles Symfony's `$2y$` prefix by converting it
to `$2a$` before calling `BCrypt.checkpw()`.

#### `NotificationService.java`
Direct JDBC against the `notifications` table (no entity class).

- `sendNotification(Long userId, String type, String message)` — INSERT.
- `getRecentForUser(Long userId)` — returns up to 5 most-recent rows as
  `List<NotifRow>` (inner static DTO).
- Backward-compat constructor `NotificationService(Object)` mirrors
  `UserService`.

---

### `validation/` Package

#### `ValidationService.java`
Static utility methods for field validation (no state).

| Method | Checks |
|--------|--------|
| `requireNonBlank(value, fieldName)` | not null, not blank |
| `requireMinLength(value, fieldName, min)` | length ≥ min |
| `requireValidEmail(email)` | regex `^[\w._%+\-]+@[\w.\-]+\.[a-zA-Z]{2,}$` |
| `requirePasswordsMatch(pw, confirm)` | equality |
| `requireValidPlan(plan)` | one of `FREE`, `MONTHLY`, `YEARLY` |
| `requireValidRole(role)` | one of `ROLE_USER`, `ROLE_ADMIN`, `ROLE_TEACHER` |
| `validateOrThrow(entity)` | full Jakarta Bean Validation on any object |

All methods throw `IllegalArgumentException` with a human-readable message
on failure — caught by controllers to display inline errors.

---

### `controller/` Package

#### `LoginController.java`
- Reads email + password from `login.fxml` fields.
- Calls `new UserService().authenticate(email, password)`.
- Routes to `AdminMain.fxml` (admins) or `UserMain.fxml` (regular users) by
  checking `user.getRoles().contains("ROLE_ADMIN")`.
- `handleGoToRegister()` — uses `StageManager.switchScene("/fxml/Register.fxml")`.

#### `RegisterController.java`
- Validates all five fields inline (per-field error labels, no popups).
- Calls `new UserService().registerUser(firstName, lastName, email, password)`.
- On success navigates back to `login.fxml` via `StageManager`.
- Email-already-taken errors from the service are displayed under the email
  field; other errors go to a general error label.

#### `admin/AdminMainController.java`
Shell for the admin layout:
- Sidebar buttons load `DashboardView.fxml` or `UserListView.fxml` into a
  central `StackPane`.
- Opens modal dialogs (`UserDetailDialog`, `UserFormDialog`) via `openStage()`.
- `refreshCurrentView()` — called by child controllers after mutations.
- ⚠️ Still passes `App.getEmf()` (returns `null`) to child controllers.
  Admin dialogs will NPE at runtime until those controllers are migrated.

#### `admin/DashboardViewController.java`
Stat summary cards + recent-users table.
⚠️ **Pending JDBC migration** — still uses `emf.createEntityManager()` and
JPQL directly. Will NPE at runtime.

#### `admin/UserListController.java`
Paginated, filterable, sortable user table with bulk actions.
⚠️ **Pending JDBC migration** — uses `emf.createEntityManager()`. The
`UserService` calls inside will actually work (since `UserService(em)` ignores
the em), but the `em.close()` calls will NPE when `em` is null.

#### `admin/UserDetailController.java`
Read-only user detail popup with quick-action buttons (activate, suspend,
grant premium, send notification, edit stats).
⚠️ **Pending JDBC migration** — uses `App.getEmf()`.

#### `admin/UserFormController.java`
Create / edit user form dialog.
⚠️ **Pending JDBC migration** — uses `emf.createEntityManager()`.

#### `admin/StatsController.java`
Dialog to view and edit a user's `LearningStats` (XP, words, minutes).
Calls `svc.initLearningStats()` and `svc.updateLearningStats()` — both are
JDBC-backed and will work once the `em.close()` NPE is fixed.
⚠️ **Pending JDBC migration** — uses `emf.createEntityManager()`.

#### `admin/NotificationController.java`
Dialog to send a notification to a user.
⚠️ **Pending JDBC migration** — uses `emf.createEntityManager()`.

#### `user/UserMainController.java`
Shell for the user layout:
- Sets `SessionManager.currentUser` after login.
- Sidebar: Dashboard (placeholder) and My Profile.
- Static `refreshUserInfo()` — called by `UserProfileController` after a
  profile save to update the sidebar name/avatar without reloading the scene.
- Avatar color derived from a deterministic hash of the user's full name.

#### `user/UserProfileController.java`
Two-card profile page — fully migrated to JDBC:
- **Card 1** — display / edit mode toggle. Saves name + email via
  `UserService.updateName()` (which internally calls `UserRepository.save()`,
  persisting all fields including email in one UPDATE).
  Email uniqueness checked via `UserService.findByEmail()` before saving.
- **Card 2** — password change with strength bar, match indicator, and
  show/hide toggles. Saves via `UserService.adminResetPassword()`.

---

### `resources/fxml/`

| File | Controller | Description |
|------|-----------|-------------|
| `login.fxml` | `LoginController` | Email + password login card |
| `Register.fxml` | `RegisterController` | 5-field registration card with inline errors |
| `admin/AdminMain.fxml` | `AdminMainController` | Admin shell: sidebar + content area |
| `admin/DashboardView.fxml` | `DashboardViewController` | Stat cards + recent-users table |
| `admin/UserListView.fxml` | `UserListController` | Filterable paginated user table |
| `admin/UserDetailDialog.fxml` | `UserDetailController` | User detail popup |
| `admin/UserFormDialog.fxml` | `UserFormController` | Create / edit user form |
| `admin/StatsDialog.fxml` | `StatsController` | Edit learning stats spinners |
| `admin/NotificationDialog.fxml` | `NotificationController` | Send notification form |
| `user/UserMain.fxml` | `UserMainController` | User shell: sidebar + content area |
| `user/UserProfileView.fxml` | `UserProfileController` | Profile display + edit + password change |

### `resources/css/`

| File | Scope |
|------|-------|
| `style.css` | Global — login card, register card, shared input/button/error styles |
| `admin.css` | Admin layout — sidebar, topbar, table badges, dialog styles |
| `user.css` | User layout — profile card, password strength bar, avatar |

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                 VARCHAR(180)  NOT NULL UNIQUE,
    password              VARCHAR(255)  NOT NULL,
    roles                 JSON          NOT NULL DEFAULT ('["ROLE_USER"]'),
    first_name            VARCHAR(100)  NOT NULL,
    last_name             VARCHAR(100)  NOT NULL,
    subscription_plan     VARCHAR(20)   NOT NULL DEFAULT 'FREE',
    subscription_expiry   DATETIME      NULL,
    is_premium            TINYINT(1)    NOT NULL DEFAULT 0,
    last_payment_status   VARCHAR(50)   NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'active',
    created_at            DATETIME      NULL,
    is_verified           TINYINT(1)    NOT NULL DEFAULT 0,
    stripe_customer_id    VARCHAR(255)  NULL,
    stripe_subscription_id VARCHAR(255) NULL,
    is_banned             TINYINT(1)    NOT NULL DEFAULT 0,
    ban_reason            TEXT          NULL
);

-- Learning statistics (one-to-one with users)
CREATE TABLE learning_stats (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT        NOT NULL UNIQUE,
    total_xp              INT           NOT NULL DEFAULT 0,
    words_learned         INT           NOT NULL DEFAULT 0,
    total_minutes_studied INT           NOT NULL DEFAULT 0,
    last_study_session    DATETIME      NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Notifications
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    message     TEXT         NOT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**`is_premium` is a derived column.**
The Java code recomputes it on every load via `User.updatePremiumStatus()`:
```
isPremium = (plan == MONTHLY || plan == YEARLY)
            && subscriptionExpiry != null
            && subscriptionExpiry.isAfter(LocalDateTime.now())
```
The column is still written on every UPDATE for Symfony compatibility.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   JavaFX UI Layer                   │
│  FXML files + CSS  ←→  Controller classes           │
└────────────────────────┬────────────────────────────┘
                         │  calls
┌────────────────────────▼────────────────────────────┐
│                  Service Layer                      │
│  UserService (implements IUserService)              │
│  NotificationService                                │
│  ValidationService (static helpers)                 │
└────────────────────────┬────────────────────────────┘
                         │  calls
┌────────────────────────▼────────────────────────────┐
│                Repository Layer                     │
│  UserRepository  — PreparedStatement + ResultSet    │
└────────────────────────┬────────────────────────────┘
                         │  uses
┌────────────────────────▼────────────────────────────┐
│                  DB Utility                         │
│  MyDataBase.getInstance().getConnection()           │
│  → single java.sql.Connection to MySQL              │
└─────────────────────────────────────────────────────┘
```

**Design rules:**
- Controllers never contain SQL.
- Repositories never contain business logic.
- Services own transactions (via `Connection.setAutoCommit(false)` for
  multi-statement operations).
- Entities are plain POJOs — no framework annotations.

---

## Connection Flow

```
App.init()
  └─ MyDataBase.getInstance()
       └─ DriverManager.getConnection("jdbc:mysql://localhost:3306/1lingualearn_db", "root", "")
            └─ prints "Connexion établie !"

Controller action
  └─ new UserService()
       └─ new UserRepository()
            └─ (on each method) MyDataBase.getInstance().getConnection()
                 └─ PreparedStatement
                      └─ ResultSet → POJO mapping
```

---

## Authentication & Role Routing

```
LoginController.handleLogin()
  │
  ├─ UserService.authenticate(email, password)
  │    ├─ UserRepository.findByEmail(email)
  │    ├─ check status == "active"
  │    └─ BCrypt.checkpw(plain, hash)   ← handles Symfony $2y$ → $2a$ prefix
  │
  └─ user.getRoles().contains("ROLE_ADMIN") ?
        ├─ YES → AdminMain.fxml   (AdminMainController.setUser)
        └─ NO  → UserMain.fxml    (UserMainController.setUser
                                    → SessionManager.setCurrentUser)
```

**Roles stored** as a Symfony-compatible JSON array in the `roles` column:
`["ROLE_USER"]` or `["ROLE_USER","ROLE_ADMIN"]`.

**Session** is held in `SessionManager.currentUser` (static field).
Cleared on logout via `SessionManager.clearSession()`.

---

## Feature Status

### Fully working (JDBC)

| Feature | Entry point |
|---------|------------|
| User registration | `Register.fxml` → `RegisterController` → `UserService.registerUser()` |
| Login + role routing | `login.fxml` → `LoginController` → `UserService.authenticate()` |
| User profile — view | `UserProfileView.fxml` → `UserProfileController.populateDisplayMode()` |
| User profile — edit name + email | `UserProfileController.saveProfile()` → `UserService.updateName()` |
| User profile — change password | `UserProfileController.updatePassword()` → `UserService.adminResetPassword()` |
| Logout | `UserMainController.handleLogout()` → `SessionManager.clearSession()` |

### Pending JDBC migration (admin controllers)

These controllers compile but will throw `NullPointerException` at runtime
because `App.getEmf()` returns `null`.

| Controller | Blocker | Notes |
|-----------|---------|-------|
| `DashboardViewController` | Uses `em.createQuery()` directly | Needs conversion to `UserService` count methods |
| `UserListController` | Calls `emf.createEntityManager()` + `em.close()` | `UserService(em)` calls already work; need to remove EM wrapping |
| `UserFormController` | Calls `emf.createEntityManager()` | |
| `UserDetailController` | Calls `App.getEmf()` | |
| `StatsController` | Calls `emf.createEntityManager()` | Underlying `initLearningStats`/`updateLearningStats` already JDBC |
| `NotificationController` | Calls `emf.createEntityManager()` | `NotificationService(em)` already JDBC |

---

## How to Run

### Prerequisites
- JDK 21+
- Maven 3.8+
- MySQL 8 running locally on port 3306
- Database `1lingualearn_db` created and populated

### Start the app
```bash
mvn javafx:run
```

### Build a fat JAR
```bash
mvn package
java -jar target/lingualearn-1.0-jar-with-dependencies.jar
```

### Database connection
Credentials are hardcoded in `MyDataBase.java`:
```java
private final String URL      = "jdbc:mysql://localhost:3306/1lingualearn_db";
private final String USER     = "root";
private final String PASSWORD = "";
```
Change these directly in the file if your MySQL setup differs.
