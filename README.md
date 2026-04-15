# Projet CrudExerciceQuiz - LinguaLearn

Ce projet est une application de bureau de gestion de Quizz et d'Exercices en JavaFX (conçue principalement pour une plateforme d'apprentissage linguistique "LinguaLearn"). Il fournit une interface d'administration interactive et moderne ainsi qu'un portail client. L'application est basée sur une architecture robuste MVC et est connectée à une base de données MySQL.

## 🏗️ Architecture du Projet

Le projet suit une architecture multicouche avec une implémentation MVC (Modèle-Vue-Contrôleur) intégrant un pattern de Services pour la base de données :

1.  **Couche Modèle (Entités)** : Modélise les données de l'application (`Exercice`, `Quiz`, `Lesson`) pour manipuler les données en mémoire.
2.  **Couche Accès aux Données (Services)** : Les classes communicant via JDBC (`DatabaseConnection`) à la base de données MySQL. Les opérations CRUD directes y sont effectuées.
3.  **Couche Contrôleur (Controllers)** : Fait le lien systématique entre la logique de la base de données et l'interaction utilisateur. Capte les événements, vérifie l'autorisation des utilisateurs et met à jour dynamiquement la vue.
4.  **Couche Vues (Resources / FXML)** : Composée de fichiers FXML pour sculpter l'architecture visuelle et stylisée en de multiples composants UI (Barre latérale, Cartes dynamiques, Tableaux de bord, etc.) avec du pur CSS JavaFX.

## 🚀 Fonctionnalités & Implémentation au Fichier

Voici le détail complet des fonctionnalités de l'application et les fichiers correspondants qui s'en chargent.

### 1. Authentification & Contrôle d'accès (RBAC)
Le système permet de s'identifier soit en tant qu'Administrateur, soit en tant que Client. Selon le rôle, l'interface s'adapte, autorisant des actions globales ou cachant des boutons et restreignant l'application en "Lecture seule".
*   **Contrôleur** : `src/main/java/org/example/controllers/LoginController.java`
*   **Vues** : `LoginView.fxml`, `ClientView.fxml`
*   **Utilitaire** : `src/main/java/org/example/util/UserSession.java` (maintien en mémoire de l'accessibilité).

### 2. Tableau de Bord et Menu de Navigation Globale (Dashboard)
Un tableau de bord unifié offrant avec une barre latérale ("Sidebar") présente sur tout le long de l'expérience d'administration. Affiche des statistiques rapides avec un affichage esthétique et des icônes d'interface.
*   **Contrôleurs** : `SidebarController.java`, `StatsController.java`
*   **Vues** : `Mainview.fxml`, `Sidebar.fxml`, `StatsView.fxml`

### 3. Gestion Complète des Exercices (CRUD)
Affiche les exercices sous forme de cartes structurées (GridView). L'admin a la capacité d'Ajouter, Consulter en détail, Modifier, ou Supprimer des exercices en passant par un formulaire sécurisé (avec validation des données).
*   **Entité / Service** : `Exercice.java`, `ExerciceService.java`
*   **Contrôleurs de liste** : `ExerciceController.java`
*   **Contrôleur du formulaire** : `ExerciceFormController.java`
*   **Contrôleur de l'élément (composant UI)** : `ExerciceCardController.java`
*   **Vues liées** : `ExerciceView.fxml`, `ExerciceForm.fxml`, `ExerciceCard.fxml`, `ExerciceDetailView.fxml`

### 4. Gestion Complète des Quizz (CRUD)
Un système puissant de conception de quiz, qui associe un quiz à un titre, des questions ou diverses règles. Liste navigable sous forme de cartes.
*   **Entité / Service** : `Quiz.java`, `QuizService.java`
*   **Contrôleurs de liste** : `QuizController.java`
*   **Contrôleur du formulaire** : `QuizFormController.java`
*   **Contrôleur de l'élément (composant UI)** : `QuizCardController.java`
*   **Vues liées** : `QuizView.fxml`, `QuizForm.fxml`, `QuizCard.fxml`

### 5. Intégration et Liaison aux Leçons
Les Quiz peuvent être formellement attachés à des règles et des cours d'une base de données de leçons. Récupérés en BDD et attachés via les ComboBox des formulaires de Quiz.
*   **Entité / Service** : `Lesson.java`, `LessonService.java`

### 6. Mode Exécution : Jouabilité & Détails des Quiz
Les utilisateurs peuvent entrer dans un quiz configuré, voir le résumé en détail du quiz sélectionné puis lancer un mode "Jouer au Quiz", avec champ de réponses ou options associées pour son exécution en temps réel.
*   **Contrôleur** : `QuizDetailController.java`, `QuizPlayController.java`
*   **Vues** : `QuizDetailView.fxml`, `QuizPlayView.fxml`

### 7. Base de Données, Maven & Lancement
Gestion intégrée sous Maven du lancement JavaFX (évitant les erreurs de modules d'environnement lors de l'exports de jar) et de la connexion à la base de données locale (XAMPP).
*   **Moteurs & Lancement** : `Main.java`, `MainLauncher.java`
*   **Outils BDD** : `DatabaseConnection.java`, `1lingualearn_db.sql` (Script original d'export de la table et des données).
*   **Configuration Maven** : `pom.xml` (Rapatrie MySQL-Connector, les dépendances OpenJFX, et Ikonli/FontAwesome pour l'iconographie de l'UI).

---
> **Auteurs & Dépendances :** Conçu avec Java JDK 17+, Maven, OpenJFX (v23) et MySQL 8.
