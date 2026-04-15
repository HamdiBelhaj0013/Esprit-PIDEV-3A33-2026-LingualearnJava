-- ========================================================
-- LinguaLearn - Sample Data Seeding for Statistics Testing
-- ========================================================

-- 1. Add some diversity to Platform Languages if not already there
INSERT IGNORE INTO `platform_language` (`id`, `name`, `code`, `flag_url`, `is_enabled`) VALUES
(4, 'Espagnol', 'es', 'https://flagpedia.net/data/flags/w580/es.png', 1),
(5, 'Allemand', 'de', 'https://flagpedia.net/data/flags/w580/de.png', 1),
(6, 'Italien', 'it', 'https://flagpedia.net/data/flags/w580/it.png', 1);

-- 2. Add some test users
-- Note: 'roles' and 'password' are dummy values for testing stats
INSERT IGNORE INTO `users` (`id`, `email`, `roles`, `password`, `subscription_plan`, `is_premium`, `created_at`, `status`, `first_name`, `last_name`, `is_verified`) VALUES
(1001, 'sofia@test.com', '["ROLE_USER"]', 'secret', 'basic', 0, '2026-04-01 10:00:00', 'active', 'Sofia', 'Rodriguez', 1),
(1002, 'marco@test.com', '["ROLE_USER"]', 'secret', 'premium', 1, '2026-04-05 11:30:00', 'active', 'Marco', 'Rossi', 1),
(1003, 'elena@test.com', '["ROLE_USER"]', 'secret', 'premium', 1, '2026-04-08 09:15:00', 'active', 'Elena', 'Schwarz', 1),
(1004, 'youssef@test.com', '["ROLE_USER"]', 'secret', 'basic', 0, '2026-04-10 14:00:00', 'active', 'Youssef', 'Alami', 1),
(1005, 'lisa@test.com', '["ROLE_USER"]', 'secret', 'basic', 0, '2026-04-12 16:45:00', 'active', 'Lisa', 'Dubois', 1);

-- 3. Add Statistics for these users
INSERT IGNORE INTO `learning_stats` (`user_id`, `total_minutes_studied`, `words_learned`, `total_xp`, `last_study_session`) VALUES
(1001, 120, 45, 1200, '2026-04-15 09:00:00'),
(1002, 350, 120, 3400, '2026-04-14 18:30:00'),
(1003, 50, 20, 450, '2026-04-15 11:00:00'),
(1004, 800, 300, 8500, '2026-04-15 13:00:00'),
(1005, 10, 5, 100, '2026-04-13 10:00:00');

-- 4. Add more Courses to see variation in content distribution
INSERT IGNORE INTO `course` (`id`, `title`, `level`, `status`, `published_at`, `author_id`, `platform_language_id`) VALUES
(10, 'Español Básico', 'beginner', 'published', '2026-04-01 12:00:00', 1, 4),
(11, 'Gramática de Oro', 'intermediate', 'published', '2026-04-02 12:00:00', 1, 4),
(12, 'Deutsch für Anfänger', 'beginner', 'published', '2026-04-03 12:00:00', 1, 5),
(13, 'Parliamo Italiano', 'beginner', 'published', '2026-04-04 12:00:00', 1, 6);

-- 5. Add Lessons to these courses
INSERT IGNORE INTO `lesson` (`id`, `title`, `content`, `vocabulary_data`, `grammar_data`, `xp_reward`, `course_id`) VALUES
(101, 'Los Saludos', 'Contenu...', '[]', '[]', 50, 10),
(102, 'La Familia', 'Contenu...', '[]', '[]', 50, 10),
(103, 'A1 Starten', 'Contenu...', '[]', '[]', 60, 12),
(104, 'Der Artikel', 'Contenu...', '[]', '[]', 60, 12),
(105, 'Die Verben', 'Contenu...', '[]', '[]', 60, 12);

-- 6. Add Quiz Attempts to create a 7-day activity trend
-- (Simulating attempts over the last few days)
INSERT IGNORE INTO `quiz_attempt` (`score`, `state`, `attempt_number`, `created_at`, `user_id`, `quiz_id`, `best_score`) VALUES
(80, 'finished', 1, '2026-04-09 10:00:00', 1001, 1, 80),
(90, 'finished', 1, '2026-04-10 11:00:00', 1002, 1, 90),
(70, 'finished', 1, '2026-04-11 09:00:00', 1004, 1, 70),
(85, 'finished', 1, '2026-04-12 15:00:00', 1002, 1, 85),
(95, 'finished', 1, '2026-04-13 17:00:00', 1001, 1, 95),
(60, 'finished', 1, '2026-04-14 10:30:00', 1003, 1, 60),
(100, 'finished', 1, '2026-04-15 12:00:00', 1004, 1, 100);
