-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : lun. 13 avr. 2026 à 16:44
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
-- Base de données : `1lingualearn_db`
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
('DoctrineMigrations\\Version20260413143117', '2026-04-13 16:31:55', 5139);

-- --------------------------------------------------------

--
-- Structure de la table `exercice`
--

CREATE TABLE `exercice` (
  `id` int(11) NOT NULL,
  `difficulty` smallint(6) NOT NULL DEFAULT 3,
  `type` varchar(50) NOT NULL,
  `question` longtext NOT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`options`)),
  `correct_answer` varchar(255) NOT NULL,
  `skill_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '[]' CHECK (json_valid(`skill_codes`)),
  `ai_generated` tinyint(4) NOT NULL,
  `enabled` tinyint(4) NOT NULL,
  `quiz_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `exercice`
--

INSERT INTO `exercice` (`id`, `difficulty`, `type`, `question`, `options`, `correct_answer`, `skill_codes`, `ai_generated`, `enabled`, `quiz_id`) VALUES
(1, 2, 'true_false', 'HDOSQDK', '[\"SQKKDJ\",\"WSXQ\"]', 'SQKKDJ', '[\"Sd\",\"SQDQ\"]', 0, 1, 1);

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
  `video_name` varchar(255) DEFAULT NULL,
  `thumb_name` varchar(255) DEFAULT NULL,
  `resource_name` varchar(255) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `course_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `title` varchar(255) NOT NULL,
  `test_type` varchar(50) NOT NULL,
  `test_category` varchar(50) NOT NULL DEFAULT 'QCM',
  `level` varchar(50) NOT NULL DEFAULT 'Beginner',
  `duration_minutes` int(11) NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `platform_language_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `difficulty` smallint(6) NOT NULL DEFAULT 3,
  `skill_codes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '[]' CHECK (json_valid(`skill_codes`)),
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `enabled` tinyint(4) NOT NULL,
  `lesson_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `quiz`
--

INSERT INTO `quiz` (`id`, `title`, `description`, `passing_score`, `question_count`, `difficulty`, `skill_codes`, `created_at`, `updated_at`, `enabled`, `lesson_id`) VALUES
(1, 'TEST', 'GHKFHKH', 6, 7, 3, '[\"OK\"]', '2026-04-13 15:35:32', '2026-04-13 15:35:32', 1, NULL);

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
  `priority` varchar(10) NOT NULL,
  `sla_deadline` datetime DEFAULT NULL,
  `is_late` tinyint(4) NOT NULL DEFAULT 0,
  `resolved_at` datetime DEFAULT NULL,
  `satisfaction_score` int(11) DEFAULT NULL,
  `satisfaction_comment` longtext DEFAULT NULL,
  `satisfaction_rated_at` datetime DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

-- --------------------------------------------------------

--
-- Structure de la table `test_question`
--

CREATE TABLE `test_question` (
  `id` int(11) NOT NULL,
  `section_category` varchar(100) NOT NULL,
  `question_type` varchar(50) NOT NULL DEFAULT 'qcm',
  `question_text` longtext NOT NULL,
  `reading_passage` longtext DEFAULT NULL,
  `audio_text` longtext DEFAULT NULL,
  `writing_subject` longtext DEFAULT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`options`)),
  `correct_answer` longtext DEFAULT NULL,
  `points` int(11) NOT NULL,
  `is_active` tinyint(4) NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `mock_test_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `email` varchar(180) NOT NULL,
  `roles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`roles`)),
  `is_banned` tinyint(4) NOT NULL DEFAULT 0,
  `banned_until` datetime DEFAULT NULL,
  `ban_reason` varchar(255) DEFAULT NULL,
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
  `stripe_subscription_id` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

-- --------------------------------------------------------

--
-- Structure de la table `user_lesson_status`
--

CREATE TABLE `user_lesson_status` (
  `id` int(11) NOT NULL,
  `is_completed` tinyint(4) NOT NULL,
  `best_quiz_score` int(11) NOT NULL,
  `last_quiz_score` int(11) NOT NULL,
  `completed_at` datetime DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `lesson_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `course`
--
ALTER TABLE `course`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `exercice`
--
ALTER TABLE `exercice`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `exercise_ai_feedback`
--
ALTER TABLE `exercise_ai_feedback`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `exercise_attempt`
--
ALTER TABLE `exercise_attempt`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

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
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `lesson`
--
ALTER TABLE `lesson`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mock_test`
--
ALTER TABLE `mock_test`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `platform_language`
--
ALTER TABLE `platform_language`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `quiz`
--
ALTER TABLE `quiz`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `quiz_attempt`
--
ALTER TABLE `quiz_attempt`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `quiz_schedule`
--
ALTER TABLE `quiz_schedule`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `reclamation_audit`
--
ALTER TABLE `reclamation_audit`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `recommendation_session`
--
ALTER TABLE `recommendation_session`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `skill_profile`
--
ALTER TABLE `skill_profile`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `support_audit_logs`
--
ALTER TABLE `support_audit_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `support_notifications`
--
ALTER TABLE `support_notifications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `support_response`
--
ALTER TABLE `support_response`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `test_question`
--
ALTER TABLE `test_question`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `test_result`
--
ALTER TABLE `test_result`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user_language`
--
ALTER TABLE `user_language`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user_lesson_status`
--
ALTER TABLE `user_lesson_status`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

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
