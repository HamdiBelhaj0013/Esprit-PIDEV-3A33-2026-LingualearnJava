package org.example.mains;

import org.example.entities.pedagogicalcontent.Course;
import org.example.entities.pedagogicalcontent.Lesson;
import org.example.entities.pedagogicalcontent.PlatformLanguage;
import org.example.service.CourseService;
import org.example.service.LessonService;
import org.example.service.PlatformLanguageService;

import java.sql.Timestamp;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        PlatformLanguageService languageService = new PlatformLanguageService();
        CourseService courseService = new CourseService();
        LessonService lessonService = new LessonService();

        try {
            System.out.println("========== TEST CRUD PLATFORM LANGUAGE ==========");

            PlatformLanguage language = new PlatformLanguage(
                    "English",
                    "EN",
                    "english.png",
                    true
            );

            languageService.add(language);
            System.out.println("PlatformLanguage ajouté.");

            List<PlatformLanguage> languages = languageService.getAll();
            System.out.println("Liste des langues :");
            for (PlatformLanguage l : languages) {
                System.out.println("ID=" + l.getId() +
                        ", name=" + l.getName() +
                        ", code=" + l.getCode() +
                        ", flag=" + l.getFlagUrl() +
                        ", enabled=" + l.isEnabled());
            }

            PlatformLanguage lastLanguage = languages.get(languages.size() - 1);

            lastLanguage.setName("French");
            lastLanguage.setCode("FR");
            lastLanguage.setFlagUrl("france.png");
            lastLanguage.setEnabled(true);
            languageService.update(lastLanguage);
            System.out.println("PlatformLanguage modifié.");

            PlatformLanguage updatedLanguage = languageService.getById(lastLanguage.getId());
            System.out.println("Après modification : " +
                    updatedLanguage.getId() + " - " +
                    updatedLanguage.getName() + " - " +
                    updatedLanguage.getCode());

            System.out.println("\n========== TEST CRUD COURSE ==========");

            Course course = new Course(
                    "Java Basics",
                    "Beginner",
                    "Published",
                    new Timestamp(System.currentTimeMillis()),
                    null,
                    updatedLanguage.getId()
            );

            courseService.add(course);
            System.out.println("Course ajouté.");

            List<Course> courses = courseService.getAll();
            System.out.println("Liste des cours :");
            for (Course c : courses) {
                System.out.println("ID=" + c.getId() +
                        ", title=" + c.getTitle() +
                        ", level=" + c.getLevel() +
                        ", status=" + c.getStatus() +
                        ", publishedAt=" + c.getPublishedAt() +
                        ", authorId=" + c.getAuthorId() +
                        ", platformLanguageId=" + c.getPlatformLanguageId());
            }

            Course lastCourse = courses.get(courses.size() - 1);

            lastCourse.setTitle("Advanced Java");
            lastCourse.setLevel("Intermediate");
            lastCourse.setStatus("Draft");
            courseService.update(lastCourse);
            System.out.println("Course modifié.");

            Course updatedCourse = courseService.getById(lastCourse.getId());
            System.out.println("Après modification : " +
                    updatedCourse.getId() + " - " +
                    updatedCourse.getTitle() + " - " +
                    updatedCourse.getLevel());

            System.out.println("\n========== TEST CRUD LESSON ==========");

            Lesson lesson = new Lesson(
                    "Introduction to Java",
                    "This is the lesson content",
                    "[\"class\", \"object\", \"method\"]",
                    "{\"grammar\":\"present simple\"}",
                    50,
                    updatedCourse.getId(),
                    "intro.mp4",
                    "intro.jpg",
                    "intro.pdf",
                    new Timestamp(System.currentTimeMillis())
            );

            lessonService.add(lesson);
            System.out.println("Lesson ajoutée.");

            List<Lesson> lessons = lessonService.getAll();
            System.out.println("Liste des leçons :");
            for (Lesson le : lessons) {
                System.out.println("ID=" + le.getId() +
                        ", title=" + le.getTitle() +
                        ", xpReward=" + le.getXpReward() +
                        ", courseId=" + le.getCourseId() +
                        ", updatedAt=" + le.getUpdatedAt());
            }

            Lesson lastLesson = lessons.get(lessons.size() - 1);

            lastLesson.setTitle("Java Variables");
            lastLesson.setXpReward(80);
            lastLesson.setVocabularyData("[\"variable\", \"type\", \"value\"]");
            lastLesson.setGrammarData("{\"grammar\":\"past simple\"}");
            lessonService.update(lastLesson);
            System.out.println("Lesson modifiée.");

            Lesson updatedLesson = lessonService.getById(lastLesson.getId());
            System.out.println("Après modification : " +
                    updatedLesson.getId() + " - " +
                    updatedLesson.getTitle() + " - XP=" +
                    updatedLesson.getXpReward());
            System.out.println("\n========== TEST DELETE LESSON ==========");
            lessonService.delete(updatedLesson.getId());
            System.out.println("Lesson supprimée.");

            System.out.println("\n========== TEST DELETE COURSE ==========");
            courseService.delete(updatedCourse.getId());
            System.out.println("Course supprimé.");

            System.out.println("\n========== TEST DELETE PLATFORM LANGUAGE ==========");
            languageService.delete(updatedLanguage.getId());
            System.out.println("PlatformLanguage supprimé.");

            System.out.println("\n========== FIN DES TESTS ==========");

        } catch (Exception e) {
            System.out.println("Erreur détectée : " + e.getMessage());
            e.printStackTrace();
        }
    }
}