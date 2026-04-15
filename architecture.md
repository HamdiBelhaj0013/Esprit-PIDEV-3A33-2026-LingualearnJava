# LingualearnJava — Architecture Reference

## What the app is

A desktop JavaFX application that mirrors the Symfony LinguaLearn web app.
It connects to the same MySQL database (`1lingualearn_db`) using raw JDBC — no ORM, no Hibernate.
Built with Maven. Entry point: `App.java` (extends `javafx.application.Application`).

Two user roles drive the entire UI split:
- `ROLE_USER` → lands on `UserMain.fxml` (sidebar shell with dashboard, profile, notifications)
- `ROLE_ADMIN` → lands on `AdminMain.fxml` (sidebar shell with dashboard, user management)

---

## Layer map

```
App.java                        ← JavaFX bootstrap, opens DB connection
│
├── util/
│   ├── MyDataBase              ← Singleton JDBC Connection (one conn for the whole app)
│   ├── SessionManager          ← Static field: currentUser (set on login, cleared on logout)
│   └── StageManager            ← Centralises Scene switching (used by RegisterController)
│
├── entity/
│   ├── User                    ← POJO: all user fields + roles JSON parser + isPremium logic
│   └── LearningStats           ← POJO: XP, words, minutes, last session; back-ref to User
│
├── repository/
│   └── UserRepository          ← All SQL. SELECT_BASE joins users LEFT JOIN learning_stats.
│                                  mapUser(ResultSet) builds User + LearningStats in one pass.
│
├── service/
│   ├── UserService             ← Business logic on top of UserRepository (validation, BCrypt, roles)
│   ├── NotificationService     ← INSERT and SELECT on `notifications` table; returns NotifRow DTOs
│   └── validation/
│       └── ValidationService   ← Static guard methods (requireNonBlank, requireValidEmail, etc.)
│                                  + Jakarta Bean Validation runner (validateOrThrow)
│
├── controller/
│   ├── LoginController         ← Reads email+password → UserService.authenticate() → routes to shell
│   ├── RegisterController      ← Calls UserService.registerUser()
│   ├── admin/
│   │   ├── AdminMainController     ← Shell: StackPane contentArea, loads child FXMLs into it
│   │   ├── DashboardViewController ← Admin stat cards (countAll, countPremium, etc.)
│   │   ├── UserListController      ← Paginated table with filters, bulk actions, per-row actions
│   │   ├── UserDetailController    ← Read-only popup: ban, activate, suspend, grant/revoke premium
│   │   ├── UserFormController      ← Create / edit user form (shared FXML, two modes)
│   │   ├── NotificationController  ← Send notification to a user
│   │   └── StatsController         ← Learning stats for admin view
│   └── user/
│       ├── UserMainController      ← Shell: StackPane contentArea, sidebar nav, SessionManager
│       ├── UserDashboardController ← Welcome header, stat cards, notifications, subscription card
│       └── UserProfileController   ← Edit name, change password
│
└── resources/
    ├── fxml/
    │   ├── login.fxml / Register.fxml
    │   ├── admin/  AdminMain.fxml, DashboardView.fxml, UserListView.fxml, …
    │   └── user/   UserMain.fxml, UserDashboard.fxml, UserProfileView.fxml
    └── css/
        ├── admin.css
        └── user.css
```

---

## Key architectural patterns

### 1. No dependency injection — everything is `new`

Controllers instantiate services directly:
```java
// In UserListController.refresh():
UserService svc = new UserService();
List<User> users = svc.findAdvanced(search, status, role, plan, sort, page, PAGE_SIZE);
```
Services instantiate repositories in their constructor:
```java
// In UserService constructor:
this.userRepository = new UserRepository();
this.validation     = new ValidationService();
```
There is no Spring, Guice, or any IoC container.

### 2. Singleton JDBC connection

One `Connection` object lives for the entire app lifetime.
Every repository method calls `MyDataBase.getInstance().getConnection()` to get it.
```java
// UserRepository — every query does this:
private Connection conn() {
    return MyDataBase.getInstance().getConnection();
}
```
This is safe because JavaFX runs on a single UI thread and there is no background threading.

### 3. Shell controller + child view pattern

Both `AdminMainController` and `UserMainController` own a `StackPane contentArea`.
Sidebar buttons replace the content of that pane:
```java
// AdminMainController.showUsers():
private void loadView(String fxmlPath, Consumer<Object> setup) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    Node view = loader.load();
    setup.accept(loader.getController());   // inject parent ref + call load()
    contentArea.getChildren().setAll(view); // swap the content area
}
```
Child controllers receive a reference to the shell so they can trigger navigation:
```java
// UserDashboardController calling back to the shell:
@FXML private void onViewAllNotifications() {
    if (parentController != null) parentController.navigateToNotifications();
}
```

