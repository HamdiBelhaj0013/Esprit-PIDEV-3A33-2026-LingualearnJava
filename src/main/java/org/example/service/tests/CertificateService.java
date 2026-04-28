package org.example.service.tests;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.example.entity.User;
import org.example.entity.tests.Certificate;
import org.example.repository.tests.CertificateRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Métier #3 — CertificateService
 * Design professionnel : fond blanc, bordure décorative bleue,
 * médaille dorée, nom élégant, QR code, signature.
 */
public class CertificateService {

    private static final String VERIFY_BASE_URL = "http://localhost:9090/api/certificate/verify/";
    private final CertificateRepository repo = new CertificateRepository();

    // ── API publique ──────────────────────────────────────────────────────────

    public Certificate generer(User user, String niveau, String languageName,
                               float scoreMoyen, String outputDir) throws IOException {

        var existing = repo.findExisting(user.getId(), niveau, languageName);
        if (existing.isPresent()) {
            Certificate c = existing.get();
            File f = c.getPdfPath() != null ? new File(c.getPdfPath()) : null;
            if (f != null && f.exists()) return c;
        }

        String uuid      = UUID.randomUUID().toString();
        String verifyUrl = VERIFY_BASE_URL + uuid;

        Certificate cert = new Certificate();
        cert.setUuid(uuid);
        cert.setUserId(user.getId());
        cert.setUserFullName(user.getFullName());
        cert.setUserEmail(user.getEmail());
        cert.setNiveau(niveau);
        cert.setLanguageName(languageName);
        cert.setScoreMoyen(scoreMoyen);
        cert.setIssuedAt(LocalDateTime.now());
        cert.setValid(true);

        String fileName = "certificat_" + niveau.toLowerCase() + "_"
                + languageName.toLowerCase().replace(" ", "_") + "_"
                + user.getId() + "_" + uuid.substring(0, 8) + ".pdf";
        String pdfPath  = outputDir + File.separator + fileName;

        byte[] qrBytes = genererQRCode(verifyUrl, 120);
        genererPDF(cert, pdfPath, qrBytes, verifyUrl);

        cert.setPdfPath(pdfPath);
        repo.save(cert);
        return cert;
    }

    public List<Certificate> findByUser(Long userId) { return repo.findByUserId(userId); }

    public Optional<Certificate> verifier(String uuid) { return repo.findByUuid(uuid); }

    // ── QR Code ───────────────────────────────────────────────────────────────

