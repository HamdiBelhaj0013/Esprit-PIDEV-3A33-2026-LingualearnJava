# 📊 LinguaLearn Admin Statistics Module

This module provides a comprehensive, real-time analytics dashboard for platform administrators to monitor user engagement, content distribution, and pedagogical performance.

## 🚀 Key Features

### 1. Dynamic KPIs
- **Active Students**: Total number of users currently enrolled or active based on study sessions.
- **Master Courses**: Total count of published courses on the platform.
- **Total Lessons**: Aggregated number of lessons across all courses.
- **Community XP**: Total experience points earned by all users.
- **Avg Quiz Score**: Global average performance across all quiz attempts.

### 2. Advanced Visualizations
- **Content Distribution (Pie Chart)**: Breaks down the percentage of courses available per language.
- **Course Depth (Bar Chart)**: Visualizes the number of lessons per course for the top 10 most substantial courses.
- **Subscription Split (Pie Chart)**: Compares the number of Premium vs. Free users.

### 3. Student Leaderboard
- A real-time ranking of top students based on XP, featuring:
  - Medal emojis for top ranks (🥇, 🥈, 🥉).
  - Progress bars proportional to the leader's XP.
  - Granular XP counts.

### 4. Interactive Elements
- **Refresh Hub**: Synchronizes data with the database and rotates the color palette.
- **Color Randomization**: Toggles between "Bright" and "Muted" modes on refresh to maintain a dynamic and premium feel.

## 🛠 Technical Implementation

- **View**: `backoffice-stats.fxml` - A responsive, card-based layout using JavaFX.
- **Controller**: `BackofficeStatsController.java` - Manages UI binding, chart population, and dynamic styling.
- **Service**: `BackofficeStatsService.java` - Handles complex SQL aggregations and data retrieval from the database.
- **Styles**: Custom CSS integrated into `backoffice.css` for a premium, modern aesthetic.

## 💾 Database Schema Utilization
The dashboard queries the following tables:
- `users`: For student counts and premium status.
- `course`: For content volume and distribution.
- `lesson`: For depth analysis.
- `learning_stats`: For XP aggregation and user performance.
- `quiz_attempt`: For activity trends and average scores.
- `platform_language`: For language-specific breakdowns.

## 📝 Usage Notes
- To test the dashboard with sample data, run the `populate_stats_test.sql` script.
- If changes to FXML/Java aren't appearing, use the `sync-stats.bat` helper script to force a resource sync to the `target/classes` directory.
