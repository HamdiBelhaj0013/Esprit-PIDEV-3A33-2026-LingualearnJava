-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : mer. 15 avr. 2026 à 14:57
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `lingualearn`
--

-- --------------------------------------------------------

--
-- Structure de la table `certificate`
--

CREATE TABLE `certificate` (
  `id` int(11) NOT NULL,
  `avg_score` double NOT NULL,
  `unique_code` varchar(36) NOT NULL,
  `issued_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL,
  `platform_language_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `certificate`
--

INSERT INTO `certificate` (`id`, `avg_score`, `unique_code`, `issued_at`, `user_id`, `platform_language_id`) VALUES
(1, 18.13, 'd1ad9191-0013-4d7b-bfaf-70dce2c7459c', '2026-02-26 09:15:50', 124, 2);

-- --------------------------------------------------------

--
-- Structure de la table `commentaire`
--

CREATE TABLE `commentaire` (
  `id` int(11) NOT NULL,
  `contenu_c` longtext NOT NULL,
  `date_com` datetime DEFAULT NULL,
  `publication_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `course`
--

CREATE TABLE `course` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `level` varchar(50) NOT NULL,
  `status` varchar(50) NOT NULL,
  `published_at` datetime NOT NULL,
  `author_id` int(11) DEFAULT NULL,
  `platform_language_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `course`
--

INSERT INTO `course` (`id`, `title`, `level`, `status`, `published_at`, `author_id`, `platform_language_id`) VALUES
(1, 'debutant', 'beginner', 'published', '2026-02-25 15:25:01', 1, 1),
(2, 'cours debutant', 'beginner', 'published', '2026-02-26 08:40:24', 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260220020333', '2026-02-25 15:20:23', 840),
('DoctrineMigrations\\Version20260220033239', '2026-02-25 15:20:24', 182),
('DoctrineMigrations\\Version20260220100000', '2026-02-25 15:20:24', 2),
('DoctrineMigrations\\Version20260220225152', '2026-02-25 22:23:01', 0),
('DoctrineMigrations\\Version20260222094633', '2026-02-25 22:24:32', 0),
('DoctrineMigrations\\Version20260224074024', '2026-02-25 22:25:02', 0),
('DoctrineMigrations\\Version20260225142123', '2026-02-25 15:21:28', 943),
('DoctrineMigrations\\Version20260225151415', '2026-02-25 22:25:39', 0),
('DoctrineMigrations\\Version20260225190729', '2026-03-03 20:40:22', 0),
('DoctrineMigrations\\Version20260225202154', '2026-03-03 20:40:23', 602),
('DoctrineMigrations\\Version20260225204103', '2026-03-03 20:40:23', 698),
('DoctrineMigrations\\Version20260225213259', '2026-02-25 22:33:04', 870),
('DoctrineMigrations\\Version20260225214109', '2026-02-25 22:41:15', 589),
('DoctrineMigrations\\Version20260225AddEmbeddingToTestQuestion', '2026-03-04 01:41:45', 12),
('DoctrineMigrations\\Version20260226120000', '2026-03-03 20:40:24', 11),
('DoctrineMigrations\\Version20260302222936', '2026-03-02 23:29:42', 687),
('DoctrineMigrations\\Version20260303193924', '2026-03-03 20:43:31', 0);

-- --------------------------------------------------------

--
-- Structure de la table `exercice`
--

CREATE TABLE `exercice` (
  `id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `question` longtext NOT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`options`)),
  `correct_answer` varchar(255) NOT NULL,
  `ai_generated` tinyint(4) NOT NULL,
  `enabled` tinyint(4) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `skill_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '[]' CHECK (json_valid(`skill_codes`)),
  `difficulty` smallint(6) NOT NULL DEFAULT 3
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `exercice`
--

INSERT INTO `exercice` (`id`, `type`, `question`, `options`, `correct_answer`, `ai_generated`, `enabled`, `quiz_id`, `skill_codes`, `difficulty`) VALUES
(1, 'translation', 'Traduisez en français :\nI am going to school', '[\"Je vais \\u00e0 l\\u2019\\u00e9cole\",\"je suis malade\",\"bonjour\"]', 'Je vais à l’école', 0, 1, 1, '[\"grammaire\"]', 2),
(2, 'translation', 'Traduisez en français :\nShe likes to read books', '[\"Elle aime lire des livres.\",\"salut\",\"je dit nimporte quoi\"]', 'Elle aime lire des livres.', 0, 1, 1, '[\"grammaire\"]', 2),
(3, 'translation', 'Traduisez en français :\n\"We are watching a movie tonight.\"', '[\"Nous regardons un film ce soir.\",\"ouiiii\",\"je suis maryem\"]', 'Nous regardons un film ce soir.', 0, 1, 1, '[\"grammaire\"]', 2),
(4, 'translation', 'Traduisez en français :\nThey finished their homework', '[\"Ils ont termin\\u00e9 leurs devoirs.\",\"bbbbbbbbbb\",\"hhhhhhhhhhhhh\"]', 'Ils ont terminé leurs devoirs.', 0, 1, 1, '[\"grammaire\"]', 2),
(5, 'translation', 'Traduisez en français :\nMy brother works in a hospital.', '[\"Mon fr\\u00e8re travaille dans un h\\u00f4pital.\",\"kkkkkkkkkkkk\",\"llllllllllllll\"]', 'Mon frère travaille dans un hôpital.', 0, 1, 1, '[\"grammaire\"]', 2);

-- --------------------------------------------------------

--
-- Structure de la table `exercise_ai_feedback`
--

CREATE TABLE `exercise_ai_feedback` (
  `id` int(11) NOT NULL,
  `is_correct` tinyint(4) NOT NULL,
  `student_answer` longtext DEFAULT NULL,
  `correct_answer` longtext DEFAULT NULL,
  `ai_explanation` longtext DEFAULT NULL,
  `ai_correction` longtext DEFAULT NULL,
  `ai_tip` longtext DEFAULT NULL,
  `ai_example` longtext DEFAULT NULL,
  `provider` varchar(50) NOT NULL,
  `model` varchar(120) DEFAULT NULL,
  `prompt_hash` varchar(64) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL,
  `quiz_attempt_id` int(11) NOT NULL,
  `exercise_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `exercise_ai_feedback`
--

INSERT INTO `exercise_ai_feedback` (`id`, `is_correct`, `student_answer`, `correct_answer`, `ai_explanation`, `ai_correction`, `ai_tip`, `ai_example`, `provider`, `model`, `prompt_hash`, `created_at`, `updated_at`, `user_id`, `quiz_attempt_id`, `exercise_id`) VALUES
(1, 0, 'Je vais à l’école.', 'Je vais à l’école', 'L\'utilisateur a répété la bonne réponse, mais il n\'y a pas eu d\'erreur à corriger.', '', '', '', 'huggingface', 'google/flan-t5-large', '1b22f06929ce5b2ef3f9253f037839ded84ab16824f5df57d22431b1c7176f3d', '2026-03-03 23:52:49', '2026-03-03 23:52:49', 128, 7, 1),
(2, 0, 'Nous regardons un film ce soir', 'Nous regardons un film ce soir.', 'L\'utilisateur a répété la bonne réponse. La traduction correcte est déjà donnée dans la question, il n\'y a donc pas d\'erreur.', '', '', '', 'huggingface', 'google/flan-t5-large', '8cdaf40239bd15e8282c01060736b2e00e503773b58c014afdba6457b4d0028c', '2026-03-03 23:53:11', '2026-03-03 23:53:11', 128, 7, 3);

-- --------------------------------------------------------

--
-- Structure de la table `exercise_attempt`
--

CREATE TABLE `exercise_attempt` (
  `id` int(11) NOT NULL,
  `is_correct` tinyint(4) NOT NULL,
  `given_answer` longtext DEFAULT NULL,
  `points` int(11) NOT NULL,
  `created_at` datetime NOT NULL,
  `time_spent_seconds` int(11) DEFAULT NULL,
  `quiz_attempt_id` int(11) NOT NULL,
  `exercise_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `exercise_attempt`
--

INSERT INTO `exercise_attempt` (`id`, `is_correct`, `given_answer`, `points`, `created_at`, `time_spent_seconds`, `quiz_attempt_id`, `exercise_id`) VALUES
(1, 0, 'Je vais à l’école.', 0, '2026-03-03 21:31:20', NULL, 1, 1),
(2, 1, 'Elle aime lire des livres.', 1, '2026-03-03 21:31:20', NULL, 1, 2),
(3, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 21:31:20', NULL, 1, 3),
(4, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 21:31:20', NULL, 1, 4),
(5, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 21:31:20', NULL, 1, 5),
(6, 0, 'aaaaaaaaaaa', 0, '2026-03-03 21:31:32', NULL, 1, 4),
(7, 0, 'Je vais à l’école.', 0, '2026-03-03 22:32:53', NULL, 2, 1),
(8, 1, 'Elle aime lire des livres.', 1, '2026-03-03 22:32:53', NULL, 2, 2),
(9, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 22:32:53', NULL, 2, 3),
(10, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 22:32:53', NULL, 2, 4),
(11, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 22:32:53', NULL, 2, 5),
(12, 1, 'Ils ont terminé leurs devoirs.', 1, '2026-03-03 22:33:47', NULL, 2, 4),
(13, 0, 'Je vais à l’école.', 0, '2026-03-03 22:49:10', NULL, 3, 1),
(14, 1, 'Elle aime lire des livres.', 1, '2026-03-03 22:49:10', NULL, 3, 2),
(15, 0, 'zzz', 0, '2026-03-03 22:49:10', NULL, 3, 3),
(16, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 22:49:10', NULL, 3, 4),
(17, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 22:49:10', NULL, 3, 5),
(18, 0, 'Je vais à l’école.', 0, '2026-03-03 23:02:14', NULL, 4, 1),
(19, 1, 'Elle aime lire des livres.', 1, '2026-03-03 23:02:14', NULL, 4, 2),
(20, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 23:02:14', NULL, 4, 3),
(21, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 23:02:14', NULL, 4, 4),
(22, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 23:02:14', NULL, 4, 5),
(23, 0, 'Je vais à l’école.', 0, '2026-03-03 23:27:28', NULL, 5, 1),
(24, 1, 'Elle aime lire des livres.', 1, '2026-03-03 23:27:28', NULL, 5, 2),
(25, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 23:27:28', NULL, 5, 3),
(26, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 23:27:28', NULL, 5, 4),
(27, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 23:27:28', NULL, 5, 5),
(28, 0, 'Je vais à l’école.', 0, '2026-03-03 23:33:26', NULL, 6, 1),
(29, 1, 'Elle aime lire des livres.', 1, '2026-03-03 23:33:26', NULL, 6, 2),
(30, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 23:33:26', NULL, 6, 3),
(31, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 23:33:26', NULL, 6, 4),
(32, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 23:33:26', NULL, 6, 5),
(33, 0, 'Je vais à l’école.', 0, '2026-03-03 23:51:52', NULL, 7, 1),
(34, 1, 'Elle aime lire des livres.', 1, '2026-03-03 23:51:52', NULL, 7, 2),
(35, 0, 'Nous regardons un film ce soir', 0, '2026-03-03 23:51:52', NULL, 7, 3),
(36, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-03 23:51:52', NULL, 7, 4),
(37, 0, 'lllllllllllllllllllllll', 0, '2026-03-03 23:51:52', NULL, 7, 5),
(38, 0, 'Je vais à l’école.', 0, '2026-03-04 13:25:25', NULL, 8, 1),
(39, 1, 'Elle aime lire des livres.', 1, '2026-03-04 13:25:25', NULL, 8, 2),
(40, 0, 'Nous regardons un film ce soir', 0, '2026-03-04 13:25:25', NULL, 8, 3),
(41, 0, 'jjjjjjjjjjjjjjjjj', 0, '2026-03-04 13:25:25', NULL, 8, 4),
(42, 0, 'sssssssssssss', 0, '2026-03-04 13:25:25', NULL, 8, 5),
(43, 0, 'aaaaaaaaaaaa', 0, '2026-03-04 13:25:49', NULL, 8, 3);

-- --------------------------------------------------------

--
-- Structure de la table `faq`
--

CREATE TABLE `faq` (
  `id` int(11) NOT NULL,
  `question` varchar(255) NOT NULL,
  `answer` longtext NOT NULL,
  `subject` varchar(50) DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `submitted_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `forum_post`
--

CREATE TABLE `forum_post` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` longtext NOT NULL,
  `author_id` int(11) NOT NULL,
  `platform_language_id` int(11) NOT NULL,
  `posted_at` datetime NOT NULL,
  `is_active` tinyint(4) NOT NULL,
  `view_count` int(11) NOT NULL,
  `reply_count` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `forum_reply`
--

CREATE TABLE `forum_reply` (
  `id` int(11) NOT NULL,
  `content` longtext NOT NULL,
  `author_id` int(11) NOT NULL,
  `replied_at` datetime NOT NULL,
  `is_active` tinyint(4) NOT NULL,
  `is_best_answer` tinyint(4) NOT NULL,
  `post_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `languages`
--

CREATE TABLE `languages` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `code` varchar(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `learning_stats`
--

CREATE TABLE `learning_stats` (
  `id` int(11) NOT NULL,
  `total_minutes_studied` int(11) NOT NULL,
  `words_learned` int(11) NOT NULL,
  `total_xp` int(11) NOT NULL,
  `last_study_session` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `learning_stats`
--

INSERT INTO `learning_stats` (`id`, `total_minutes_studied`, `words_learned`, `total_xp`, `last_study_session`, `user_id`) VALUES
(2, 0, 0, 0, NULL, 3),
(3, 0, 0, 500, NULL, 124),
(4, 1, 0, 20, '2026-03-03 22:59:59', 128),
(5, 0, 0, 0, NULL, 136);

-- --------------------------------------------------------

--
-- Structure de la table `lesson`
--

CREATE TABLE `lesson` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` longtext NOT NULL,
  `vocabulary_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`vocabulary_data`)),
  `grammar_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`grammar_data`)),
  `xp_reward` int(11) NOT NULL,
  `course_id` int(11) NOT NULL,
  `video_name` varchar(255) DEFAULT NULL,
  `thumb_name` varchar(255) DEFAULT NULL,
  `resource_name` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `lesson`
--

INSERT INTO `lesson` (`id`, `title`, `content`, `vocabulary_data`, `grammar_data`, `xp_reward`, `course_id`, `video_name`, `thumb_name`, `resource_name`, `updated_at`) VALUES
(1, 'orthographe', '🔤 Les lettres\n\nIl y a 26 lettres en français (comme en anglais).\n\nVoyelles : A – E – I – O – U – Y\nConsonnes : B – C – D – F – G – H – J – K – L – M – N – P – Q – R – S – T – V – W – X – Z\n\n✏ Exemple :\n\nA → “a” (comme dans ami)\n\nE → “eu”\n\nR → son prononcé dans la gorge\n\n2️⃣ Les salutations\n4\n👋 Dire bonjour\n\nBonjour → Hello\n\nSalut → Hi (informel)\n\nBonsoir → Good evening\n\n👋 Dire au revoir\n\nAu revoir → Goodbye\n\nÀ bientôt → See you soon\n\nÀ demain → See you tomorrow\n\n💬 Petit dialogue :\n\n— Bonjour !\n— Bonjour ! Ça va ?\n— Ça va bien, merci.', '[]', '[]', 20, 1, NULL, NULL, 'td-chapitre2-2425-69a7556242220651254511.pdf', '2026-03-03 22:40:50'),
(2, 'Cours francais débutant', 'Every person has a daily routine. A daily routine is the list of activities that we usually do every day. Having a routine helps us stay organized and manage our time better.\n\n1) Morning Routine\n\nMost people start their day in the morning. They wake up, brush their teeth, take a shower, and get dressed. Some people eat breakfast at home, while others prefer to drink coffee on their way to work or school.\n\nFor example:\n\nI wake up at 7 a.m.\n\nI brush my teeth and take a shower.\n\nI have breakfast with my family.\nRoutine', '[]', '[]', 20, 1, NULL, NULL, 'ch2-lmd-699ff93274370804329866.pdf', '2026-02-26 08:41:38');

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `mock_test`
--

CREATE TABLE `mock_test` (
  `id` int(11) NOT NULL,
  `platform_language_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `test_type` varchar(50) NOT NULL,
  `duration_minutes` int(11) NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `test_category` varchar(50) NOT NULL DEFAULT 'QCM',
  `level` varchar(50) NOT NULL DEFAULT 'Beginner'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `mock_test`
--

INSERT INTO `mock_test` (`id`, `platform_language_id`, `title`, `test_type`, `duration_minutes`, `is_active`, `created_at`, `updated_at`, `test_category`, `level`) VALUES
(1, 2, 'test qcm 1', 'TOEFL', 3, 1, '2026-02-26 08:54:54', NULL, 'QCM', 'Beginner'),
(2, 2, 'test listening', 'TOEFL', 4, 1, '2026-02-26 09:08:09', NULL, 'Listening', 'Beginner'),
(3, 2, 'TEST WRITING', 'TOEFL', 4, 1, '2026-02-26 09:10:14', NULL, 'Writing', 'Beginner'),
(4, 2, 'speakimg test', 'TOEFL', 4, 1, '2026-02-26 09:12:15', NULL, 'Speaking', 'Beginner'),
(5, 2, 'QCM', 'TOEFL', 2, 1, '2026-02-26 09:13:50', '2026-02-26 09:14:01', 'QCM', 'Intermediate'),
(6, 2, 'qcm', 'TOEFL', 2, 1, '2026-02-26 09:14:23', NULL, 'QCM', 'Advanced'),
(7, 1, 'test speaking', 'TOEFL', 6, 1, '2026-03-04 01:44:23', NULL, 'Speaking', 'Beginner');

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

CREATE TABLE `notifications` (
  `id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `message` longtext NOT NULL,
  `is_read` tinyint(4) NOT NULL,
  `created_at` datetime NOT NULL,
  `read_at` datetime DEFAULT NULL,
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`metadata`)),
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `notifications`
--

INSERT INTO `notifications` (`id`, `type`, `message`, `is_read`, `created_at`, `read_at`, `metadata`, `user_id`) VALUES
(1, 'premium', 'Your prenuim is valid', 1, '2026-02-25 23:20:44', '2026-02-25 23:38:53', '{\"sender\":\"admin\",\"admin_id\":1,\"sent_at\":\"2026-02-25T23:20:44+01:00\"}', 124),
(2, 'info', 'sfhnsdgjhsdgghasfj', 1, '2026-02-25 23:38:34', '2026-02-25 23:38:51', '{\"sender\":\"admin\",\"admin_id\":1,\"sent_at\":\"2026-02-25T23:38:34+01:00\"}', 124),
(3, 'premium', 'hello', 1, '2026-02-25 23:59:10', '2026-02-25 23:59:10', '{\"sender\":\"user\",\"reply_to\":1,\"sent_at\":\"2026-02-25T23:59:10+01:00\"}', 124),
(4, 'premium', 'holaaa', 1, '2026-02-25 23:59:19', '2026-02-25 23:59:19', '{\"sender\":\"user\",\"reply_to\":1,\"sent_at\":\"2026-02-25T23:59:19+01:00\"}', 124);

-- --------------------------------------------------------

--
-- Structure de la table `platform_language`
--

CREATE TABLE `platform_language` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `code` varchar(10) NOT NULL,
  `flag_url` varchar(255) NOT NULL,
  `is_enabled` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `platform_language`
--

INSERT INTO `platform_language` (`id`, `name`, `code`, `flag_url`, `is_enabled`) VALUES
(1, 'Francais', 'fr', 'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c3/Flag_of_France.svg/1280px-Flag_of_France.svg.png', 1),
(2, 'Anglais', 'ang', 'https://upload.wikimedia.org/wikipedia/commons/4/42/Flag_of_the_United_Kingdom.png', 1),
(3, 'arabe', 'ar', 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRY_TjvR8tHXLAY8N4TCoYzuZmlnSwTC51pvg&s', 1),
(4, 'espagnole', 'esp', 'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/Flag_of_Spain.svg/1280px-Flag_of_Spain.svg.png', 1),
(5, 'italie', 'ita', 'https://upload.wikimedia.org/wikipedia/commons/thumb/0/03/Flag_of_Italy.svg/1280px-Flag_of_Italy.svg.png', 1),
(6, 'allemen', 'al', 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Flag_of_Germany.svg/1280px-Flag_of_Germany.svg.png', 1);

-- --------------------------------------------------------

--
-- Structure de la table `publication`
--

CREATE TABLE `publication` (
  `id` int(11) NOT NULL,
  `titre_pub` varchar(255) NOT NULL,
  `type_pub` varchar(255) NOT NULL,
  `lien_pub` varchar(255) DEFAULT NULL,
  `contenu_pub` longtext NOT NULL,
  `date_pub` datetime NOT NULL,
  `likes` int(11) NOT NULL,
  `dislikes` int(11) NOT NULL,
  `report_pub` int(11) DEFAULT NULL,
  `floue` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `publication`
--

INSERT INTO `publication` (`id`, `titre_pub`, `type_pub`, `lien_pub`, `contenu_pub`, `date_pub`, `likes`, `dislikes`, `report_pub`, `floue`, `user_id`) VALUES
(1, 'test test', 'image', NULL, 'gasfshasdgafhasdgaeeghbawrghasfghbRHGaerfhsdfzh', '2026-02-25 22:41:46', 1, 0, 1, NULL, 1),
(3, 'Message Positif du Jour', 'image', '69a00b332e2a7.jpg', 'C\'est une splendide journée ! Profitons de cette opportunité pour nous concentrer sur nos objectifs et aborder chaque défi avec optimisme.', '2026-02-26 09:58:27', 2, 0, NULL, NULL, 1),
(4, 'bonne journee', 'image', '69a83ea5729c3.png', 'c est une bonne journee', '2026-03-04 15:16:05', 0, 0, NULL, NULL, 1);

-- --------------------------------------------------------

--
-- Structure de la table `quiz`
--

CREATE TABLE `quiz` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` longtext DEFAULT NULL,
  `passing_score` int(11) NOT NULL,
  `question_count` int(11) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `enabled` tinyint(4) NOT NULL,
  `lesson_id` int(11) DEFAULT NULL,
  `difficulty` smallint(6) NOT NULL DEFAULT 3,
  `skill_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '[]' CHECK (json_valid(`skill_codes`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `quiz`
--

INSERT INTO `quiz` (`id`, `title`, `description`, `passing_score`, `question_count`, `created_at`, `updated_at`, `enabled`, `lesson_id`, `difficulty`, `skill_codes`) VALUES
(1, 'francais quizzes', 'Ce quiz de français a pour objectif d’évaluer et de renforcer vos connaissances en langue française. Il couvre différents aspects essentiels tels que la grammaire, la conjugaison, le vocabulaire, l’orthographe et la compréhension écrite.', 10, 5, '2026-03-03 20:47:35', '2026-03-03 20:47:50', 1, 2, 2, '[\"grammaire\"]');

-- --------------------------------------------------------

--
-- Structure de la table `quiz_attempt`
--

CREATE TABLE `quiz_attempt` (
  `id` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  `state` varchar(50) NOT NULL,
  `attempt_number` smallint(6) NOT NULL,
  `created_at` datetime NOT NULL,
  `finished_at` datetime DEFAULT NULL,
  `best_score` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `lesson_id` int(11) DEFAULT NULL,
  `second_chance_exercise_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `quiz_attempt`
--

INSERT INTO `quiz_attempt` (`id`, `score`, `state`, `attempt_number`, `created_at`, `finished_at`, `best_score`, `user_id`, `quiz_id`, `lesson_id`, `second_chance_exercise_id`) VALUES
(1, 20, 'finished_second', 1, '2026-03-03 21:31:20', '2026-03-03 21:31:32', 20, 128, 1, 2, 4),
(2, 40, 'finished_second', 1, '2026-03-03 22:32:53', '2026-03-03 22:33:47', 40, 128, 1, 2, 4),
(3, 20, 'second_chance_available', 1, '2026-03-03 22:49:10', '2026-03-03 22:49:10', 20, 128, 1, 2, NULL),
(4, 20, 'second_chance_available', 1, '2026-03-03 23:02:14', '2026-03-03 23:02:14', 20, 128, 1, 2, NULL),
(5, 20, 'second_chance_available', 1, '2026-03-03 23:27:28', '2026-03-03 23:27:28', 20, 128, 1, 2, NULL),
(6, 20, 'second_chance_available', 1, '2026-03-03 23:33:26', '2026-03-03 23:33:26', 20, 128, 1, 2, NULL),
(7, 20, 'second_chance_available', 1, '2026-03-03 23:51:52', '2026-03-03 23:51:52', 20, 128, 1, 2, NULL),
(8, 20, 'finished_second', 1, '2026-03-04 13:25:25', '2026-03-04 13:25:49', 20, 128, 1, 2, 3);

-- --------------------------------------------------------

--
-- Structure de la table `quiz_schedule`
--

CREATE TABLE `quiz_schedule` (
  `id` int(11) NOT NULL,
  `scheduled_at` datetime NOT NULL,
  `status` varchar(20) NOT NULL,
  `note` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `student_id` int(11) NOT NULL,
  `quiz_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `quiz_schedule`
--

INSERT INTO `quiz_schedule` (`id`, `scheduled_at`, `status`, `note`, `created_at`, `updated_at`, `student_id`, `quiz_id`) VALUES
(1, '2026-03-12 12:02:00', 'DONE', 'rappel moi', '2026-03-03 20:56:34', '2026-03-03 21:31:20', 128, 1);

-- --------------------------------------------------------

--
-- Structure de la table `reclamation`
--

CREATE TABLE `reclamation` (
  `id` int(11) NOT NULL,
  `subject` varchar(100) NOT NULL,
  `message_body` longtext NOT NULL,
  `status` varchar(20) NOT NULL,
  `submitted_at` datetime NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `priority` varchar(10) NOT NULL,
  `sla_deadline` datetime DEFAULT NULL,
  `is_late` tinyint(4) NOT NULL DEFAULT 0,
  `resolved_at` datetime DEFAULT NULL,
  `satisfaction_score` int(11) DEFAULT NULL,
  `satisfaction_comment` longtext DEFAULT NULL,
  `satisfaction_rated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reclamation`
--

INSERT INTO `reclamation` (`id`, `subject`, `message_body`, `status`, `submitted_at`, `user_id`, `priority`, `sla_deadline`, `is_late`, `resolved_at`, `satisfaction_score`, `satisfaction_comment`, `satisfaction_rated_at`) VALUES
(1, 'payment', 'wooooooooooooooooooooooooooooh', 'RESOLVED', '2026-03-02 23:59:47', 128, 'MEDIUM', '2026-03-03 23:59:47', 0, '2026-03-03 00:01:48', 1, NULL, '2026-03-04 14:49:57'),
(2, 'payment', 'urgeeeennttttt', 'IN_PROGRESS', '2026-03-03 00:02:50', 128, 'MEDIUM', '2026-03-04 00:07:56', 0, '2026-03-03 00:07:19', NULL, NULL, NULL),
(3, 'payment', 'urgent repondez', 'PENDING', '2026-03-03 00:03:40', 128, 'URGENT', '2026-03-03 02:03:40', 0, NULL, NULL, NULL, NULL),
(4, 'payment', 'im  verry happy', 'PENDING', '2026-03-03 00:11:10', 128, 'MEDIUM', '2026-03-04 00:11:10', 0, NULL, NULL, NULL, NULL),
(5, 'payment', 'hello hiiiiiii', 'PENDING', '2026-03-04 14:44:47', 128, 'MEDIUM', '2026-03-05 14:44:47', 0, NULL, NULL, NULL, NULL),
(6, 'payment', 'helloooo hiiii', 'PENDING', '2026-03-04 14:45:00', 128, 'MEDIUM', '2026-03-05 14:45:00', 0, NULL, NULL, NULL, NULL),
(7, 'payment', 'yezehhaaaaaa', 'RESOLVED', '2026-03-04 14:45:20', 128, 'MEDIUM', '2026-03-05 14:45:20', 0, '2026-03-04 14:48:57', NULL, NULL, NULL),
(8, 'payment', 'je suis triste', 'RESOLVED', '2026-03-04 15:33:25', 128, 'MEDIUM', '2026-03-05 15:33:26', 0, '2026-03-04 15:34:51', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `reclamation_audit`
--

CREATE TABLE `reclamation_audit` (
  `id` int(10) UNSIGNED NOT NULL,
  `type` varchar(10) NOT NULL,
  `object_id` varchar(255) NOT NULL,
  `discriminator` varchar(255) DEFAULT NULL,
  `transaction_hash` varchar(40) DEFAULT NULL,
  `diffs` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`diffs`)),
  `blame_id` varchar(255) DEFAULT NULL,
  `blame_user` varchar(255) DEFAULT NULL,
  `blame_user_fqdn` varchar(255) DEFAULT NULL,
  `blame_user_firewall` varchar(100) DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `reclamation_audit`
--

INSERT INTO `reclamation_audit` (`id`, `type`, `object_id`, `discriminator`, `transaction_hash`, `diffs`, `blame_id`, `blame_user`, `blame_user_fqdn`, `blame_user_firewall`, `ip`, `created_at`) VALUES
(1, 'insert', '1', NULL, '5645dfb50e4bf6222ed3bd5dd670a0dd5841af2f', '{\"@source\":{\"id\":1,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#1\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"wooooooooooooooooooooooooooooh\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-02 23:59:47\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-03 23:59:47\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-02 22:59:47'),
(2, 'update', '1', NULL, 'fb3fdf27df3726757ef5c03abc0e9411257aa21f', '{\"status\":{\"old\":\"PENDING\",\"new\":\"RESOLVED\"},\"resolvedAt\":{\"new\":\"2026-03-03 00:01:48\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-02 23:01:48'),
(3, 'insert', '2', NULL, '01c68e9904b8da49a9ef951c683446ecebc81b6d', '{\"@source\":{\"id\":2,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#2\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"urgeeeennttttt\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-03 00:02:50\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-04 00:02:50\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-02 23:02:50'),
(4, 'insert', '3', NULL, 'e669948f71f7b0d66d3a11b5e44e2bb29aac302b', '{\"@source\":{\"id\":3,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#3\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"urgent repondez\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-03 00:03:40\"},\"priority\":{\"new\":\"URGENT\"},\"slaDeadline\":{\"new\":\"2026-03-03 02:03:40\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-02 23:03:40'),
(5, 'update', '2', NULL, '2f085716589f60d615730438d0ceda5a38da6db8', '{\"status\":{\"old\":\"PENDING\",\"new\":\"RESOLVED\"},\"resolvedAt\":{\"new\":\"2026-03-03 00:07:19\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-02 23:07:19'),
(6, 'update', '2', NULL, 'ca5e078f46a0f8741150eae07b7c266e47e542df', '{\"status\":{\"old\":\"RESOLVED\",\"new\":\"CLOSED\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-02 23:07:38'),
(7, 'update', '2', NULL, '729c9088c1d566814f97ece30d7545e468ae6d3e', '{\"status\":{\"old\":\"CLOSED\",\"new\":\"IN_PROGRESS\"},\"slaDeadline\":{\"old\":\"2026-03-04 00:02:50\",\"new\":\"2026-03-04 00:07:56\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-02 23:07:56'),
(8, 'insert', '4', NULL, 'a45b08943e3371317ce028ac31ff76c21cd73e64', '{\"@source\":{\"id\":4,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#4\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"im  verry happy\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-03 00:11:10\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-04 00:11:10\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-02 23:11:10'),
(9, 'insert', '5', NULL, '483b8a1852cf24e6e4c91d6903171c1c1ca31d19', '{\"@source\":{\"id\":5,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#5\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"hello hiiiiiii\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-04 14:44:47\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-05 14:44:47\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-04 13:44:47'),
(10, 'insert', '6', NULL, '1cabde463b919ab80d6f4216a11a2d2a087bef8b', '{\"@source\":{\"id\":6,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#6\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"helloooo hiiii\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-04 14:45:00\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-05 14:45:00\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-04 13:45:00'),
(11, 'insert', '7', NULL, '71b784d2e35b5d8cc02c4fbaac381cf695b32cfe', '{\"@source\":{\"id\":7,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#7\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"yezehhaaaaaa\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-04 14:45:20\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-05 14:45:20\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-04 13:45:20'),
(12, 'update', '7', NULL, '89d0dd681381bbc2a316c9c9ee5ebf56d5b5c6b1', '{\"status\":{\"old\":\"PENDING\",\"new\":\"RESOLVED\"},\"resolvedAt\":{\"new\":\"2026-03-04 14:48:57\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-04 13:48:57'),
(13, 'update', '1', NULL, 'df4d7591ce7579c1825d8cdac2a9147b448f1e57', '{\"satisfactionScore\":{\"new\":1},\"satisfactionRatedAt\":{\"new\":\"2026-03-04 14:49:57\"}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-04 13:49:57'),
(14, 'insert', '8', NULL, 'fc40ea94b758deb37dce4dbef3cd418390ffd559', '{\"@source\":{\"id\":8,\"class\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation\",\"label\":\"App\\\\Module\\\\Support\\\\Entity\\\\Reclamation#8\",\"table\":\"reclamation\"},\"subject\":{\"new\":\"payment\"},\"messageBody\":{\"new\":\"je suis triste\"},\"status\":{\"new\":\"PENDING\"},\"submittedAt\":{\"new\":\"2026-03-04 15:33:25\"},\"priority\":{\"new\":\"MEDIUM\"},\"slaDeadline\":{\"new\":\"2026-03-05 15:33:26\"},\"isLate\":{\"new\":false},\"user\":{\"new\":{\"id\":128,\"class\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User\",\"label\":\"App\\\\Module\\\\UserManagement\\\\Entity\\\\User#128\",\"table\":\"users\"}}}', '128', 'ahmed@esprit.tn', 'DH\\Auditor\\User\\User', 'main', '127.0.0.1', '2026-03-04 14:33:26'),
(15, 'update', '8', NULL, 'add4fa00964093bcc34190fdff0f2d61555a074e', '{\"status\":{\"old\":\"PENDING\",\"new\":\"RESOLVED\"},\"resolvedAt\":{\"new\":\"2026-03-04 15:34:51\"}}', '1', 'belhajhamdi185@gmail.com', 'DH\\Auditor\\User\\User', 'admin', '127.0.0.1', '2026-03-04 14:34:51');

-- --------------------------------------------------------

--
-- Structure de la table `recommendation_session`
--

CREATE TABLE `recommendation_session` (
  `id` int(11) NOT NULL,
  `weak_skill_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`weak_skill_codes`)),
  `recommended_exercise_ids` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`recommended_exercise_ids`)),
  `ai_feedback` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `recommendation_session`
--

INSERT INTO `recommendation_session` (`id`, `weak_skill_codes`, `recommended_exercise_ids`, `ai_feedback`, `created_at`, `user_id`) VALUES
(1, '[\"grammaire\"]', '[1,2,3,4,5]', NULL, '2026-03-03 22:32:09', 128),
(2, '[\"grammaire\"]', '[1,2,3,4,5]', NULL, '2026-03-03 22:32:31', 128),
(3, '[\"grammaire\"]', '[1,2,3,4,5]', NULL, '2026-03-05 08:40:49', 128);

-- --------------------------------------------------------

--
-- Structure de la table `skill_profile`
--

CREATE TABLE `skill_profile` (
  `id` int(11) NOT NULL,
  `skill_code` varchar(100) NOT NULL,
  `mastery` smallint(6) NOT NULL DEFAULT 0,
  `attempts_count` int(11) NOT NULL DEFAULT 0,
  `updated_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `skill_profile`
--

INSERT INTO `skill_profile` (`id`, `skill_code`, `mastery`, `attempts_count`, `updated_at`, `user_id`) VALUES
(1, 'grammaire', 19, 43, '2026-03-05 08:40:48', 128);

-- --------------------------------------------------------

--
-- Structure de la table `support_audit_logs`
--

CREATE TABLE `support_audit_logs` (
  `id` int(11) NOT NULL,
  `action` varchar(50) NOT NULL,
  `description` varchar(255) NOT NULL,
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`metadata`)),
  `created_at` datetime NOT NULL,
  `old_value` varchar(50) DEFAULT NULL,
  `new_value` varchar(50) DEFAULT NULL,
  `reclamation_id` int(11) DEFAULT NULL,
  `performed_by_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `support_audit_logs`
--

INSERT INTO `support_audit_logs` (`id`, `action`, `description`, `metadata`, `created_at`, `old_value`, `new_value`, `reclamation_id`, `performed_by_id`) VALUES
(1, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #1', NULL, '2026-03-03 00:00:15', NULL, NULL, 1, 1),
(2, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #1', NULL, '2026-03-03 00:01:37', NULL, NULL, 1, 1),
(3, 'STATUS_CHANGED', 'Statut changé de \'PENDING\' → \'RESOLVED\' par Hamdi Belhaj', NULL, '2026-03-03 00:01:48', 'PENDING', 'RESOLVED', 1, 1),
(4, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #2', NULL, '2026-03-03 00:07:02', NULL, NULL, 2, 1),
(5, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #2', NULL, '2026-03-03 00:07:19', NULL, NULL, 2, 1),
(6, 'STATUS_CHANGED', 'Statut changé de \'PENDING\' → \'RESOLVED\' par Hamdi Belhaj', NULL, '2026-03-03 00:07:19', 'PENDING', 'RESOLVED', 2, 1),
(7, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #2', NULL, '2026-03-03 00:07:37', NULL, NULL, 2, 1),
(8, 'STATUS_CHANGED', 'Statut changé de \'RESOLVED\' → \'CLOSED\' par Hamdi Belhaj', NULL, '2026-03-03 00:07:38', 'RESOLVED', 'CLOSED', 2, 1),
(9, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #2', NULL, '2026-03-03 00:07:55', NULL, NULL, 2, 1),
(10, 'STATUS_CHANGED', 'Statut changé de \'CLOSED\' → \'IN_PROGRESS\' par Hamdi Belhaj', NULL, '2026-03-03 00:07:56', 'CLOSED', 'IN_PROGRESS', 2, 1),
(11, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #7', NULL, '2026-03-04 14:48:34', NULL, NULL, 7, 1),
(12, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #7', NULL, '2026-03-04 14:48:52', NULL, NULL, 7, 1),
(13, 'STATUS_CHANGED', 'Statut changé de \'PENDING\' → \'RESOLVED\' par Hamdi Belhaj', NULL, '2026-03-04 14:48:57', 'PENDING', 'RESOLVED', 7, 1),
(14, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #7', NULL, '2026-03-04 14:49:01', NULL, NULL, 7, 1),
(15, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #7', NULL, '2026-03-04 14:49:15', NULL, NULL, 7, 1),
(16, 'SATISFACTION_RATED', 'Note 1/5 donnée par Ahmed Mahmoud pour le ticket #1', '{\"score\":1}', '2026-03-04 14:49:57', NULL, '1', 1, 128),
(17, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #8', NULL, '2026-03-04 15:34:21', NULL, NULL, 8, 1),
(18, 'RESPONSE_ADDED', 'Réponse ajoutée par Hamdi Belhaj sur la réclamation #8', NULL, '2026-03-04 15:34:49', NULL, NULL, 8, 1),
(19, 'STATUS_CHANGED', 'Statut changé de \'PENDING\' → \'RESOLVED\' par Hamdi Belhaj', NULL, '2026-03-04 15:34:51', 'PENDING', 'RESOLVED', 8, 1);

-- --------------------------------------------------------

--
-- Structure de la table `support_notifications`
--

CREATE TABLE `support_notifications` (
  `id` int(11) NOT NULL,
  `message` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL,
  `is_read` tinyint(4) NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL,
  `reclamation_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `support_response`
--

CREATE TABLE `support_response` (
  `id` int(11) NOT NULL,
  `message` longtext NOT NULL,
  `responded_at` datetime NOT NULL,
  `reclamation_id` int(11) NOT NULL,
  `author_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `support_response`
--

INSERT INTO `support_response` (`id`, `message`, `responded_at`, `reclamation_id`, `author_id`) VALUES
(1, 'okkkkkkk', '2026-03-03 00:00:15', 1, 1),
(2, 'rrrrrrrrrrrrrr', '2026-03-03 00:01:37', 1, 1),
(3, 'okkkkk  biennn noteee', '2026-03-03 00:07:02', 2, 1),
(4, 'okk bien notee', '2026-03-03 00:07:19', 2, 1),
(5, 'okkkkk', '2026-03-03 00:07:37', 2, 1),
(6, 'hiiiiiii', '2026-03-03 00:07:55', 2, 1),
(7, 'okkkk  bien noteee', '2026-03-04 14:48:34', 7, 1),
(8, 'okkkk bien', '2026-03-04 14:48:52', 7, 1),
(9, 'okkkk bien', '2026-03-04 14:49:01', 7, 1),
(10, 'okkkkkkkkk', '2026-03-04 14:49:15', 7, 1),
(11, 'okkkk    bien noteeee', '2026-03-04 15:34:21', 8, 1),
(12, 'okkk  bien noteee', '2026-03-04 15:34:49', 8, 1);

-- --------------------------------------------------------

--
-- Structure de la table `test_question`
--

CREATE TABLE `test_question` (
  `id` int(11) NOT NULL,
  `section_category` varchar(100) NOT NULL,
  `question_text` longtext NOT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`options`)),
  `correct_answer` longtext DEFAULT NULL,
  `points` int(11) NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `mock_test_id` int(11) NOT NULL,
  `question_type` varchar(50) NOT NULL DEFAULT 'qcm',
  `reading_passage` longtext DEFAULT NULL,
  `audio_text` longtext DEFAULT NULL,
  `writing_subject` longtext DEFAULT NULL,
  `embedding` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`embedding`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `test_question`
--

INSERT INTO `test_question` (`id`, `section_category`, `question_text`, `options`, `correct_answer`, `points`, `is_active`, `created_at`, `updated_at`, `mock_test_id`, `question_type`, `reading_passage`, `audio_text`, `writing_subject`, `embedding`) VALUES
(1, 'QCM', 'What ___ your name?', '{\"A\":\"IS\",\"B\":\"YOUR\",\"C\":\"MY\"}', 'IS', 2, 1, '2026-02-26 08:55:40', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(2, 'QCM', '. She ___ a doctor.', '{\"A\":\"HAVE\",\"B\":\"IS\",\"C\":\"ARE\"}', 'IS', 5, 1, '2026-02-26 08:56:19', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(3, 'QCM', 'I ___ to school every day.', '{\"A\":\"goes\",\"B\":\"go\",\"C\":\"going\"}', 'go', 2, 1, '2026-02-26 08:56:57', '2026-02-26 08:57:28', 1, 'qcm', NULL, NULL, NULL, NULL),
(4, 'QCM', '___ you speak English?', '{\"A\":\"do\",\"B\":\"does\",\"C\":\"is\",\"D\":\"\"}', 'do', 1, 1, '2026-02-26 08:57:54', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(5, 'QCM', 'There ___ two cats in the garden.', '{\"A\":\"is\",\"B\":\"am\",\"C\":\"are\"}', 'are', 4, 1, '2026-02-26 08:58:34', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(6, 'QCM', 'What is the plural of \"child\"?', '{\"A\":\"childs\",\"B\":\"childrens\",\"C\":\"children\"}', 'children', 2, 1, '2026-02-26 08:59:18', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(7, 'QCM', 'He ___ TV last night.', '{\"A\":\"watch\",\"B\":\"watched\",\"C\":\"waatches\"}', 'watch', 2, 1, '2026-02-26 08:59:56', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(8, 'QCM', 'Which word is an adjective?', '{\"A\":\"quickly\",\"B\":\"beautiful\",\"C\":\"run\"}', 'beautiful', 2, 1, '2026-02-26 09:00:42', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(9, 'QCM', '___ is your birthday?', '{\"A\":\"where\",\"B\":\"who\",\"C\":\"when\"}', 'when', 2, 1, '2026-02-26 09:01:22', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(10, 'QCM', 'I have ___ apple on my desk.', '{\"A\":\"a\",\"B\":\"an\",\"C\":\"the\"}', 'an', 2, 1, '2026-02-26 09:01:55', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(11, 'QCM', 'Which sentence is correct?', '{\"A\":\"She don\'t like coffee.\",\"B\":\"She doesn\'t likes coffee.\",\"C\":\"She doesn\'t like coffee.\"}', '', 2, 1, '2026-02-26 09:02:31', NULL, 1, 'qcm', NULL, NULL, NULL, NULL),
(12, 'QCM', 'QQQQQQQQQQQQQQ', '{\"A\":\"WEEEE\",\"B\":\"XXXXXXXXX\",\"C\":\"\",\"D\":\"\"}', 'XXXXXXXXX', 2, 1, '2026-02-26 09:14:45', NULL, 6, 'qcm', NULL, NULL, NULL, NULL),
(13, 'QCM', 'AAAAAAAAA', '{\"A\":\"QERTY\",\"B\":\"YTREWQ\",\"C\":\"\",\"D\":\"\"}', 'YTREWQ', 2, 1, '2026-02-26 09:15:13', NULL, 5, 'qcm', NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `test_result`
--

CREATE TABLE `test_result` (
  `id` int(11) NOT NULL,
  `overall_score` double NOT NULL,
  `ai_predicted_score` double DEFAULT NULL,
  `ai_weakness_report` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`ai_weakness_report`)),
  `ai_correction` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`ai_correction`)),
  `ai_note` double DEFAULT NULL,
  `date_taken` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `mock_test_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `test_result`
--

INSERT INTO `test_result` (`id`, `overall_score`, `ai_predicted_score`, `ai_weakness_report`, `ai_correction`, `ai_note`, `date_taken`, `updated_at`, `mock_test_id`, `user_id`) VALUES
(1, 18.4, NULL, '[{\"section\":\"QCM\",\"score\":90.0,\"status\":\"good\",\"correct\":9,\"total\":10}]', '{\"timeReport\":{\"isValid\":true,\"isSuspicious\":false,\"elapsedSeconds\":60,\"allowedSeconds\":180,\"overageSeconds\":0,\"timeRatio\":0.333,\"timeUsedPct\":33.3,\"penaltyPct\":0,\"rawScore\":18.4,\"adjustedScore\":18.4,\"verdict\":\"on_time\",\"verdictLabel\":\"On time\",\"verdictColor\":\"green\"},\"elapsedTime\":{\"seconds\":60,\"formatted\":\"1 min 00 sec\",\"ratio\":33.3},\"rawScore\":18.4}', NULL, '2026-02-26 09:06:58', NULL, 1, 124),
(2, 0, NULL, '[{\"section\":\"Multiple Choice (QCM)\",\"correct\":0,\"total\":3,\"score\":0.0,\"status\":\"needs_improvement\"},{\"section\":\"Open Questions\",\"correct\":0,\"total\":2,\"score\":0.0,\"status\":\"needs_improvement\"}]', '{\"type\":\"listening\",\"timeReport\":{\"isValid\":true,\"isSuspicious\":false,\"elapsedSeconds\":69,\"allowedSeconds\":240,\"overageSeconds\":0,\"timeRatio\":0.288,\"timeUsedPct\":28.8,\"penaltyPct\":0,\"rawScore\":0.0,\"adjustedScore\":0.0,\"verdict\":\"on_time\",\"verdictLabel\":\"On time\",\"verdictColor\":\"green\"},\"elapsedTime\":{\"seconds\":69,\"formatted\":\"1 min 09 sec\",\"ratio\":28.8},\"rawScore\":0.0,\"openEvaluation\":{\"evaluations\":[{\"isCorrect\":false,\"score\":0.0,\"feedback\":\"Your answer \'ERGH\' does not relate to the correct activity, \'yoga class\'. It seems you had difficulty understanding this part of the audio.\"},{\"isCorrect\":false,\"score\":0.0,\"feedback\":\"Your answer \'LKJHGF\' is not related to the cafe\'s closing time, which is \'5 PM\'. Please focus on listening for numbers and time expressions.\"}],\"openScore\":0.0,\"feedback\":\"It appears you had significant difficulty with these listening comprehension questions. Your answers consist of random letters, indicating that you were unable to identify any relevant information from the audio. For future practice, try to listen for keywords from the questions (e.g., \'activity\', \'time\', \'open\') and focus on understanding the main points. Keep practicing, listening skills improve with effort!\"},\"audioType\":\"announcement\"}', 0, '2026-02-26 09:09:43', NULL, 2, 124),
(3, 12.5, NULL, NULL, '{\"note\":12.5,\"feedback\":\"This is a good start to express your favorite day. You\'ve identified the topic, which is great for a beginner. However, there are some important grammatical corrections needed to make your sentence clear and correct.\",\"grammar\":\"There are several grammatical errors. The main issue is with subject-verb agreement and word order: \'my favorite are day\' should be \'my favorite day is\'. \'Are\' is used for plural subjects, but \'day\' is singular. Also, remember to start sentences with a capital letter and end them with a period.\",\"coherence\":\"The sentence is short and directly answers the question, so it is coherent in its message. For future essays, try to expand with more sentences to elaborate on your choice.\",\"vocabulary\":\"The vocabulary used (\'my\', \'favorite\', \'day\', \'monday\') is simple and appropriate for a beginner level. It clearly conveys your message.\",\"suggestions\":[\"Review subject-verb agreement, especially for \'is\' and \'are\'.\",\"Practice starting sentences with a capital letter and ending them with a period.\",\"Try to write a few more sentences next time to explain *why* Monday is your favorite day.\"],\"passed\":true,\"topic\":\"My Favorite Day of the Week\",\"userResponse\":\"my favorite are day is monday\",\"wordCount\":6}', 12.5, '2026-02-26 09:11:17', NULL, 3, 124),
(4, 0, NULL, '[{\"section\":\"QCM\",\"score\":0.0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1}]', '{\"timeReport\":{\"isValid\":true,\"isSuspicious\":true,\"elapsedSeconds\":5,\"allowedSeconds\":120,\"overageSeconds\":0,\"timeRatio\":0.042,\"timeUsedPct\":4.2,\"penaltyPct\":0,\"rawScore\":0.0,\"adjustedScore\":0.0,\"verdict\":\"suspicious\",\"verdictLabel\":\"Submitted too fast\",\"verdictColor\":\"orange\"},\"elapsedTime\":{\"seconds\":5,\"formatted\":\"0 min 05 sec\",\"ratio\":4.2},\"rawScore\":0.0}', NULL, '2026-02-26 09:15:26', NULL, 5, 124),
(5, 18, NULL, '[{\"section\":\"QCM\",\"score\":100.0,\"status\":\"good\",\"correct\":1,\"total\":1}]', '{\"timeReport\":{\"isValid\":true,\"isSuspicious\":true,\"elapsedSeconds\":5,\"allowedSeconds\":120,\"overageSeconds\":0,\"timeRatio\":0.042,\"timeUsedPct\":4.2,\"penaltyPct\":0,\"rawScore\":20.0,\"adjustedScore\":18.0,\"verdict\":\"suspicious\",\"verdictLabel\":\"Submitted too fast\",\"verdictColor\":\"orange\"},\"elapsedTime\":{\"seconds\":5,\"formatted\":\"0 min 05 sec\",\"ratio\":4.2},\"rawScore\":20.0}', NULL, '2026-02-26 09:15:36', NULL, 5, 124),
(6, 18, NULL, '[{\"section\":\"QCM\",\"score\":100.0,\"status\":\"good\",\"correct\":1,\"total\":1}]', '{\"timeReport\":{\"isValid\":true,\"isSuspicious\":true,\"elapsedSeconds\":4,\"allowedSeconds\":120,\"overageSeconds\":0,\"timeRatio\":0.033,\"timeUsedPct\":3.3,\"penaltyPct\":0,\"rawScore\":20.0,\"adjustedScore\":18.0,\"verdict\":\"suspicious\",\"verdictLabel\":\"Submitted too fast\",\"verdictColor\":\"orange\"},\"elapsedTime\":{\"seconds\":4,\"formatted\":\"0 min 04 sec\",\"ratio\":3.3},\"rawScore\":20.0}', NULL, '2026-02-26 09:15:50', NULL, 6, 124),
(7, 10, NULL, '[{\"section\":\"Writing\",\"score\":10.0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1}]', '{\"note\":10.0,\"feedback\":\"Unable to process correction. Please try again.\",\"grammar\":\"Analysis unavailable\",\"coherence\":\"Analysis unavailable\",\"vocabulary\":\"Analysis unavailable\",\"suggestions\":[\"Please resubmit your essay\"],\"passed\":false,\"topic\":\"Write about your daily routine and hobbies.\",\"userResponse\":\"zstgdnvzfg<dfgshfdkyjeydgzjseztdjuazetdjs\",\"wordCount\":2}', 10, '2026-03-04 02:06:18', NULL, 3, 128),
(8, 10, NULL, NULL, '{\"note\":10.0,\"feedback\":\"Unable to process correction. Please try again.\",\"grammar\":\"Analysis unavailable\",\"coherence\":\"Analysis unavailable\",\"vocabulary\":\"Analysis unavailable\",\"suggestions\":[\"Please resubmit your essay\"],\"passed\":false,\"topic\":\"Write about your daily routine and hobbies.\",\"userResponse\":\"fsafasfsafsafasfsa\",\"wordCount\":1}', 10, '2026-03-04 02:07:37', NULL, 3, 128),
(9, 6.5, NULL, '[{\"section\":\"Grammar\",\"score\":0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1},{\"section\":\"Vocabulary\",\"score\":0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1},{\"section\":\"Fluency\",\"score\":0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1},{\"section\":\"Coherence\",\"score\":0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1},{\"section\":\"Pronunciation\",\"score\":0,\"status\":\"needs_improvement\",\"correct\":0,\"total\":1}]', '{\"type\":\"speaking\",\"evaluation\":{\"note\":6.5,\"grammar\":\"The student\'s grammar is limited and often incorrect, with simple sentence structures and a lack of verb tense consistency, which is expected at the beginner level, but the errors are frequent and hinder communication\",\"vocabulary\":\"The student\'s vocabulary is very limited, with a reliance on basic words and phrases, and a lack of descriptive language, which makes it difficult to express opinions or ideas\",\"fluency\":\"The student\'s fluency is affected by their limited grammar and vocabulary, resulting in short and simple sentences, and a lack of cohesion between ideas, despite the high fluency score from the audio analysis, the student\'s speech is not smooth or natural\",\"coherence\":\"The student\'s responses often lack coherence and relevance to the topic, with abrupt changes in direction and a lack of clear ideas or opinions, making it challenging to follow their train of thought\",\"pronunciation\":\"The student\'s pronunciation is not clear and is affected by their first language, with difficulties in intonation, stress, and individual sounds, which can lead to misunderstandings\",\"strengths\":[\"The student is willing to express their opinions and feelings\",\"The student has a supportive friend, Takwa, which is a positive aspect of their life\"],\"improvements\":[\"The student needs to work on expanding their vocabulary and grammar to improve their ability to express themselves\",\"The student should practice speaking in a more coherent and organized manner to communicate their ideas effectively\"],\"overall_feedback\":\"The student is at a beginner level and needs to work on all aspects of their English language skills, including grammar, vocabulary, fluency, coherence, and pronunciation, to improve their ability to communicate effectively and express their ideas clearly, with a focus on building their confidence and skills in a supportive and encouraging environment\",\"passed\":false,\"cefr_level\":\"A1\"},\"history\":[{\"role\":\"gemini\",\"text\":\"Hello and welcome to your English speaking test! Can you tell me what you like about the city where you live?\"},{\"role\":\"user\",\"text\":\"I don\'t like anything. I hate it. It sucks.\",\"fluencyMetrics\":{\"wordsPerMinute\":130,\"avgConfidence\":0.994,\"pauseCount\":0,\"fluencyScore\":100}},{\"role\":\"gemini\",\"text\":\"That\'s quite honest of you, let\'s try to find something positive though! Is there anything that you think could be improved in your city to make it a better place to live?\"},{\"role\":\"user\",\"text\":\"I don\'t have anything positive. I\'m and my friend, Takwa, just make me great.\",\"fluencyMetrics\":{\"wordsPerMinute\":136,\"avgConfidence\":0.917,\"pauseCount\":0,\"fluencyScore\":97}},{\"role\":\"gemini\",\"text\":\"That\'s a great point about having supportive friends like Takwa, it\'s wonderful to have people who make us feel great! If you could change one thing about your city to make it a more enjoyable place to live, what would it be?\"},{\"role\":\"user\",\"text\":\"Not great. Not great. It makes me crazy. She made you. She\'s so stupid.\",\"fluencyMetrics\":{\"wordsPerMinute\":130,\"avgConfidence\":0.984,\"pauseCount\":1,\"fluencyScore\":96}},{\"role\":\"gemini\",\"text\":\"It sounds like you\'re feeling a bit frustrated, let\'s try to focus on finding a solution If you could talk to the person in charge of your city, what would you say to them to make your city a better place to live?\"},{\"role\":\"user\",\"text\":\"I will say burn it.\",\"fluencyMetrics\":{\"wordsPerMinute\":122,\"avgConfidence\":0.948,\"pauseCount\":0,\"fluencyScore\":98}},{\"role\":\"gemini\",\"text\":\"That\'s a very strong feeling, let\'s try to think of a more constructive way to express it What do you think would need to change in your city for you to feel more positive about living there?\"},{\"role\":\"user\",\"text\":\"I will change the president.\",\"fluencyMetrics\":{\"wordsPerMinute\":102,\"avgConfidence\":0.966,\"pauseCount\":0,\"fluencyScore\":99}},{\"role\":\"gemini\",\"text\":\"That\'s an interesting idea, changing the president could certainly bring about significant changes to the city, I appreciate your honesty and willingness to share your thoughts throughout our conversation\"}],\"avgFluency\":{\"wordsPerMinute\":124.0,\"avgConfidence\":0.96,\"pauseCount\":0.2,\"fluencyScore\":98.0},\"timeReport\":{\"isValid\":true,\"isSuspicious\":false,\"elapsedSeconds\":132,\"allowedSeconds\":240,\"overageSeconds\":0,\"timeRatio\":0.55,\"timeUsedPct\":55.0,\"penaltyPct\":0,\"rawScore\":6.5,\"adjustedScore\":6.5,\"verdict\":\"on_time\",\"verdictLabel\":\"On time\",\"verdictColor\":\"green\"},\"elapsedTime\":{\"seconds\":132,\"formatted\":\"2 min 12 sec\",\"ratio\":55.0},\"rawScore\":6.5,\"subject\":\"Your City\"}', 6.5, '2026-03-04 15:58:50', NULL, 4, 128);

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `email` varchar(180) NOT NULL,
  `roles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`roles`)),
  `password` varchar(255) NOT NULL,
  `subscription_plan` varchar(50) NOT NULL,
  `subscription_expiry` datetime DEFAULT NULL,
  `is_premium` tinyint(4) NOT NULL,
  `last_payment_status` varchar(50) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `status` varchar(50) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `is_verified` tinyint(4) NOT NULL DEFAULT 0,
  `email_verification_token` varchar(100) DEFAULT NULL,
  `email_verification_token_expires_at` datetime DEFAULT NULL,
  `password_reset_token` varchar(100) DEFAULT NULL,
  `password_reset_token_expires_at` datetime DEFAULT NULL,
  `stripe_customer_id` varchar(100) DEFAULT NULL,
  `stripe_subscription_id` varchar(100) DEFAULT NULL,
  `is_banned` tinyint(4) NOT NULL DEFAULT 0,
  `banned_until` datetime DEFAULT NULL,
  `ban_reason` varchar(500) DEFAULT NULL,
  `banned_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`id`, `email`, `roles`, `password`, `subscription_plan`, `subscription_expiry`, `is_premium`, `last_payment_status`, `created_at`, `status`, `first_name`, `last_name`, `is_verified`, `email_verification_token`, `email_verification_token_expires_at`, `password_reset_token`, `password_reset_token_expires_at`, `stripe_customer_id`, `stripe_subscription_id`, `is_banned`, `banned_until`, `ban_reason`, `banned_at`) VALUES
(1, 'belhajhamdi185@gmail.com', '[\"ROLE_ADMIN\",\"ROLE_USER\"]', '$2y$13$xyUzJUAelo4zxW/xiXLLO.sZHNR25t46OAi3yc5kfv6Zj3t93dA1y', 'FREE', NULL, 0, NULL, '2026-02-25 15:23:13', 'active', 'Hamdi', 'Belhaj', 1, NULL, NULL, 'e3a9b0e4ad650cd0b2c554f779f6be18bfabc238ba5aa18d8a920bd95fcc92d8', '2026-03-05 10:14:58', NULL, NULL, 0, NULL, NULL, NULL),
(3, 'moenisgasmi2016@gmail.com', '[\"ROLE_TEACHER\",\"ROLE_ADMIN\"]', '$2y$13$QAkdaPRf5E49P1dRzRb17OelwIhylew1yV4YOBYKssjuEaPPB8Xfa', 'FREE', NULL, 0, NULL, '2026-02-25 23:08:55', 'active', 'MOHAMMAD', 'MOAHDI', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(124, 'mariem@gmail.com', '[\"ROLE_USER\"]', '$2y$13$Nd0hIKEVldF7xzWTItwE1OVTUTEcgup7/9.Xqe9Qju3GjNuQuDnma', 'YEARLY', '2027-02-26 09:04:34', 1, 'success', '2026-02-25 23:19:23', 'active', 'Mariem', 'Zayeni', 1, NULL, NULL, '1b647df4e841be82ad7891982339803643d3e15cc5fdd9127ba0f25208cb52d1', '2026-02-27 11:27:18', 'cus_U2wJGvbGEdnTeJ', 'sub_1T4zYfJdwesqnbiYGkfEBLZU', 0, NULL, NULL, NULL),
(127, 'johndoe@example.com', '[\"ROLE_USER\"]', '$2y$13$1uMyriVysW7SFvq00pOSqO.osLWIkXTbqaF41MpNepYxdSPM4JVry', 'FREE', NULL, 0, NULL, '2026-02-26 09:52:36', 'active', 'John', 'Doe', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(128, 'ahmed@esprit.tn', '[\"ROLE_USER\"]', '$2y$13$S4ynMaMv5knP6VmKR0rHjOM/gVkt0TPL06CpHBfZQPDs5ECHx02qS', 'YEARLY', '2027-03-02 23:44:09', 1, 'success', '2026-02-26 09:53:09', 'active', 'Ahmed', 'Mahmoud', 1, NULL, NULL, NULL, NULL, 'cus_U4opiT1uaYi7Kp', 'sub_1T6fC4JdwesqnbiYFTePKL96', 0, NULL, NULL, NULL),
(131, 'belhajhamdi100@gmail.com', '[\"ROLE_ADMIN\",\"ROLE_USER\"]', '$2y$13$iej7YU8WpvQdVUD/iqlKg.3qcusJk/8WmhJEXkO7/gah2JhgqriGK', 'FREE', NULL, 0, NULL, '2026-03-03 20:06:07', 'active', 'MOHAMMAD', 'MOAHDI', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(132, 'demo@test.com', '[\"ROLE_USER\"]', '$2y$13$Uffe5SPJQJxVT3IqhAQ5j.7.al1qGa.dvJfwsV1c5udQUBIbeUMSu', 'FREE', NULL, 1, NULL, '2026-03-03 23:06:08', 'active', 'Demo', 'User', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(133, 'takwa@gmail.com', '[\"ROLE_ADMIN\",\"ROLE_USER\"]', '$2y$13$6zWihKGRh.sqb5m1YgQ1feghtJZBKYgoLM/P0Rt1hHeQKzmJy4DHO', 'FREE', NULL, 1, NULL, '2026-03-04 00:59:01', 'active', 'takwa', 'belguith', 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(134, 'takwa@test.com', '[\"ROLE_ADMIN\",\"ROLE_USER\"]', '$2y$13$pJsGfNDMkpv5nVmaKRPt.eCphYHlHLipBAfh7UcmzAYtWCtDiU7gK', 'FREE', NULL, 1, NULL, '2026-03-04 01:02:24', 'active', 'Aymen', 'Touihri', 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(136, 'admin123@org.com', '[\"ROLE_USER\"]', '$2a$12$BTRx3WnGPDLI0Nl1L617gugQ6eliyQILC1E2H35ligEEZLo9ssXy6', 'FREE', NULL, 0, NULL, '2026-04-07 14:55:35', 'active', 'hamdi', 'belhaj', 0, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `user_language`
--

CREATE TABLE `user_language` (
  `id` int(11) NOT NULL,
  `proficiency_level` varchar(20) NOT NULL,
  `is_native` tinyint(4) NOT NULL,
  `added_at` datetime NOT NULL,
  `user_id` int(11) NOT NULL,
  `platform_language_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_language`
--

INSERT INTO `user_language` (`id`, `proficiency_level`, `is_native`, `added_at`, `user_id`, `platform_language_id`) VALUES
(2, 'A1', 0, '2026-02-26 08:47:43', 124, 1),
(3, 'A1', 0, '2026-03-02 23:48:18', 128, 3),
(4, 'A1', 0, '2026-03-02 23:48:25', 128, 1);

-- --------------------------------------------------------

--
-- Structure de la table `user_lesson_status`
--

CREATE TABLE `user_lesson_status` (
  `id` int(11) NOT NULL,
  `is_completed` tinyint(4) NOT NULL,
  `best_quiz_score` int(11) NOT NULL,
  `completed_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `lesson_id` int(11) NOT NULL,
  `last_quiz_score` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_lesson_status`
--

INSERT INTO `user_lesson_status` (`id`, `is_completed`, `best_quiz_score`, `completed_at`, `user_id`, `lesson_id`, `last_quiz_score`) VALUES
(1, 0, 40, NULL, 128, 2, 20);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `certificate`
--
ALTER TABLE `certificate`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UNIQ_219CDA4AB19D0B94` (`unique_code`),
  ADD KEY `IDX_219CDA4AA76ED395` (`user_id`),
  ADD KEY `IDX_219CDA4ACD56BC53` (`platform_language_id`);

--
-- Index pour la table `commentaire`
--
ALTER TABLE `commentaire`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_67F068BC38B217A7` (`publication_id`);

--
-- Index pour la table `course`
--
ALTER TABLE `course`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_169E6FB9F675F31B` (`author_id`),
  ADD KEY `IDX_169E6FB9CD56BC53` (`platform_language_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `exercice`
--
ALTER TABLE `exercice`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_E418C74D853CD175` (`quiz_id`);

--
-- Index pour la table `exercise_ai_feedback`
--
ALTER TABLE `exercise_ai_feedback`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_exercise_ai_feedback_user_attempt_exercise_prompt` (`user_id`,`quiz_attempt_id`,`exercise_id`,`prompt_hash`),
  ADD KEY `IDX_803DD2BCE934951A` (`exercise_id`),
  ADD KEY `idx_exercise_ai_feedback_attempt` (`quiz_attempt_id`),
  ADD KEY `idx_exercise_ai_feedback_user` (`user_id`);

--
-- Index pour la table `exercise_attempt`
--
ALTER TABLE `exercise_attempt`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_C2F9F523E934951A` (`exercise_id`),
  ADD KEY `idx_exercise_attempt_quiz_attempt` (`quiz_attempt_id`);

--
-- Index pour la table `faq`
--
ALTER TABLE `faq`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `forum_post`
--
ALTER TABLE `forum_post`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `forum_reply`
--
ALTER TABLE `forum_reply`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_E5DC60374B89032C` (`post_id`);

--
-- Index pour la table `languages`
--
ALTER TABLE `languages`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `learning_stats`
--
ALTER TABLE `learning_stats`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UNIQ_A9D52C90A76ED395` (`user_id`);

--
-- Index pour la table `lesson`
--
ALTER TABLE `lesson`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_F87474F3591CC992` (`course_id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `mock_test`
--
ALTER TABLE `mock_test`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_D9FB90A1CD56BC53` (`platform_language_id`);

--
-- Index pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_6000B0D3A76ED395` (`user_id`);

--
-- Index pour la table `platform_language`
--
ALTER TABLE `platform_language`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `publication`
--
ALTER TABLE `publication`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_AF3C6779A76ED395` (`user_id`);

--
-- Index pour la table `quiz`
--
ALTER TABLE `quiz`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_A412FA92CDF80196` (`lesson_id`);

--
-- Index pour la table `quiz_attempt`
--
ALTER TABLE `quiz_attempt`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_AB6AFC6A76ED395` (`user_id`),
  ADD KEY `IDX_AB6AFC6853CD175` (`quiz_id`),
  ADD KEY `IDX_AB6AFC6CDF80196` (`lesson_id`),
  ADD KEY `IDX_AB6AFC613A460D6` (`second_chance_exercise_id`),
  ADD KEY `idx_quiz_attempt_user_quiz` (`user_id`,`quiz_id`);

--
-- Index pour la table `quiz_schedule`
--
ALTER TABLE `quiz_schedule`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_8CFCE8BECB944F1A` (`student_id`),
  ADD KEY `IDX_8CFCE8BE853CD175` (`quiz_id`);

--
-- Index pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_CE606404A76ED395` (`user_id`);

--
-- Index pour la table `reclamation_audit`
--
ALTER TABLE `reclamation_audit`
  ADD PRIMARY KEY (`id`),
  ADD KEY `type_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`type`),
  ADD KEY `object_id_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`object_id`),
  ADD KEY `discriminator_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`discriminator`),
  ADD KEY `transaction_hash_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`transaction_hash`),
  ADD KEY `blame_id_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`blame_id`),
  ADD KEY `created_at_98d6d4200acdceaafb2260d0ea2a31e5_idx` (`created_at`);

--
-- Index pour la table `recommendation_session`
--
ALTER TABLE `recommendation_session`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_recommendation_session_user` (`user_id`);

--
-- Index pour la table `skill_profile`
--
ALTER TABLE `skill_profile`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_skill_profile_user_skill` (`user_id`,`skill_code`),
  ADD KEY `idx_skill_profile_user` (`user_id`);

--
-- Index pour la table `support_audit_logs`
--
ALTER TABLE `support_audit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_F49BEC7A2D6BA2D9` (`reclamation_id`),
  ADD KEY `IDX_F49BEC7A2E65C292` (`performed_by_id`);

--
-- Index pour la table `support_notifications`
--
ALTER TABLE `support_notifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_385347A0A76ED395` (`user_id`),
  ADD KEY `IDX_385347A02D6BA2D9` (`reclamation_id`);

--
-- Index pour la table `support_response`
--
ALTER TABLE `support_response`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_8ACD80C42D6BA2D9` (`reclamation_id`),
  ADD KEY `IDX_8ACD80C4F675F31B` (`author_id`);

--
-- Index pour la table `test_question`
--
ALTER TABLE `test_question`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_23944218E5D55330` (`mock_test_id`);

--
-- Index pour la table `test_result`
--
ALTER TABLE `test_result`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_84B3C63DE5D55330` (`mock_test_id`),
  ADD KEY `IDX_84B3C63DA76ED395` (`user_id`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UNIQ_1483A5E9E7927C74` (`email`);

--
-- Index pour la table `user_language`
--
ALTER TABLE `user_language`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_345695B5A76ED395` (`user_id`),
  ADD KEY `IDX_345695B5CD56BC53` (`platform_language_id`);

--
-- Index pour la table `user_lesson_status`
--
ALTER TABLE `user_lesson_status`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_E618381BA76ED395` (`user_id`),
  ADD KEY `IDX_E618381BCDF80196` (`lesson_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `certificate`
--
ALTER TABLE `certificate`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `commentaire`
--
ALTER TABLE `commentaire`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `course`
--
ALTER TABLE `course`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `exercice`
--
ALTER TABLE `exercice`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `exercise_ai_feedback`
--
ALTER TABLE `exercise_ai_feedback`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `exercise_attempt`
--
ALTER TABLE `exercise_attempt`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=44;

--
-- AUTO_INCREMENT pour la table `faq`
--
ALTER TABLE `faq`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `forum_post`
--
ALTER TABLE `forum_post`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `forum_reply`
--
ALTER TABLE `forum_reply`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `languages`
--
ALTER TABLE `languages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `learning_stats`
--
ALTER TABLE `learning_stats`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT pour la table `lesson`
--
ALTER TABLE `lesson`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mock_test`
--
ALTER TABLE `mock_test`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `platform_language`
--
ALTER TABLE `platform_language`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `publication`
--
ALTER TABLE `publication`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `quiz`
--
ALTER TABLE `quiz`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `quiz_attempt`
--
ALTER TABLE `quiz_attempt`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `quiz_schedule`
--
ALTER TABLE `quiz_schedule`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT pour la table `reclamation_audit`
--
ALTER TABLE `reclamation_audit`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT pour la table `recommendation_session`
--
ALTER TABLE `recommendation_session`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `skill_profile`
--
ALTER TABLE `skill_profile`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `support_audit_logs`
--
ALTER TABLE `support_audit_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT pour la table `support_notifications`
--
ALTER TABLE `support_notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `support_response`
--
ALTER TABLE `support_response`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `test_question`
--
ALTER TABLE `test_question`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `test_result`
--
ALTER TABLE `test_result`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=137;

--
-- AUTO_INCREMENT pour la table `user_language`
--
ALTER TABLE `user_language`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `user_lesson_status`
--
ALTER TABLE `user_lesson_status`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `certificate`
--
ALTER TABLE `certificate`
  ADD CONSTRAINT `FK_219CDA4AA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK_219CDA4ACD56BC53` FOREIGN KEY (`platform_language_id`) REFERENCES `platform_language` (`id`);

--
-- Contraintes pour la table `commentaire`
--
ALTER TABLE `commentaire`
  ADD CONSTRAINT `FK_67F068BC38B217A7` FOREIGN KEY (`publication_id`) REFERENCES `publication` (`id`);

--
-- Contraintes pour la table `course`
--
ALTER TABLE `course`
  ADD CONSTRAINT `FK_169E6FB9CD56BC53` FOREIGN KEY (`platform_language_id`) REFERENCES `platform_language` (`id`),
  ADD CONSTRAINT `FK_169E6FB9F675F31B` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `exercice`
--
ALTER TABLE `exercice`
  ADD CONSTRAINT `FK_E418C74D853CD175` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `exercise_ai_feedback`
--
ALTER TABLE `exercise_ai_feedback`
  ADD CONSTRAINT `FK_803DD2BCA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_803DD2BCE934951A` FOREIGN KEY (`exercise_id`) REFERENCES `exercice` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_803DD2BCF8FE9957` FOREIGN KEY (`quiz_attempt_id`) REFERENCES `quiz_attempt` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `exercise_attempt`
--
ALTER TABLE `exercise_attempt`
  ADD CONSTRAINT `FK_C2F9F523E934951A` FOREIGN KEY (`exercise_id`) REFERENCES `exercice` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_C2F9F523F8FE9957` FOREIGN KEY (`quiz_attempt_id`) REFERENCES `quiz_attempt` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `forum_reply`
--
ALTER TABLE `forum_reply`
  ADD CONSTRAINT `FK_E5DC60374B89032C` FOREIGN KEY (`post_id`) REFERENCES `forum_post` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `learning_stats`
--
ALTER TABLE `learning_stats`
  ADD CONSTRAINT `FK_A9D52C90A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `lesson`
--
ALTER TABLE `lesson`
  ADD CONSTRAINT `FK_F87474F3591CC992` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`);

--
-- Contraintes pour la table `mock_test`
--
ALTER TABLE `mock_test`
  ADD CONSTRAINT `FK_D9FB90A1CD56BC53` FOREIGN KEY (`platform_language_id`) REFERENCES `platform_language` (`id`);

--
-- Contraintes pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `FK_6000B0D3A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `publication`
--
ALTER TABLE `publication`
  ADD CONSTRAINT `FK_AF3C6779A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `quiz`
--
ALTER TABLE `quiz`
  ADD CONSTRAINT `FK_A412FA92CDF80196` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `quiz_attempt`
--
ALTER TABLE `quiz_attempt`
  ADD CONSTRAINT `FK_AB6AFC613A460D6` FOREIGN KEY (`second_chance_exercise_id`) REFERENCES `exercice` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_AB6AFC6853CD175` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_AB6AFC6A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_AB6AFC6CDF80196` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `quiz_schedule`
--
ALTER TABLE `quiz_schedule`
  ADD CONSTRAINT `FK_8CFCE8BE853CD175` FOREIGN KEY (`quiz_id`) REFERENCES `quiz` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_8CFCE8BECB944F1A` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD CONSTRAINT `FK_CE606404A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `recommendation_session`
--
ALTER TABLE `recommendation_session`
  ADD CONSTRAINT `FK_8D27DC78A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `skill_profile`
--
ALTER TABLE `skill_profile`
  ADD CONSTRAINT `FK_9BA23426A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `support_audit_logs`
--
ALTER TABLE `support_audit_logs`
  ADD CONSTRAINT `FK_F49BEC7A2D6BA2D9` FOREIGN KEY (`reclamation_id`) REFERENCES `reclamation` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `FK_F49BEC7A2E65C292` FOREIGN KEY (`performed_by_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Contraintes pour la table `support_notifications`
--
ALTER TABLE `support_notifications`
  ADD CONSTRAINT `FK_385347A02D6BA2D9` FOREIGN KEY (`reclamation_id`) REFERENCES `reclamation` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `FK_385347A0A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Contraintes pour la table `support_response`
--
ALTER TABLE `support_response`
  ADD CONSTRAINT `FK_8ACD80C42D6BA2D9` FOREIGN KEY (`reclamation_id`) REFERENCES `reclamation` (`id`),
  ADD CONSTRAINT `FK_8ACD80C4F675F31B` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`);

--
-- Contraintes pour la table `test_question`
--
ALTER TABLE `test_question`
  ADD CONSTRAINT `FK_23944218E5D55330` FOREIGN KEY (`mock_test_id`) REFERENCES `mock_test` (`id`);

--
-- Contraintes pour la table `test_result`
--
ALTER TABLE `test_result`
  ADD CONSTRAINT `FK_84B3C63DA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK_84B3C63DE5D55330` FOREIGN KEY (`mock_test_id`) REFERENCES `mock_test` (`id`);

--
-- Contraintes pour la table `user_language`
--
ALTER TABLE `user_language`
  ADD CONSTRAINT `FK_345695B5A76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK_345695B5CD56BC53` FOREIGN KEY (`platform_language_id`) REFERENCES `platform_language` (`id`);

--
-- Contraintes pour la table `user_lesson_status`
--
ALTER TABLE `user_lesson_status`
  ADD CONSTRAINT `FK_E618381BA76ED395` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK_E618381BCDF80196` FOREIGN KEY (`lesson_id`) REFERENCES `lesson` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
