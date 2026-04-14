package org.example.controllers.backoffice;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.example.entities.Publication;
import org.example.entities.Commentaire;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.services.ServiceCommentaire;
import org.example.services.ServicePublication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Button btnDashboard;
    @FXML private Button btnPublications;
    @FXML private StackPane contentArea;
    
    @FXML private Label totalPublications;
    @FXML private Label totalCommentaires;
    @FXML private Label totalLikes;
    @FXML private Label totalReports;
    
    @FXML private PieChart pubsByTypeChart;
    @FXML private BarChart<String, Number> topLikedChart;
    @FXML private BarChart<String, Number> commentsPerPubChart;
    @FXML private BarChart<String, Number> mostReportedChart;

    private final ServicePublication servicePublication = new ServicePublication();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private Node dashboardContent;

    @FXML
    public void initialize() {
        // Sauvegarder le contenu initial du dashboard
        if (!contentArea.getChildren().isEmpty()) {
            dashboardContent = contentArea.getChildren().get(0);
        }
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    public void showDashboard() {
        setActiveBtn(btnDashboard);
        // Réafficher le contenu dashboard (KPI + Charts)
        if (dashboardContent != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(dashboardContent);
        }
        refreshStats();
        updateCharts();
    }

    @FXML
    public void showPublications() {
        setActiveBtn(btnPublications);
        loadView("/backoffice/fxml/publication_manager.fxml");
    }

    @FXML
    public void handleRefresh() {
        refreshStats();
        updateCharts();
    }

    private void refreshStats() {
        try {
            List<Publication> pubs = servicePublication.getAll();
            List<Commentaire> coms = serviceCommentaire.getAll();

            int likes = pubs.stream().mapToInt(Publication::getLikes).sum();
            int reports = pubs.stream().mapToInt(Publication::getReportPub).sum();

            totalPublications.setText(String.valueOf(pubs.size()));
            totalCommentaires.setText(String.valueOf(coms.size()));
            totalLikes.setText(String.valueOf(likes));
            totalReports.setText(String.valueOf(reports));
        } catch (Exception e) {
            System.err.println("Erreur stats: " + e.getMessage());
            showError("Erreur", "Impossible de charger les statistiques");
        }
    }

    private void updateCharts() {
        try {
            List<Publication> pubs = servicePublication.getAll();
            List<Commentaire> coms = serviceCommentaire.getAll();

            // Pie Chart: Publications by Type
            if (!pubs.isEmpty()) {
                Map<String, Long> typeCount = pubs.stream()
                        .collect(Collectors.groupingBy(p -> p.getTypePub() != null ? p.getTypePub() : "Unknown", Collectors.counting()));
                
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                typeCount.forEach((type, count) -> pieData.add(new PieChart.Data(type + " (" + count + ")", count)));
                pubsByTypeChart.setData(pieData);
            }

            // Top 5 Liked Publications
            if (!pubs.isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Likes");
                pubs.stream()
                        .sorted((a, b) -> Integer.compare(b.getLikes(), a.getLikes()))
                        .limit(5)
                        .forEach(p -> series.getData().add(new XYChart.Data<>(
                                p.getTitrePub() != null && p.getTitrePub().length() > 15 
                                    ? p.getTitrePub().substring(0, 15) + "..." 
                                    : p.getTitrePub(),
                                p.getLikes()
                        )));
                topLikedChart.getData().clear();
                topLikedChart.getData().add(series);
            }

            // Comments per Publication (Top 5)
            if (!coms.isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Comments");
                Map<Integer, Long> comCount = coms.stream()
                        .collect(Collectors.groupingBy(Commentaire::getPublicationId, Collectors.counting()));
                
                comCount.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(5)
                        .forEach(entry -> {
                            String label = "Pub #" + entry.getKey();
                            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
                        });
                
                commentsPerPubChart.getData().clear();
                commentsPerPubChart.getData().add(series);
            }

            // Most Reported Publications
            if (!pubs.isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Reports");
                pubs.stream()
                        .filter(p -> p.getReportPub() > 0)
                        .sorted((a, b) -> Integer.compare(b.getReportPub(), a.getReportPub()))
                        .limit(5)
                        .forEach(p -> series.getData().add(new XYChart.Data<>(
                                p.getTitrePub() != null && p.getTitrePub().length() > 15 
                                    ? p.getTitrePub().substring(0, 15) + "..." 
                                    : p.getTitrePub(),
                                p.getReportPub()
                        )));
                mostReportedChart.getData().clear();
                if (!series.getData().isEmpty()) {
                    mostReportedChart.getData().add(series);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur charts: " + e.getMessage());
        }
    }

    @FXML
    public void exportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("export_publications.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        
        // Get window from any node
        Window window = null;
        if (btnDashboard.getScene() != null) {
            window = btnDashboard.getScene().getWindow();
        }
        
        File file = fc.showSaveDialog(window);
        if (file == null) return;

        try {
            List<Publication> pubs = servicePublication.getAll();
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            doc.add(new Paragraph("LinguaLearn - Export Publications").setFontSize(16).setBold());
            doc.add(new Paragraph("Total : " + pubs.size() + " publication(s)").setFontSize(11));
            doc.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 1, 1}))
                    .useAllAvailableWidth();

            for (String h : new String[]{"ID", "Titre", "Contenu", "Likes", "Date"}) {
                table.addHeaderCell(new Cell().add(new Paragraph(h).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }

            for (Publication p : pubs) {
                table.addCell(String.valueOf(p.getId()));
                table.addCell(p.getTitrePub() != null ? p.getTitrePub() : "");
                String contenu = p.getContenuPub() != null ? p.getContenuPub() : "";
                table.addCell(contenu.length() > 60 ? contenu.substring(0, 60) + "..." : contenu);
                table.addCell(String.valueOf(p.getLikes()));
                table.addCell(p.getDatePub() != null ? p.getDatePub().toLocalDate().toString() : "");
            }

            doc.add(table);
            doc.close();
            showInfo("Export PDF", "Fichier sauvegarde : " + file.getName());
        } catch (Exception e) {
            showError("Erreur export PDF", e.getMessage());
        }
    }

    @FXML
    public void exportExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le fichier Excel");
        fc.setInitialFileName("export_publications.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        
        Window window = null;
        if (btnDashboard.getScene() != null) {
            window = btnDashboard.getScene().getWindow();
        }
        
        File file = fc.showSaveDialog(window);
        if (file == null) return;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            List<Publication> pubs = servicePublication.getAll();
            List<Commentaire> coms = serviceCommentaire.getAll();

            Sheet pubSheet = workbook.createSheet("Publications");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row pubHeader = pubSheet.createRow(0);
            String[] pubCols = {"ID", "Titre", "Type", "Contenu", "Date", "Likes", "Dislikes", "Signalements"};
            for (int i = 0; i < pubCols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = pubHeader.createCell(i);
                cell.setCellValue(pubCols[i]);
                cell.setCellStyle(headerStyle);
                pubSheet.setColumnWidth(i, 5000);
            }
            int rowNum = 1;
            for (Publication p : pubs) {
                Row row = pubSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getTitrePub() != null ? p.getTitrePub() : "");
                row.createCell(2).setCellValue(p.getTypePub() != null ? p.getTypePub() : "");
                row.createCell(3).setCellValue(p.getContenuPub() != null ? p.getContenuPub() : "");
                row.createCell(4).setCellValue(p.getDatePub() != null ? p.getDatePub().toString() : "");
                row.createCell(5).setCellValue(p.getLikes());
                row.createCell(6).setCellValue(p.getDislikes());
                row.createCell(7).setCellValue(p.getReportPub());
            }

            Sheet comSheet = workbook.createSheet("Commentaires");
            Row comHeader = comSheet.createRow(0);
            String[] comCols = {"ID", "Contenu", "Date", "Publication ID"};
            for (int i = 0; i < comCols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = comHeader.createCell(i);
                cell.setCellValue(comCols[i]);
                cell.setCellStyle(headerStyle);
                comSheet.setColumnWidth(i, 6000);
            }
            rowNum = 1;
            for (Commentaire c : coms) {
                Row row = comSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(c.getId());
                row.createCell(1).setCellValue(c.getContenuC() != null ? c.getContenuC() : "");
                row.createCell(2).setCellValue(c.getDateCom() != null ? c.getDateCom().toString() : "");
                row.createCell(3).setCellValue(c.getPublicationId());
            }

            Sheet statsSheet = workbook.createSheet("Statistiques");
            statsSheet.createRow(0).createCell(0).setCellValue("Statistiques LinguaLearn");
            statsSheet.getRow(0).getCell(0).setCellStyle(headerStyle);
            statsSheet.createRow(1).createCell(0).setCellValue("Total publications");
            statsSheet.getRow(1).createCell(1).setCellValue(pubs.size());
            statsSheet.createRow(2).createCell(0).setCellValue("Total commentaires");
            statsSheet.getRow(2).createCell(1).setCellValue(coms.size());
            statsSheet.createRow(3).createCell(0).setCellValue("Total likes");
            statsSheet.getRow(3).createCell(1).setCellValue(pubs.stream().mapToInt(Publication::getLikes).sum());
            statsSheet.createRow(4).createCell(0).setCellValue("Total signalements");
            statsSheet.getRow(4).createCell(1).setCellValue(pubs.stream().mapToInt(Publication::getReportPub).sum());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            showInfo("Export Excel", "Fichier sauvegarde : " + file.getName());
        } catch (Exception e) {
            showError("Erreur export Excel", e.getMessage());
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            showError("Erreur chargement vue", e.getMessage());
            e.printStackTrace();
        }
    }

    private void setActiveBtn(Button active) {
        btnDashboard.setStyle(btnDashboard == active ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;" : "-fx-background-color: transparent; -fx-text-fill: #cbd5e1;");
        btnPublications.setStyle(btnPublications == active ? "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;" : "-fx-background-color: transparent; -fx-text-fill: #cbd5e1;");
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    private void showInfo(String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }
}