### 4. Auth flow — login to UI in 5 steps

```
LoginController.handleLogin()
  → UserService.authenticate(email, password)
      → UserRepository.findByEmail(email)        // SQL: SELECT + JOIN
      → filter: status == "active"
      → filter: BCrypt.checkpw(plain, hash)      // handles Symfony $2y$ → $2a$ prefix
  → user.getRoles().contains("ROLE_ADMIN") ?
      AdminMain.fxml → AdminMainController.setUser(user)
    :
      UserMain.fxml  → UserMainController.setUser(user)
                           → SessionManager.setCurrentUser(user)
                           → showDashboard(null)  ← first page rendered
```

### 5. Roles stored as Symfony JSON

The `roles` column is a VARCHAR containing `["ROLE_USER","ROLE_ADMIN"]`.
`User` parses and serialises it manually (no Jackson):
```java
// User.getRoles() — parses ["ROLE_USER","ROLE_ADMIN"]:
String t = roles.trim().substring(1, roles.length() - 1); // strip [ ]
for (String part : t.split(","))
    result.add(part.trim().replace("\"", ""));             // strip quotes

// User.setRoles(List) — serialises back:
"[\"ROLE_USER\",\"ROLE_ADMIN\"]"
```

### 6. FXML loading sequence

JavaFX FXML load order:
1. `FXMLLoader.load()` instantiates the controller class from `fx:controller`
2. `@FXML` fields are injected (matched by `fx:id`)
3. `initialize()` is called
4. Caller gets controller via `loader.getController()`
5. Caller calls setup methods (`setUser()`, `setParentController()`, `loadData()`)

### 7. Manual JDBC transactions

Operations that touch multiple tables use `setAutoCommit(false)`:
```java
// UserRepository.delete(user) — deletes stats row first, then user:
c.setAutoCommit(false);
DELETE FROM learning_stats WHERE user_id = ?
DELETE FROM users WHERE id = ?
c.commit();
// on error: c.rollback()

// UserRepository.saveWithStats(user, stats) — atomic insert of both rows:
c.setAutoCommit(false);
insert(user);            // sets user.id from RETURN_GENERATED_KEYS
insertLearningStats(stats);
c.commit();
```

---

## Annotated function examples

### A — Full login call chain

```java
// LoginController.java
private void handleLogin(ActionEvent event) {
    String email    = emailField.getText().trim();
    String password = passwordField.getText();

    var userOpt = new UserService().authenticate(email, password);
    // ↑ returns Optional<User>: empty if wrong password or suspended

    if (userOpt.isEmpty()) { showError("Invalid email or password."); return; }

    User user       = userOpt.get();
    boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
    navigateToDashboard(user, isAdmin);
}

// UserService.java
public Optional<User> authenticate(String email, String plainPassword) {
    return userRepository.findByEmail(email.trim().toLowerCase())
            .filter(u -> "active".equals(u.getStatus()))
            .filter(u -> verifyPassword(plainPassword, u.getPassword()));
    // Two .filter() calls chain as AND conditions on the Optional
}

public boolean verifyPassword(String plain, String hash) {
    if (hash != null && hash.startsWith("$2y$"))
        hash = "$2a$" + hash.substring(4); // Symfony uses $2y$, BCrypt lib uses $2a$
    return BCrypt.checkpw(plain, hash);
}
```

### B — How a user row becomes a Java object

```java
// UserRepository.java — SELECT_BASE does one SQL call that joins both tables:
private static final String SELECT_BASE =
    "SELECT u.id, u.email, ..., " +
    "ls.id AS ls_id, ls.total_xp, ls.words_learned, ... " +
    "FROM users u LEFT JOIN learning_stats ls ON ls.user_id = u.id";

private User mapUser(ResultSet rs) throws SQLException {
    User u = new User();
    u.setId(rs.getLong("id"));
    // NOTE: expiry must be set BEFORE plan because setSubscriptionPlan()
    // calls updatePremiumStatus() which reads the expiry field:
    u.setSubscriptionExpiry(expiry != null ? expiry.toLocalDateTime() : null);
    u.setSubscriptionPlan(plan != null ? plan : "FREE"); // ← triggers isPremium calc

    long lsId = rs.getLong("ls_id");
    if (!rs.wasNull()) {           // LEFT JOIN — if no stats row, lsId is SQL NULL
        LearningStats ls = new LearningStats();
        ls.setTotalXP(rs.getInt("total_xp"));
        u.setLearningStats(ls);    // ← also sets ls.user = u via the setter
    }
    return u;
}
```

