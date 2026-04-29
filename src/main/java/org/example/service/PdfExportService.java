package org.example.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.entities.pedagogicalcontent.Lesson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final float MARGIN = 50;
    private static final float START_Y = 750;
    private static final float BOTTOM_Y = 50;
    private static final float LINE_HEIGHT = 15;
    private static final int MAX_CHARS_PER_LINE = 88;

    public String exportLessonToPdf(Lesson lesson) {
        if (lesson == null) {
            System.err.println("Lesson is null");
            return null;
        }

        try {
            Files.createDirectories(Paths.get("exports/pdf"));

            String safeTitle = safeFileName(lesson.getTitle());
            String filename = "exports/pdf/Lesson_" + safeTitle + "_" + System.currentTimeMillis() + ".pdf";

            try (PDDocument document = new PDDocument()) {
                PdfWriter writer = new PdfWriter(document);

                writer.writeTitle(cleanPdfText(lesson.getTitle()), 24);
                writer.writeLine("XP Reward: " + lesson.getXpReward(), FONT, 12);
                writer.writeLine("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), FONT, 12);
                writer.addSpace(15);

                writer.writeTitle("CONTENT", 14);
                String content = lesson.getContent();
                if (content == null || content.isBlank()) {
                    writer.writeWrappedText("No content available.", FONT, 11);
                } else {
                    writer.writeWrappedText(content, FONT, 11);
                }

                writer.close();
                document.save(filename);
            }

            System.out.println("PDF exported: " + filename);
            return filename;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String exportCourseToPdf(String courseTitle, List<Lesson> lessons) {
        try {
            Files.createDirectories(Paths.get("exports/pdf"));

            String safeTitle = safeFileName(courseTitle);
            String filename = "exports/pdf/Course_" + safeTitle + "_" + System.currentTimeMillis() + ".pdf";

            try (PDDocument document = new PDDocument()) {
                PdfWriter writer = new PdfWriter(document);

                writer.writeTitle(cleanPdfText(courseTitle), 28);
                writer.writeLine("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), FONT, 10);
                writer.addSpace(25);

                if (lessons == null || lessons.isEmpty()) {
                    writer.writeWrappedText("No lessons available.", FONT, 11);
                } else {
                    for (Lesson lesson : lessons) {
                        writer.writeTitle(cleanPdfText(lesson.getTitle()), 15);
                        writer.writeWrappedText(lesson.getContent() == null ? "No content available." : lesson.getContent(), FONT, 11);
                        writer.addSpace(12);
                    }
                }

                writer.close();
                document.save(filename);
            }

            System.out.println("Course PDF exported: " + filename);
            return filename;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String safeFileName(String text) {
        String value = text == null || text.isBlank() ? "untitled" : text.trim();
        value = value.replaceAll("[^a-zA-Z0-9_-]", "_");
        value = value.replaceAll("_+", "_");
        return value.length() > 50 ? value.substring(0, 50) : value;
    }

    private static String cleanPdfText(String text) {
        if (text == null) return "";

        return text
                .replace("’", "'")
                .replace("‘", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("–", "-")
                .replace("—", "-")
                .replace("…", "...")
                .replaceAll("[^\\x20-\\x7EÀ-ÿ]", "");
    }

    private static List<String> wrapText(String text, int maxChars) {
        String cleaned = cleanPdfText(text).replace("\r", "\n");
        return cleaned.lines()
                .flatMap(line -> wrapSingleLine(line, maxChars).stream())
                .toList();
    }

    private static List<String> wrapSingleLine(String line, int maxChars) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (line == null || line.isBlank()) {
            result.add("");
            return result;
        }

        String remaining = line.trim();
        while (remaining.length() > maxChars) {
            int cut = remaining.lastIndexOf(' ', maxChars);
            if (cut <= 0) cut = maxChars;
            result.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isEmpty()) result.add(remaining);
        return result;
    }

    private static class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void writeTitle(String text, int size) throws IOException {
            addSpace(5);
            writeLine(text == null || text.isBlank() ? "Untitled" : text, FONT_BOLD, size);
            addSpace(10);
        }

        void writeWrappedText(String text, PDFont font, int size) throws IOException {
            for (String line : wrapText(text, MAX_CHARS_PER_LINE)) {
                writeLine(line, font, size);
            }
        }

        void writeLine(String text, PDFont font, int size) throws IOException {
            if (y < BOTTOM_Y) newPage();

            stream.setFont(font, size);
            stream.beginText();
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(cleanPdfText(text));
            stream.endText();
            y -= LINE_HEIGHT;
        }

        void addSpace(float space) throws IOException {
            y -= space;
            if (y < BOTTOM_Y) newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = START_Y;
        }

        void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
