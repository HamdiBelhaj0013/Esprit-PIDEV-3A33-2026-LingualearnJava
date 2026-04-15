# LinguaLearn — Forum Module (Frontoffice + Backoffice)

## Overview

LinguaLearn is a language learning platform. This module covers the **Forum** feature, including Publications, Stories, Comments, Likes/Dislikes, Notifications, and a full Backoffice dashboard — built with **JavaFX + JDBC (MySQL)**.

---

## Features

### Frontoffice
- 📰 **Publications feed** — only `post` type publications (stories are excluded)
- 📖 **Stories** — horizontal story strip with viewer, navigation, like/dislike
- 👍👎 **Like / Dislike** — exclusive toggle, ratio bar, real-time counter update
- 🔔 **Notifications** — triggered on like/dislike actions
- 💬 **Comments** — inline comment section per publication
- 🔍 **Search & Sort** — search by title/content, sort by date
- ➕ **Add / Edit / Delete** publications and stories

### Backoffice
- 🃏 **Publication card grid** — image banner, stats chips, inline actions
- 💬 **Comment manager** — table view with filters and pagination
- 📊 **KPI dashboard** — total publications, likes, reports, types
- 🔎 **Filters** — search by title, type, min likes
- 📄 **Pagination** — configurable cards/page

---

## Entry Points

| App | Class |
|-----|-------|
| Frontoffice | `MainApp` |
| Backoffice  | `MainApp` → Admin Dashboard |

---

## Run

```powershell
Set-Location "C:\pi\asma_pi\Esprit-PIDEV-3A33-2026-LingualearnJava"
mvn -DskipTests compile
mvn javafx:run
```

---

## Database

Connection configured in `src/main/java/utils/DBConnection.java`:

| Property | Value |
|----------|-------|
| URL      | `jdbc:mysql://localhost:3306/lingualearn` |
| User     | `root` |
| Password | *(empty)* |

### Required tables
- `publication` — `id`, `titre_pub`, `type_pub`, `lien_pub`, `contenu_pub`, `date_pub`, `likes`, `dislikes`, `report_pub`, `user_id`
- `commentaire` — `id`, `contenu`, `publication_id`

> If the DB is unavailable, the app starts but CRUD actions will show errors.

---

## Project Structure

```
src/main/
├── java/
│   ├── MainApp.java
│   └── org/example/
│       ├── controllers/
│       │   ├── frontoffice/   # PublicationController, CommentaireController, ...
│       │   └── backoffice/    # PublicationBackofficeController, CommentaireBackofficeController, ...
│       ├── entities/          # Publication, Commentaire, ...
│       ├── services/          # ServicePublication, ServiceCommentaire, NotificationManager
│       └── interfaces/        # IServices
└── resources/
    ├── frontoffice/fxml/      # PublicationView, MainView, AjouterPublicationView, ...
    ├── backoffice/fxml/       # publication_manager, commentaire_manager, dashboard, ...
    └── fxml/                  # login, AdminDashboard, UserDashboard
```