### C — Dynamic filtered query building

```java
// UserRepository.findAdvanced() — builds WHERE clause dynamically:
StringBuilder sql = new StringBuilder(SELECT_BASE + " WHERE 1=1");
List<Object> params = new ArrayList<>();

if (search != null && !search.isBlank()) {
    sql.append(" AND (LOWER(u.email) LIKE ? OR LOWER(u.first_name) LIKE ? ...)");
    params.add("%" + search.toLowerCase() + "%");  // same value bound 3 times
}
if (status != null && !status.isBlank()) {
    sql.append(" AND u.status = ?");
    params.add(status);
}
// ... role, plan filters same pattern ...
sql.append(" LIMIT ? OFFSET ?");
params.add(pageSize);
params.add((page - 1) * pageSize);

// bindParams() then iterates params and calls ps.setString/setInt/setLong
// based on runtime type of each Object in the list
```

### D — Notification send and receive

```java
// Sending (admin action or system event):
new NotificationService().sendNotification(
    user.getId(), "WELCOME", "Your account has been created.");
// → INSERT INTO notifications (user_id, type, message, is_read, created_at) VALUES (?,?,?,0,?)

// Receiving (UserDashboardController.loadNotifications):
List<NotificationService.NotifRow> notifs =
    new NotificationService().getRecentForUser(user.getId());
// → SELECT id, type, message, is_read, created_at
//   FROM notifications WHERE user_id=? ORDER BY created_at DESC LIMIT 5

for (NotifRow n : notifs) {
    // n.type, n.message, n.isRead, n.createdAt are plain public fields
    // (NotifRow is a static inner class — just a data bag, no getters)
}
```

### E — Premium upgrade (multi-setter side effects)

```java
// UserService.upgradeToPremium():
public void upgradeToPremium(User user, String plan, LocalDateTime expiry) {
    ValidationService.requireValidPlan(plan); // throws if not FREE/MONTHLY/YEARLY
    user.setSubscriptionExpiry(expiry);       // stores expiry, calls updatePremiumStatus()
    user.setSubscriptionPlan(plan);           // stores plan,   calls updatePremiumStatus()
    user.setLastPaymentStatus("success");
    saveAndFlush(user);                       // → userRepository.save(user) → UPDATE users SET ...
}

// User.updatePremiumStatus() — called automatically by both setters above:
public void updatePremiumStatus() {
    this.isPremium =
        ("MONTHLY".equals(subscriptionPlan) || "YEARLY".equals(subscriptionPlan))
        && subscriptionExpiry != null
        && subscriptionExpiry.isAfter(LocalDateTime.now());
}
// isPremium is a derived field — it's never set directly, always computed.
```

### F — Admin shell opening a modal dialog

```java
// AdminMainController.openUserDetail(user):
private void openStage(String fxmlPath, String title,
                       double minW, double minH, Consumer<Object> setup) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
    Parent root = loader.load();
    setup.accept(loader.getController());   // inject the controller deps

    Stage stage = new Stage();
    stage.initOwner(contentArea.getScene().getWindow()); // child of main window
    stage.initModality(Modality.APPLICATION_MODAL);      // blocks main window
    stage.setScene(new Scene(root));
    stage.showAndWait();  // blocks until dialog is closed
}
// Usage at call site:
mainController.openUserDetail(user);
// which calls: openStage(..., ctrl -> { if (ctrl instanceof UserDetailController c) c.setUser(user); })
```

---

## Database tables (relevant to user module)

| Table | Key columns |
|---|---|
| `users` | id, email, password, roles (JSON), first_name, last_name, status, subscription_plan, subscription_expiry, is_premium, is_banned, ban_reason, created_at |
| `learning_stats` | id, user_id (FK), total_xp, words_learned, total_minutes_studied, last_study_session |
| `notifications` | id, user_id (FK), type, message, is_read, created_at |

`UserRepository.SELECT_BASE` always does `LEFT JOIN learning_stats` so a single query
populates both `User` and its embedded `LearningStats`.

---

## What does NOT exist yet

- No background threads / Task / Service wrappers — all DB calls are synchronous on the UI thread
- No caching — every controller call hits the DB fresh
- No event bus — parent/child communication is done via direct method references passed at load time
- No module system beyond the two user types (admin, user)
