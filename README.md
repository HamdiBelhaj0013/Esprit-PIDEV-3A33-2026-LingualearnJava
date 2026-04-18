# 🌐 LinguaLearn — JavaFX Desktop Application

> **Projet PIDEV 3A33 — Esprit School of Engineering 2026**  
> Équipe : Lingualearn Java | Branche : `forum`

---

## 📋 Description

LinguaLearn est une application desktop JavaFX permettant aux apprenants de langues de partager des publications, interagir via des commentaires, et bénéficier d'outils IA intégrés pour une expérience communautaire enrichie.

---

## ✨ Fonctionnalités

### 📰 Publications (Front Office)
- ✅ Affichage en **cartes modernes** avec images, likes/dislikes et ratio visuel
- ✅ **Séparation Publications / Stories** : les stories restent dans la section dédiée, les posts dans le feed principal
- ✅ Tri croissant/décroissant par date
- ✅ Recherche en temps réel
- ✅ Pagination (5 publications par page)
- ✅ Ajout / Modification / Suppression de publications
- ✅ Système de **Like & Dislike** avec barre de ratio

### 📖 Stories
- ✅ Affichage circulaire style Instagram
- ✅ Navigation entre stories (← →)
- ✅ Ajout/modification/suppression d'une story
- ✅ Likes/dislikes sur les stories

### 💬 Commentaires dynamiques
- ✅ Section commentaires **inline** (toggle visible/masqué)
- ✅ Ajout de commentaire avec touche **Entrée** ou bouton Envoyer
- ✅ Modification et suppression de commentaires
- ✅ **🛡️ Détection automatique de mots inappropriés** : le commentaire est bloqué et un email d'alerte est envoyé à `saaharhamraoui@gmail.com`

### 🔔 Notifications (style Facebook)
- ✅ Panel dropdown animé (ouverture/fermeture fluide)
- ✅ Badge rouge avec compteur de non-lus
- ✅ Toast notification (coin haut-droit, disparaît après 3s)
- ✅ "Tout marquer comme lu"
- ✅ **Bug fix** : le gestionnaire de clic extérieur est maintenant correctement désabonné (référence stable)

### 🤖 Chatbot IA — LinguaBot
- ✅ Widget **flottant** en bas à droite (style Facebook Messenger)
- ✅ Propulsé par l'API **GROQ** (LLaMA 3.3 70B)
- ✅ Bulles de chat avec avatar, historique visuel
- ✅ Indicateur "en train d'écrire..."
- ✅ Réponses en français, conseils sur l'apprentissage des langues

### ✨ Amélioration IA des publications
- ✅ Bouton **"✨ Améliorer avec l'IA"** dans le formulaire d'ajout
- ✅ Améliore automatiquement le titre et le contenu via **GROQ API**
- ✅ Résultat injecté directement dans les champs

### 🛡️ Modération — Bad Word Checker
- ✅ Vérification automatique à chaque soumission de commentaire
- ✅ Commentaire bloqué si mots inappropriés détectés
- ✅ **Email d'alerte automatique** envoyé à `saaharhamraoui@gmail.com`
- ✅ Liste de mots en français et en anglais

---

## 🏗️ Architecture

```
src/main/java/org/example/
├── controllers/frontoffice/
│   ├── MainController.java               # Navbar + notifications dropdown (bug fix)
│   ├── PublicationController.java        # Feed publications + stories
│   ├── CommentaireController.java        # Commentaires inline + bad word check
│   ├── ChatbotController.java            # Widget chatbot IA flottant  ← NEW
│   └── AjouterPublicationController.java # + bouton amélioration IA   ← UPDATED
├── services/
│   ├── BadWordChecker.java               # Détecteur mots inappropriés ← NEW
│   ├── EmailService.java                 # Envoi emails SMTP Gmail      ← NEW
│   ├── GroqService.java                  # Client API GROQ (LLaMA 3)   ← NEW
│   ├── ServicePublication.java
│   ├── ServiceCommentaire.java
│   └── NotificationManager.java
└── entities/
    ├── Publication.java
    ├── Commentaire.java
    └── Notification.java
```

---

## 🚀 Démarrage rapide

```bash
git clone <repo-url>
cd Esprit-PIDEV-3A33-2026-LingualearnJava
git checkout forum
mvn clean install
mvn javafx:run
```

---

## 🔑 APIs utilisées

| Service | Usage |
|---------|-------|
| **GROQ API** (LLaMA 3.3 70B) | Chatbot LinguaBot + amélioration de publications |
| **Gmail SMTP** | Emails de modération bad words |

---

## 👥 Équipe — PIDEV 3A33 — Esprit 2025/2026
