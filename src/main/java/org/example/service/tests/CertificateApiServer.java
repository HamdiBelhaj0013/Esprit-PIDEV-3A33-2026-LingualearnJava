package org.example.service.tests;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.entity.tests.Certificate;
import org.example.repository.tests.CertificateRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Étape 8 — API HTTP embarquée port 9090
 * GET /api/certificate/verify/{uuid}       → Page HTML professionnelle
 * GET /api/certificate/verify/{uuid}/json  → JSON brut
 */
public class CertificateApiServer {

    private static final int    PORT = 9090;
    private static final String PATH = "/api/certificate/verify/";
    private static final Logger LOG  = Logger.getLogger(CertificateApiServer.class.getName());
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm", java.util.Locale.FRENCH);

    private static HttpServer server;

    // ── Start / Stop ──────────────────────────────────────────────────────────

    public static void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext(PATH, CertificateApiServer::handle);
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            LOG.info("API LinguaLearn demarree -> http://localhost:" + PORT + PATH + "{uuid}");
        } catch (IOException e) {
            LOG.warning("API non demarree : " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    private static void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"GET".equals(ex.getRequestMethod())) {
            sendHtml(ex, 405, buildPage(null, "—")); return;
        }

        String  path     = ex.getRequestURI().getPath();
        boolean jsonMode = path.endsWith("/json");
        String  uuid     = extractUuid(path.replace("/json", ""));

        if (uuid == null) { sendHtml(ex, 400, buildPage(null, "invalid")); return; }

        try {
            Optional<Certificate> opt = new CertificateRepository().findByUuid(uuid);
            if (opt.isEmpty()) {
                if (jsonMode) sendJson(ex, 404, "{\"valid\":false,\"message\":\"Certificat introuvable.\"}");
                else          sendHtml(ex, 404, buildPage(null, uuid));
                return;
            }
            Certificate cert = opt.get();
            if (jsonMode) sendJson(ex, 200, toJson(cert));
            else          sendHtml(ex, 200, buildPage(cert, uuid));
        } catch (Exception e) {
            sendHtml(ex, 500, buildPage(null, uuid));
        }
    }

    // ── Page HTML ─────────────────────────────────────────────────────────────

    private static String buildPage(Certificate cert, String uuid) {
        boolean valid = cert != null && cert.isValid();

        // Langue → couleur thématique
        String langColor = "#1a237e";
        String langBg    = "#e8eaf6";
        String langFlag  = "🌐";
        if (cert != null) {
            String lang = cert.getLanguageName().toLowerCase();
            if (lang.contains("fran") || lang.contains("french")) {
                langColor = "#0055A4"; langBg = "#e3eeff"; langFlag = "🇫🇷";
            } else if (lang.contains("english") || lang.contains("anglais")) {
                langColor = "#012169"; langBg = "#e8eaf6"; langFlag = "🇬🇧";
            } else if (lang.contains("espagnol") || lang.contains("spanish")) {
                langColor = "#c60b1e"; langBg = "#fdecea"; langFlag = "🇪🇸";
            } else if (lang.contains("allemand") || lang.contains("german")) {
                langColor = "#000000"; langBg = "#f5f5f5"; langFlag = "🇩🇪";
            } else if (lang.contains("arabe") || lang.contains("arabic")) {
                langColor = "#006233"; langBg = "#e8f5e9"; langFlag = "🇸🇦";
            } else if (lang.contains("italien") || lang.contains("italian")) {
                langColor = "#009246"; langBg = "#e8f5e9"; langFlag = "🇮🇹";
            }
        }

        // Niveau
        String niveauLabel = "";
        String niveauColor = "#1a237e";
        String niveauBg    = "#e8eaf6";
        if (cert != null) {
            niveauLabel = switch (cert.getNiveau()) {
                case "INTERMEDIATE" -> "INTERMÉDIAIRE";
                case "ADVANCED"     -> "AVANCÉ";
                default             -> "DÉBUTANT";
            };
            niveauColor = switch (cert.getNiveau()) {
                case "INTERMEDIATE" -> "#e65100";
                case "ADVANCED"     -> "#4527a0";
                default             -> "#2e7d32";
            };
            niveauBg = switch (cert.getNiveau()) {
                case "INTERMEDIATE" -> "#fff3e0";
                case "ADVANCED"     -> "#ede7f6";
                default             -> "#e8f5e9";
            };
        }

        float sur20 = cert != null
                ? Math.round(cert.getScoreMoyen() / 100f * 20f * 10f) / 10f : 0f;
        int   pct   = cert != null ? Math.round(cert.getScoreMoyen()) : 0;

        String issued = (cert != null && cert.getIssuedAt() != null)
                ? cert.getIssuedAt().format(FMT) : "—";

        String verifyUrl = "http://localhost:9090" + PATH + uuid;

        // ── Construit le HTML proprement (pas de String.formatted avec CSS) ───
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"fr\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>LinguaLearn — Vérification de Certificat</title>\n");
        sb.append("<style>\n");
        sb.append("*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n");
        sb.append("body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f4ff; min-height: 100vh; }\n");

        // Header
        sb.append(".topbar { background: #1a237e; padding: 0 40px; height: 64px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 12px rgba(0,0,0,0.18); }\n");
        sb.append(".brand { display: flex; align-items: center; gap: 12px; }\n");
        sb.append(".brand-icon { width: 38px; height: 38px; background: #ffd600; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 20px; color: #1a237e; }\n");
        sb.append(".brand-name { color: white; font-size: 20px; font-weight: 700; }\n");
        sb.append(".brand-sub  { color: rgba(255,255,255,0.55); font-size: 12px; }\n");
        sb.append(".topbar-right { color: rgba(255,255,255,0.65); font-size: 12px; }\n");

        // Main
        sb.append(".main { max-width: 860px; margin: 0 auto; padding: 48px 24px; }\n");

        // Status banner
        String statusBg     = valid ? "#d3f9d8" : "#ffe3e3";
        String statusBorder = valid ? "#69db7c" : "#ffa8a8";
        String statusColor  = valid ? "#2b8a3e" : "#c92a2a";
        String statusIcon   = valid ? "✅" : "❌";
        String statusTitle  = valid ? "Certificat Authentique et Valide" : "Certificat Invalide ou Introuvable";
        String statusMsg    = valid
                ? "Ce certificat a bien été délivré par LinguaLearn et est authentique."
                : "Ce certificat est introuvable dans notre base ou a été révoqué.";

        sb.append(".status { background: ").append(statusBg).append("; border: 1.5px solid ").append(statusBorder)
                .append("; border-radius: 14px; padding: 22px 28px; display: flex; align-items: center; gap: 18px; margin-bottom: 28px; }\n");
        sb.append(".status-icon { font-size: 42px; }\n");
        sb.append(".status-title { font-size: 20px; font-weight: 700; color: ").append(statusColor).append("; }\n");
        sb.append(".status-msg   { font-size: 13px; color: ").append(statusColor).append("; opacity: 0.8; margin-top: 4px; }\n");

        // Card
        sb.append(".card { background: white; border-radius: 18px; box-shadow: 0 4px 32px rgba(26,35,126,0.10); overflow: hidden; margin-bottom: 24px; }\n");
        sb.append(".card-header { padding: 20px 32px; border-bottom: 1px solid #e3e8f0; display: flex; align-items: center; justify-content: space-between; }\n");
        sb.append(".card-title { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em; color: #9ca3af; }\n");
        sb.append(".card-body  { padding: 28px 32px; }\n");

        // Holder section
        sb.append(".holder-name  { font-size: 30px; font-weight: 700; color: #1a237e; margin-bottom: 4px; }\n");
        sb.append(".holder-email { font-size: 14px; color: #6c7a99; margin-bottom: 20px; }\n");

        // Grid
        sb.append(".grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16px; }\n");
        sb.append(".grid-item { background: #f8fafc; border-radius: 12px; border: 1px solid #e3e8f0; padding: 18px 16px; }\n");
        sb.append(".grid-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: #9ca3af; margin-bottom: 8px; }\n");
        sb.append(".grid-value { font-size: 18px; font-weight: 700; color: #1a237e; }\n");
        sb.append(".grid-sub   { font-size: 11px; color: #6c7a99; margin-top: 3px; }\n");

        // Lang badge
        sb.append(".lang-badge { display: inline-flex; align-items: center; gap: 8px; background: ").append(langBg)
                .append("; color: ").append(langColor).append("; border: 1.5px solid ").append(langColor)
                .append("33; border-radius: 30px; padding: 6px 16px; font-weight: 700; font-size: 15px; }\n");

        // Niveau badge
        sb.append(".niveau-badge { display: inline-block; background: ").append(niveauBg)
                .append("; color: ").append(niveauColor)
                .append("; border-radius: 8px; padding: 5px 14px; font-weight: 700; font-size: 14px; }\n");

        // Score
        sb.append(".score-big { font-size: 28px; font-weight: 700; color: #2fb344; }\n");
        sb.append(".score-pct { font-size: 14px; color: #6c7a99; margin-top: 3px; }\n");

        // UUID block
        sb.append(".uuid-block { background: #f8fafc; border: 1px solid #e3e8f0; border-radius: 10px; padding: 14px 18px; margin-top: 24px; }\n");
        sb.append(".uuid-label { font-size: 11px; font-weight: 700; text-transform: uppercase; color: #9ca3af; margin-bottom: 6px; }\n");
        sb.append(".uuid-val { font-family: monospace; font-size: 12px; color: #374151; word-break: break-all; }\n");
        sb.append(".uuid-url { font-family: monospace; font-size: 11px; color: #1565c0; margin-top: 4px; word-break: break-all; }\n");

        // Footer
        sb.append(".footer { background: #1a237e; color: rgba(255,255,255,0.55); text-align: center; padding: 18px; font-size: 11px; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        // ── TOPBAR ────────────────────────────────────────────────────────────
        sb.append("<div class=\"topbar\">\n");
        sb.append("  <div class=\"brand\">\n");
        sb.append("    <div class=\"brand-icon\">L</div>\n");
        sb.append("    <div><div class=\"brand-name\">LinguaLearn</div>");
        sb.append("<div class=\"brand-sub\">Plateforme de Certification Linguistique</div></div>\n");
        sb.append("  </div>\n");
        sb.append("  <div class=\"topbar-right\">Vérification d'Authenticité</div>\n");
        sb.append("</div>\n");

        // ── MAIN ──────────────────────────────────────────────────────────────
        sb.append("<div class=\"main\">\n");

        // Status
        sb.append("<div class=\"status\">\n");
        sb.append("  <div class=\"status-icon\">").append(statusIcon).append("</div>\n");
        sb.append("  <div><div class=\"status-title\">").append(statusTitle).append("</div>\n");
        sb.append("  <div class=\"status-msg\">").append(statusMsg).append("</div></div>\n");
        sb.append("</div>\n");

        if (cert != null) {

            // ── Card titulaire ────────────────────────────────────────────────
            sb.append("<div class=\"card\">\n");
            sb.append("  <div class=\"card-header\">\n");
            sb.append("    <div class=\"card-title\">Titulaire du Certificat</div>\n");
            sb.append("    <div class=\"lang-badge\">").append(langFlag).append(" ")
                    .append(esc(cert.getLanguageName())).append("</div>\n");
            sb.append("  </div>\n");
            sb.append("  <div class=\"card-body\">\n");
            sb.append("    <div class=\"holder-name\">").append(esc(cert.getUserFullName())).append("</div>\n");
            sb.append("    <div class=\"holder-email\">").append(esc(cert.getUserEmail())).append("</div>\n");

            // Grille
            sb.append("    <div class=\"grid\">\n");

            // Langue
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">").append(langFlag).append(" Langue certifiée</div>\n");
            sb.append("        <div class=\"grid-value\" style=\"color:").append(langColor).append(";\">")
                    .append(esc(cert.getLanguageName())).append("</div>\n");
            sb.append("      </div>\n");

            // Niveau
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">📊 Niveau atteint</div>\n");
            sb.append("        <div><span class=\"niveau-badge\">").append(niveauLabel).append("</span></div>\n");
            sb.append("      </div>\n");

            // Score
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">🏆 Score moyen</div>\n");
            sb.append("        <div class=\"score-big\">").append(sur20).append(" / 20</div>\n");
            sb.append("        <div class=\"score-pct\">").append(pct).append("%</div>\n");
            sb.append("      </div>\n");

            // Date délivrance
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">📅 Date de délivrance</div>\n");
            sb.append("        <div class=\"grid-value\" style=\"font-size:14px;\">").append(issued).append("</div>\n");
            sb.append("      </div>\n");

            // Émetteur
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">🏛️ Émetteur</div>\n");
            sb.append("        <div class=\"grid-value\" style=\"font-size:14px;\">LinguaLearn</div>\n");
            sb.append("        <div class=\"grid-sub\">Plateforme de Certification</div>\n");
            sb.append("      </div>\n");

            // Statut
            sb.append("      <div class=\"grid-item\">\n");
            sb.append("        <div class=\"grid-label\">🔒 Statut</div>\n");
            String sColor = cert.isValid() ? "#2b8a3e" : "#c92a2a";
            String sLabel = cert.isValid() ? "✅ Valide" : "❌ Révoqué";
            sb.append("        <div class=\"grid-value\" style=\"color:").append(sColor).append(";\">")
                    .append(sLabel).append("</div>\n");
            sb.append("      </div>\n");

            sb.append("    </div>\n"); // end grid

            // UUID + URL
            sb.append("    <div class=\"uuid-block\">\n");
            sb.append("      <div class=\"uuid-label\">🔑 Identifiant unique (UUID)</div>\n");
            sb.append("      <div class=\"uuid-val\">").append(uuid).append("</div>\n");
            sb.append("      <div class=\"uuid-url\">").append(verifyUrl).append("</div>\n");
            sb.append("    </div>\n");

            sb.append("  </div>\n"); // end card-body
            sb.append("</div>\n");   // end card

        } // end if cert != null

        sb.append("</div>\n"); // end main

        // ── FOOTER ────────────────────────────────────────────────────────────
        sb.append("<div class=\"footer\">LinguaLearn &mdash; Plateforme de Certification Linguistique</div>\n");
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    // ── JSON ──────────────────────────────────────────────────────────────────

    private static String toJson(Certificate cert) {
        float sur20 = Math.round(cert.getScoreMoyen() / 100f * 20f * 10f) / 10f;
        String issued = cert.getIssuedAt() != null
                ? cert.getIssuedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
        return "{\n"
                + "  \"valid\": " + cert.isValid() + ",\n"
                + "  \"uuid\": \"" + cert.getUuid() + "\",\n"
                + "  \"holder\": \"" + esc(cert.getUserFullName()) + "\",\n"
                + "  \"email\": \"" + esc(cert.getUserEmail()) + "\",\n"
                + "  \"niveau\": \"" + cert.getNiveau() + "\",\n"
                + "  \"language\": \"" + esc(cert.getLanguageName()) + "\",\n"
                + "  \"score_sur20\": \"" + sur20 + " / 20\",\n"
                + "  \"score_pct\": \"" + Math.round(cert.getScoreMoyen()) + "%\",\n"
                + "  \"issued_at\": \"" + issued + "\",\n"
                + "  \"issuer\": \"LinguaLearn\"\n"
                + "}";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractUuid(String path) {
        int i = path.lastIndexOf('/');
        if (i < 0 || i == path.length() - 1) return null;
        String u = path.substring(i + 1).trim();
        return u.matches("[0-9a-fA-F\\-]{36}") ? u : null;
    }

    private static void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}