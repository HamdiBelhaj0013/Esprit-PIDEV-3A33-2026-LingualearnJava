# LinguaLearn - Quiz & Exercise Manager

A JavaFX desktop application for managing quizzes and exercises, built with Java 17 and MySQL.

## Features

- **Quiz Management** – Create, edit, delete, and play quizzes with multiple difficulty levels
- **Exercise Management** – Full CRUD operations for exercises linked to quizzes
- **AI Feedback** – Gemini API integration for personalized quiz result analysis
- **Stripe Payments** – Premium quiz access (levels 4-5) with Stripe checkout
- **Email Notifications** – Automatic confirmation emails after payments
- **Star Ratings** – Interactive 5-star rating system for exercises
- **Role-Based Access** – Admin dashboard and read-only client portal
- **Modern UI** – Card-based grid layout, sidebar navigation, and custom CSS styling

## Tech Stack

| Layer        | Technology                     |
|--------------|--------------------------------|
| Language     | Java 17                        |
| UI Framework | JavaFX 23 + FXML               |
| Database     | MySQL (via XAMPP)               |
| Build Tool   | Maven                          |
| Icons        | Ikonli FontAwesome 5            |
| Payments     | Stripe Java SDK                 |
| Email        | Jakarta Mail                    |
| Auth         | jBCrypt                         |
| AI           | Google Gemini API               |

## Project Structure

```
src/main/java/org/example/
├── Main.java                   # App entry point
├── MainLauncher.java           # Launcher wrapper
├── controller/                 # JavaFX controllers
│   ├── LoginController         # Authentication
│   ├── QuizController          # Quiz grid view
│   ├── QuizFormController      # Quiz create/edit
│   ├── QuizPlayController      # Quiz execution
│   ├── QuizResultController    # Results + AI feedback
│   ├── ExerciceController      # Exercise grid view
│   ├── ExerciceFormController  # Exercise create/edit
│   ├── SidebarController       # Navigation sidebar
│   └── StatsController         # Statistics dashboard
├── entities/                   # Data models
│   ├── Quiz, Exercice, Lesson, Rating, User
├── service/                    # Business logic & DB access
│   ├── QuizService, ExerciceService, LessonService
│   ├── UserService, RatingService
│   ├── StripeService, EmailService, GeminiService
└── util/
    └── UserSession             # Session management
```

---

## 💳 Stripe – Paiement Premium

Les quiz de **niveau 4** (10$) et **niveau 5** (25$) sont considérés comme du contenu premium.  
Lorsqu'un utilisateur sélectionne un quiz premium, l'application :

1. Vérifie si l'utilisateur a déjà un accès premium en base de données
2. Affiche une fenêtre de confirmation avec le prix
3. Crée une **session Stripe Checkout** via `StripeService.createCheckoutSession()`
4. Redirige l'utilisateur vers la page de paiement sécurisée Stripe dans le navigateur
5. Après paiement réussi, met à jour le statut premium de l'utilisateur en BDD

**Fichiers concernés :**
- `StripeService.java` – Création de la session Checkout via le SDK Stripe
- `QuizDetailController.java` – Logique de vérification et déclenchement du paiement

---

## 📧 Email – Confirmation de Paiement

Après chaque paiement Stripe réussi, un **email de confirmation HTML** est envoyé automatiquement à l'adresse de l'utilisateur connecté.

L'email contient :
- Le nom du quiz acheté
- Le montant payé
- Le statut de la transaction
- Un design professionnel avec gradient et mise en page responsive

**Implémentation :**
- Protocole **SMTP** via Gmail (port 587, TLS activé)
- Bibliothèque **Jakarta Mail**
- Le contenu est un template HTML inline avec CSS

**Fichier concerné :** `EmailService.java` → méthode `sendPaymentConfirmation()`

---

## ⭐ Rating – Système de Notation

Chaque exercice peut être noté par les utilisateurs via un **système interactif de 5 étoiles**.

Fonctionnement :
- L'utilisateur clique sur une étoile (1 à 5) pour attribuer sa note
- Si l'utilisateur a déjà noté l'exercice, sa note est **mise à jour** (`ON DUPLICATE KEY UPDATE`)
- La **moyenne des notes** est calculée et affichée sur la carte de l'exercice
- Chaque couple `(user_id, exercice_id)` est unique en base (contrainte `UNIQUE KEY`)

**Table MySQL :** `rating_exercice` (id, user_id, exercice_id, value)

**Fichiers concernés :**
- `RatingService.java` – CRUD des notes (ajout/mise à jour, moyenne, note utilisateur)
- `ExerciceCardController.java` – Affichage des étoiles interactives dans l'UI
- `Rating.java` – Entité de données

---

## 🤖 Gemini AI – Feedback Intelligent

L'application intègre l'**API Google Gemini** (modèle `gemini-2.5-flash`) pour fournir un feedback personnalisé après chaque quiz.

### Deux types de feedback :

**1. Explication par question** (`getAnswerExplanation`)
- Pour chaque question, l'IA génère une explication de 2-3 phrases
- Si la réponse est correcte → encouragement
- Si la réponse est incorrecte → explication de l'erreur avec la bonne réponse

**2. Résumé global du quiz** (`getQuizSummary`)
- Analyse de la performance globale (score X/Y)
- Conseil personnalisé pour s'améliorer
- Ton encourageant adapté à l'apprentissage des langues

### Implémentation technique :
- Appels HTTP via `java.net.http.HttpClient` (pas de dépendance externe)
- Parsing JSON manuel de la réponse Gemini
- Timeout de 30 secondes par requête
- Gestion d'erreurs avec messages de fallback

**Fichiers concernés :**
- `GeminiService.java` – Appels API et construction des prompts
- `QuizResultController.java` – Affichage du feedback dans l'interface des résultats

---

## Prerequisites

- **Java 17+**
- **Maven**
- **MySQL** (via XAMPP or standalone)
- Database: `lingualearn_db` (SQL file included: `1lingualearn_db.sql`)

## Setup

1. **Clone the repo**
   ```bash
   git clone <repo-url>
   cd CrudExerciceQuiz
   ```

2. **Import the database**
   ```bash
   mysql -u root < 1lingualearn_db.sql
   ```

3. **Run the app**
   ```bash
   mvn javafx:run
   ```

## Authors

- ESPRIT – PIDEV 3A33 2026