    private byte[] genererQRCode(String content, int size) throws IOException {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix  = new QRCodeWriter()
                    .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException e) {
            throw new IOException("Erreur QR Code : " + e.getMessage(), e);
        }
    }

    // ── PDF — design professionnel ────────────────────────────────────────────

    private void genererPDF(Certificate cert, String pdfPath,
                            byte[] qrBytes, String verifyUrl) throws IOException {
        new File(pdfPath).getParentFile().mkdirs();

        // ── Couleurs ──────────────────────────────────────────────────────────
        DeviceRgb BLEU_FONCE  = new DeviceRgb(10,  25,  100);   // #0a1964
        DeviceRgb BLEU_MOYEN  = new DeviceRgb(21,  71,  160);   // #1547a0
        DeviceRgb BLEU_CLAIR  = new DeviceRgb(100, 149, 210);   // bordure intérieure
        DeviceRgb OR          = new DeviceRgb(212, 175, 55);    // doré medal
        DeviceRgb OR_CLAIR    = new DeviceRgb(240, 210, 90);
        DeviceRgb TEXTE_FONCE = new DeviceRgb(20,  20,  60);
        DeviceRgb TEXTE_GRIS  = new DeviceRgb(100, 110, 140);
        DeviceRgb LIGNE       = new DeviceRgb(180, 190, 210);

        DeviceRgb NIV_COLOR = switch (cert.getNiveau()) {
            case "INTERMEDIATE" -> new DeviceRgb(180, 80,  0);
            case "ADVANCED"     -> new DeviceRgb(70,  30,  130);
            default             -> new DeviceRgb(30,  110, 60);
        };
        String niveauFr = switch (cert.getNiveau()) {
            case "INTERMEDIATE" -> "INTERMÉDIAIRE";
            case "ADVANCED"     -> "AVANCÉ";
            default             -> "DÉBUTANT";
        };

        // A4 portrait
        float W = PageSize.A4.getWidth();   // 595
        float H = PageSize.A4.getHeight();  // 842

        try (PdfWriter   pw  = new PdfWriter(pdfPath);
             PdfDocument pdf = new PdfDocument(pw);
             Document    doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(0, 0, 0, 0);

            // ── Ajouter la page explicitement avant d'utiliser PdfCanvas ─────
            pdf.addNewPage();
            PdfPage   page   = pdf.getFirstPage();
            PdfCanvas canvas = new PdfCanvas(page);

            // ── FOND BLANC ────────────────────────────────────────────────────
            canvas.setFillColor(ColorConstants.WHITE);
            canvas.rectangle(0, 0, W, H);
            canvas.fill();

            // ── BORDURE EXTÉRIEURE ÉPAISSE BLEUE ─────────────────────────────
            float bord = 18f;
            canvas.setStrokeColor(BLEU_FONCE);
            canvas.setLineWidth(10f);
            canvas.rectangle(bord, bord, W - 2*bord, H - 2*bord);
            canvas.stroke();

            // ── BORDURE INTÉRIEURE FINE ───────────────────────────────────────
            float bord2 = 28f;
            canvas.setStrokeColor(BLEU_CLAIR);
            canvas.setLineWidth(1.5f);
            canvas.rectangle(bord2, bord2, W - 2*bord2, H - 2*bord2);
            canvas.stroke();

            // ── BANDE BLEUE DU HAUT ───────────────────────────────────────────
            canvas.setFillColor(BLEU_FONCE);
            canvas.rectangle(bord, H - bord - 100, W - 2*bord, 100);
            canvas.fill();

            // ── BANDE BLEUE DU BAS ────────────────────────────────────────────
            canvas.setFillColor(BLEU_FONCE);
            canvas.rectangle(bord, bord, W - 2*bord, 80);
            canvas.fill();

            // ── COINS DÉCORATIFS (petits carrés or) ───────────────────────────
            float c = 14f;
            canvas.setFillColor(OR);
            // coins de la bordure intérieure
            canvas.rectangle(bord2 - 4, H - bord2 - c, c, c); canvas.fill();
            canvas.rectangle(W - bord2 - 10, H - bord2 - c, c, c); canvas.fill();
            canvas.rectangle(bord2 - 4, bord2, c, c); canvas.fill();
            canvas.rectangle(W - bord2 - 10, bord2, c, c); canvas.fill();

            // ── MÉDAILLE DORÉE (cercles concentriques) ────────────────────────
            float mx = W / 2f;
            float my = H - bord - 100 - 2; // juste en bas de la bande bleue haut
            // Cercle extérieur or foncé
            canvas.setFillColor(OR);
            canvas.circle(mx, my, 40);
            canvas.fill();
            // Cercle intérieur or clair
            canvas.setFillColor(OR_CLAIR);
            canvas.circle(mx, my, 32);
            canvas.fill();
            // Cercle intérieur blanc
            canvas.setFillColor(ColorConstants.WHITE);
            canvas.circle(mx, my, 24);
            canvas.fill();
            // Anneau bleu foncé entre cercles
            canvas.setStrokeColor(BLEU_FONCE);
            canvas.setLineWidth(2f);
            canvas.circle(mx, my, 28);
            canvas.stroke();

            // Rubans sous la médaille
            canvas.setFillColor(BLEU_FONCE);
            // Ruban gauche
            canvas.moveTo(mx - 12, my - 24);
            canvas.lineTo(mx - 6,  my - 52);
            canvas.lineTo(mx,      my - 24);
            canvas.closePathFillStroke();
            // Ruban droit
            canvas.moveTo(mx,      my - 24);
            canvas.lineTo(mx + 6,  my - 52);
            canvas.lineTo(mx + 12, my - 24);
            canvas.closePathFillStroke();

            // ── LIGNES DÉCORATIVES VAGUES (imitation du modèle) ──────────────
            // Vagues en haut à gauche
            for (int i = 0; i < 6; i++) {
                float alpha = 0.12f - i * 0.015f;
                int a = (int)(alpha * 255);
                canvas.setStrokeColor(BLEU_MOYEN);
                canvas.setLineWidth(1.2f);
                float offset = i * 8f;
                canvas.moveTo(bord2 + offset, H - bord - 100);
                canvas.curveTo(bord2 + 40 + offset, H - 140, bord2 + 80 + offset, H - 200, bord2 + offset, H - 260);
                canvas.stroke();
            }
            // Vagues en bas à droite
            for (int i = 0; i < 6; i++) {
                canvas.setStrokeColor(BLEU_MOYEN);
                canvas.setLineWidth(1.2f);
                float offset = i * 8f;
                canvas.moveTo(W - bord2 - offset, bord + 80);
                canvas.curveTo(W - bord2 - 40 - offset, bord + 140, W - bord2 - 80 - offset, bord + 200, W - bord2 - offset, bord + 260);
                canvas.stroke();
            }

            canvas.release();

            // ── Traductions selon la langue du certificat ─────────────────────
            String lang = cert.getLanguageName().toLowerCase();
            String txtCertificate, txtOfAchievement, txtPresentedTo,
                    txtCompleted, txtDate, txtSignature, txtScanVerify, txtFooter;

            if (lang.contains("fran") || lang.contains("french")) {
                txtCertificate  = "CERTIFICAT";
                txtOfAchievement = "DE RÉUSSITE";
                txtPresentedTo  = "DÉCERNÉ À";
                txtCompleted    = "a réussi avec succès le programme de certification";
                txtDate         = "Date";
                txtSignature    = "Signature";
                txtScanVerify   = "Scanner pour vérifier";
                txtFooter       = "LinguaLearn — Plateforme de Certification Linguistique";
            } else if (lang.contains("english") || lang.contains("anglais")) {
                txtCertificate  = "CERTIFICATE";
                txtOfAchievement = "OF ACHIEVEMENT";
                txtPresentedTo  = "PROUDLY PRESENTED TO";
                txtCompleted    = "has successfully completed the language certification programme";
                txtDate         = "Date";
                txtSignature    = "Signature";
                txtScanVerify   = "Scan to verify";
                txtFooter       = "LinguaLearn — Language Certification Platform";
            } else if (lang.contains("espagnol") || lang.contains("spanish")) {
                txtCertificate  = "CERTIFICADO";
                txtOfAchievement = "DE LOGRO";
                txtPresentedTo  = "PRESENTADO A";
                txtCompleted    = "ha completado con éxito el programa de certificación";
                txtDate         = "Fecha";
                txtSignature    = "Firma";
                txtScanVerify   = "Escanear para verificar";
                txtFooter       = "LinguaLearn — Plataforma de Certificación Lingüística";
            } else if (lang.contains("allemand") || lang.contains("german")) {
                txtCertificate  = "ZERTIFIKAT";
                txtOfAchievement = "DER LEISTUNG";
                txtPresentedTo  = "VERLIEHEN AN";
                txtCompleted    = "hat das Sprachzertifizierungsprogramm erfolgreich abgeschlossen";
                txtDate         = "Datum";
                txtSignature    = "Unterschrift";
                txtScanVerify   = "Zum Verifizieren scannen";
                txtFooter       = "LinguaLearn — Sprachzertifizierungsplattform";
            } else if (lang.contains("arabe") || lang.contains("arabic")) {
                txtCertificate  = "شهادة";
                txtOfAchievement = "إنجاز";
                txtPresentedTo  = "تُمنح إلى";
                txtCompleted    = "أتمَّ بنجاح برنامج شهادة اللغة";
                txtDate         = "التاريخ";
                txtSignature    = "التوقيع";
                txtScanVerify   = "امسح للتحقق";
                txtFooter       = "LinguaLearn — منصة شهادات اللغة";
            } else if (lang.contains("italien") || lang.contains("italian")) {
                txtCertificate  = "CERTIFICATO";
                txtOfAchievement = "DI SUCCESSO";
                txtPresentedTo  = "ASSEGNATO A";
                txtCompleted    = "ha completato con successo il programma di certificazione linguistica";
                txtDate         = "Data";
                txtSignature    = "Firma";
                txtScanVerify   = "Scansiona per verificare";
                txtFooter       = "LinguaLearn — Piattaforma di Certificazione Linguistica";
            } else {
                // Défaut : français
                txtCertificate  = "CERTIFICAT";
                txtOfAchievement = "DE RÉUSSITE";
                txtPresentedTo  = "DÉCERNÉ À";
                txtCompleted    = "a réussi avec succès le programme de certification";
                txtDate         = "Date";
                txtSignature    = "Signature";
                txtScanVerify   = "Scanner pour vérifier";
                txtFooter       = "LinguaLearn — Plateforme de Certification Linguistique";
            }

            // ── CONTENU TEXTE via Document ────────────────────────────────────

            // Titre en haut dans la bande bleue
            doc.add(new Paragraph(txtCertificate)
                    .setFontColor(ColorConstants.WHITE)
                    .setFontSize(28).setBold()
                    .setCharacterSpacing(6)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(24).setMarginBottom(2));

            doc.add(new Paragraph(txtOfAchievement)
                    .setFontColor(new DeviceRgb(180, 200, 230))
                    .setFontSize(11)
                    .setCharacterSpacing(4)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(0));

            // Espace pour la médaille
            doc.add(new Paragraph("★")
                    .setFontColor(OR)
                    .setFontSize(22)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(38).setMarginBottom(0));

            // "DÉCERNÉ À" / "PROUDLY PRESENTED TO" etc.
            doc.add(new Paragraph(txtPresentedTo)
                    .setFontColor(TEXTE_GRIS)
                    .setFontSize(9)
                    .setCharacterSpacing(3)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(28).setMarginBottom(6));

            // Nom du titulaire — style script élégant
            doc.add(new Paragraph(cert.getUserFullName())
                    .setFontColor(BLEU_FONCE)
                    .setFontSize(36).setBold()
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(6));

            // Email
            doc.add(new Paragraph(cert.getUserEmail())
                    .setFontColor(TEXTE_GRIS)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Ligne décorative
            doc.add(horizontalLine(W - 140, LIGNE));

            // Corps du texte (traduit selon la langue)
            doc.add(new Paragraph(txtCompleted)
                    .setFontColor(TEXTE_GRIS)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(14).setMarginBottom(6));

            // Langue
            doc.add(new Paragraph(cert.getLanguageName().toUpperCase())
                    .setFontColor(BLEU_MOYEN)
                    .setFontSize(20).setBold()
                    .setCharacterSpacing(2)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(14));

            // Badge niveau
            float sur20 = Math.round(cert.getScoreMoyen() / 100f * 20f * 10f) / 10f;
            Table nivTable = new Table(new float[]{160})
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setBackgroundColor(NIV_COLOR)
                    .setMarginBottom(14);
            nivTable.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(8)
                    .add(new Paragraph("NIVEAU  " + niveauFr)
                            .setFontColor(ColorConstants.WHITE)
                            .setFontSize(11).setBold()
                            .setTextAlignment(TextAlignment.CENTER)));
            doc.add(nivTable);

            // Score
            doc.add(new Paragraph(
                    String.format("Score moyen :  %.1f / 20   (%.0f%%)", sur20, cert.getScoreMoyen()))
                    .setFontColor(NIV_COLOR)
                    .setFontSize(13).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(16));

            // Ligne décorative avant signature
            doc.add(horizontalLine(W - 140, LIGNE));

            // Zone signature + date + QR
            String dateStr = cert.getIssuedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH));

            Table bottomRow = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .setWidth(W - 100)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginTop(16);

            // Date
            Cell dateCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            dateCell.add(new Paragraph("_______________")
                    .setFontColor(LIGNE).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            dateCell.add(new Paragraph(txtDate)
                    .setFontColor(TEXTE_GRIS).setFontSize(9)
                    .setCharacterSpacing(1).setTextAlignment(TextAlignment.CENTER).setMarginTop(2));
            dateCell.add(new Paragraph(dateStr)
                    .setFontColor(TEXTE_FONCE).setFontSize(9).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            bottomRow.addCell(dateCell);

            // QR Code au centre
            Cell qrCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            qrCell.add(new Image(ImageDataFactory.create(qrBytes))
                    .setWidth(70).setHeight(70)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER));
            qrCell.add(new Paragraph(txtScanVerify)
                    .setFontColor(TEXTE_GRIS).setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(2));
            bottomRow.addCell(qrCell);

            // Signature
            Cell sigCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            sigCell.add(new Paragraph("_______________")
                    .setFontColor(LIGNE).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            sigCell.add(new Paragraph(txtSignature)
                    .setFontColor(TEXTE_GRIS).setFontSize(9)
                    .setCharacterSpacing(1).setTextAlignment(TextAlignment.CENTER).setMarginTop(2));
            sigCell.add(new Paragraph("LinguaLearn")
                    .setFontColor(BLEU_FONCE).setFontSize(9).setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            bottomRow.addCell(sigCell);

            doc.add(bottomRow);

            // UUID petit en bas
            doc.add(new Paragraph("Certificate ID : " + cert.getUuid())
                    .setFontColor(new DeviceRgb(190, 200, 215))
                    .setFontSize(6.5f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8));

            doc.add(new Paragraph(txtFooter + "   |   " + verifyUrl)
                    .setFontColor(new DeviceRgb(160, 185, 220))
                    .setFontSize(7)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(2));
        }
    }

    // ── Ligne horizontale décorative ──────────────────────────────────────────

    private static Table horizontalLine(float width, DeviceRgb color) {
        Table t = new Table(new float[]{width})
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                .setMarginTop(4).setMarginBottom(4);
        t.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(color, 0.8f))
                .setPadding(0).setHeight(1));
        return t;
    }
}