package org.example.service.tests;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.repository.tests.AntiCheatRepository;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * API #2 — AntiCheatApiServer
 * Serveur HTTP embarqué sur le port 9091.
 *
 * Endpoints :
 *   POST /api/anticheat/event
 *        Body JSON : { "userId":1, "testId":5, "userFullName":"...", "testTitle":"...",
 *                      "eventType":"FOCUS_LOST", "nbSorties":1, "soumisAuto":false,
 *                      "penalitePct":0, "scoreAvant":80, "scoreApres":80, "detail":"..." }
 *        → Enregistre un événement anti-triche
 *
 *   GET  /api/anticheat/status/{userId}/{testId}
 *        → Statut anti-triche d'un étudiant sur un test (JSON)
 *
 *   GET  /api/anticheat/logs
 *        → Tous les incidents (100 derniers) — pour l'admin (HTML ou JSON)
 *
 *   GET  /api/anticheat/logs/json
 *        → Même chose en JSON brut
 *
 *   GET  /api/anticheat/stats
 *        → Statistiques globales (JSON)
 *
 * Démarrer dans App.java :
 *   AntiCheatApiServer.start();
 *   AntiCheatApiServer.stop();
 */
public class AntiCheatApiServer {

    private static final int    PORT = 9091;
    private static final Logger LOG  = Logger.getLogger(AntiCheatApiServer.class.getName());
    private static HttpServer   server;

    // ── Start / Stop ──────────────────────────────────────────────────────────

    public static void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/api/anticheat/", AntiCheatApiServer::handle);
            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            LOG.info("AntiCheat API demarree -> http://localhost:" + PORT + "/api/anticheat/");
        } catch (IOException e) {
            LOG.warning("AntiCheat API non demarree : " + e.getMessage());
        }
    }

    public static void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    // ── Router ────────────────────────────────────────────────────────────────

    private static void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1); return;
        }

        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        try {
            if ("POST".equals(method) && path.equals("/api/anticheat/event")) {
                handlePostEvent(ex);
            } else if ("GET".equals(method) && path.startsWith("/api/anticheat/status/")) {
                handleGetStatus(ex, path);
            } else if ("GET".equals(method) && path.equals("/api/anticheat/logs/json")) {
                handleGetLogsJson(ex);
            } else if ("GET".equals(method) && path.equals("/api/anticheat/logs")) {
                handleGetLogsHtml(ex);
            } else if ("GET".equals(method) && path.equals("/api/anticheat/stats")) {
                handleGetStats(ex);
            } else {
                sendJson(ex, 404,
                        "{\"error\":\"Endpoint introuvable\",\"path\":\"" + path + "\"}");
            }
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }

    // ── POST /api/anticheat/event ─────────────────────────────────────────────

    private static void handlePostEvent(HttpExchange ex) throws IOException {
        String body = readBody(ex);

        // Parser JSON manuellement (pas de dépendance Jackson)
        long   userId      = parseLong(body,   "userId",      0);
        long   testId      = parseLong(body,    "testId",      0);
        String userName    = parseStr(body,    "userFullName","");
        String testTitle   = parseStr(body,    "testTitle",   "");
        String eventType   = parseStr(body,    "eventType",   "UNKNOWN");
        int    nbSorties   = parseInt(body,    "nbSorties",   0);
        boolean soumisAuto = parseBool(body,   "soumisAuto",  false);
        int    penalite    = parseInt(body,    "penalitePct", 0);
        float  scoreAvant  = parseFloat(body,  "scoreAvant",  0f);
        float  scoreApres  = parseFloat(body,  "scoreApres",  0f);
        String detail      = parseStr(body,    "detail",      "");

        if (userId == 0 || testId == 0) {
            sendJson(ex, 400, "{\"error\":\"userId et testId sont obligatoires.\"}");
            return;
        }

        new AntiCheatRepository().logEvent(
                userId, testId, userName, testTitle,
                eventType, nbSorties, soumisAuto,
                penalite, scoreAvant, scoreApres, detail);

        sendJson(ex, 201, "{\"success\":true,\"message\":\"Événement enregistré.\","
                + "\"eventType\":\"" + eventType + "\"}");
    }

    // ── GET /api/anticheat/status/{userId}/{testId} ───────────────────────────

    private static void handleGetStatus(HttpExchange ex, String path) throws IOException {
        // path = /api/anticheat/status/1/5
        String[] parts = path.split("/");
        if (parts.length < 6) {
            sendJson(ex, 400, "{\"error\":\"Format: /api/anticheat/status/{userId}/{testId}\"}");
            return;
        }
        try {
            long userId = Long.parseLong(parts[4]);
            long testId = Long.parseLong(parts[5]);
            Map<String, Object> status = new AntiCheatRepository().getStatus(userId, testId);
            sendJson(ex, 200, toJson(status));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "{\"error\":\"userId et testId doivent être des nombres.\"}");
        }
    }

    // ── GET /api/anticheat/logs/json ──────────────────────────────────────────

    private static void handleGetLogsJson(HttpExchange ex) throws IOException {
        List<Map<String, Object>> logs = new AntiCheatRepository().findAll(100);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < logs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(logs.get(i)));
        }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    // ── GET /api/anticheat/logs (HTML) ────────────────────────────────────────

    private static void handleGetLogsHtml(HttpExchange ex) throws IOException {
        List<Map<String, Object>> logs = new AntiCheatRepository().findAll(100);
        Map<String, Object> stats = new AntiCheatRepository().getGlobalStats();
        sendHtml(ex, 200, buildLogsPage(logs, stats));
    }

    // ── GET /api/anticheat/stats ──────────────────────────────────────────────

    private static void handleGetStats(HttpExchange ex) throws IOException {
        Map<String, Object> stats = new AntiCheatRepository().getGlobalStats();
        sendJson(ex, 200, toJson(stats));
    }

    // ── Page HTML Logs ────────────────────────────────────────────────────────

    private static String buildLogsPage(List<Map<String, Object>> logs,
                                        Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width,initial-scale=1'>");
        sb.append("<title>LinguaLearn — Surveillance Anti-Triche</title>");
        sb.append("<style>");
        sb.append("*{box-sizing:border-box;margin:0;padding:0}");
        sb.append("body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff}");
        sb.append(".topbar{background:#1a237e;padding:0 32px;height:60px;display:flex;");
        sb.append("align-items:center;justify-content:space-between;");
        sb.append("box-shadow:0 2px 12px rgba(0,0,0,0.2)}");
        sb.append(".brand{display:flex;align-items:center;gap:10px}");
        sb.append(".brand-icon{width:36px;height:36px;background:#ffd600;border-radius:8px;");
        sb.append("display:flex;align-items:center;justify-content:center;");
        sb.append("font-weight:900;font-size:18px;color:#1a237e}");
        sb.append(".brand-name{color:white;font-size:18px;font-weight:700}");
        sb.append(".topbar-sub{color:rgba(255,255,255,0.6);font-size:12px}");
        sb.append(".main{max-width:1100px;margin:0 auto;padding:36px 24px}");
        // Stats cards
        sb.append(".stats-grid{display:grid;grid-template-columns:repeat(6,1fr);gap:14px;margin-bottom:28px}");
        sb.append(".stat-card{background:white;border-radius:12px;padding:16px;");
        sb.append("border:1px solid #e3e8f0;text-align:center;");
        sb.append("box-shadow:0 2px 8px rgba(26,35,126,0.07)}");
        sb.append(".stat-val{font-size:28px;font-weight:700;color:#1a237e}");
        sb.append(".stat-lbl{font-size:10px;font-weight:600;text-transform:uppercase;");
        sb.append("letter-spacing:0.06em;color:#9ca3af;margin-top:4px}");
        // Table
        sb.append(".card{background:white;border-radius:14px;overflow:hidden;");
        sb.append("box-shadow:0 4px 20px rgba(26,35,126,0.08);margin-bottom:20px}");
        sb.append(".card-header{background:#1a237e;padding:16px 24px;");
        sb.append("display:flex;align-items:center;justify-content:space-between}");
        sb.append(".card-title{color:white;font-size:15px;font-weight:700}");
        sb.append(".card-sub{color:rgba(255,255,255,0.6);font-size:12px}");
        sb.append("table{width:100%;border-collapse:collapse}");
        sb.append("th{background:#f8fafc;padding:12px 16px;font-size:10px;font-weight:700;");
        sb.append("text-transform:uppercase;letter-spacing:0.06em;color:#9ca3af;");
        sb.append("border-bottom:1px solid #e3e8f0;text-align:left}");
        sb.append("td{padding:12px 16px;font-size:13px;color:#374151;");
        sb.append("border-bottom:0.5px solid #f0f4ff}");
        sb.append("tr:last-child td{border-bottom:none}");
        sb.append("tr:hover td{background:#f8faff}");
        // Badges événements
        sb.append(".badge{display:inline-block;padding:3px 10px;border-radius:20px;");
        sb.append("font-size:10px;font-weight:700}");
        sb.append(".badge-focus{background:#fff3e0;color:#e65100}");
        sb.append(".badge-copy{background:#fce4ec;color:#c2185b}");
        sb.append(".badge-paste{background:#fce4ec;color:#c2185b}");
        sb.append(".badge-submit{background:#ffebee;color:#c62828}");
        sb.append(".badge-ok{background:#e8f5e9;color:#2e7d32}");
        sb.append(".empty{text-align:center;padding:60px;color:#9ca3af}");
        sb.append(".footer{background:#1a237e;color:rgba(255,255,255,0.5);");
        sb.append("text-align:center;padding:14px;font-size:11px;margin-top:24px}");
        sb.append("</style></head><body>");

        // Topbar
        sb.append("<div class='topbar'>");
        sb.append("<div class='brand'><div class='brand-icon'>L</div>");
        sb.append("<div><div class='brand-name'>LinguaLearn</div>");
        sb.append("<div class='topbar-sub'>Surveillance Anti-Triche</div></div></div>");
        sb.append("<div style='color:rgba(255,255,255,0.6);font-size:12px'>");
        sb.append("API : http://localhost:9091/api/anticheat/</div>");
        sb.append("</div>");

        // Main
        sb.append("<div class='main'>");

        // Stats
        sb.append("<div class='stats-grid'>");
        addStatCard(sb, String.valueOf(stats.getOrDefault("total_incidents", 0)),      "Incidents totaux");
        addStatCard(sb, String.valueOf(stats.getOrDefault("nb_users_suspects", 0)),    "Étudiants suspects");
        addStatCard(sb, String.valueOf(stats.getOrDefault("nb_tests_concernes", 0)),   "Tests concernés");
        addStatCard(sb, String.valueOf(stats.getOrDefault("nb_soumissions_auto", 0)),  "Soumissions auto");
        addStatCard(sb, String.valueOf(stats.getOrDefault("nb_copies", 0)),            "Tentatives copie");
        addStatCard(sb, String.valueOf(stats.getOrDefault("nb_pastes", 0)),            "Tentatives collage");
        sb.append("</div>");

        // Table logs
        sb.append("<div class='card'>");
        sb.append("<div class='card-header'>");
        sb.append("<div class='card-title'>📋 Journal des incidents</div>");
        sb.append("<div class='card-sub'>").append(logs.size()).append(" événements récents</div>");
        sb.append("</div>");

        if (logs.isEmpty()) {
            sb.append("<div class='empty'>✅ Aucun incident détecté</div>");
        } else {
            sb.append("<table><thead><tr>");
            sb.append("<th>#</th><th>Étudiant</th><th>Test</th><th>Événement</th>");
            sb.append("<th>Sorties</th><th>Soumis auto</th><th>Pénalité</th>");
            sb.append("<th>Score avant</th><th>Score après</th><th>Date</th>");
            sb.append("</tr></thead><tbody>");

            for (Map<String, Object> log : logs) {
                String eventType = String.valueOf(log.getOrDefault("event_type", ""));
                String badgeClass = switch (eventType) {
                    case "FOCUS_LOST"    -> "badge-focus";
                    case "COPY_ATTEMPT"  -> "badge-copy";
                    case "PASTE_ATTEMPT" -> "badge-paste";
                    case "FORCE_SUBMIT"  -> "badge-submit";
                    default              -> "badge-ok";
                };
                String eventLabel = switch (eventType) {
                    case "FOCUS_LOST"    -> "Sortie fenêtre";
                    case "COPY_ATTEMPT"  -> "Copie (Ctrl+C)";
                    case "PASTE_ATTEMPT" -> "Collage (Ctrl+V)";
                    case "FORCE_SUBMIT"  -> "Soumission forcée";
                    case "WINDOW_CLOSE"  -> "Fermeture fenêtre";
                    default              -> eventType;
                };

                boolean soumisAuto = Boolean.TRUE.equals(log.get("soumis_auto"));
                int penalite = (int) log.getOrDefault("penalite_pct", 0);
                float av = (float)(double) Double.parseDouble(
                        String.valueOf(log.getOrDefault("score_avant", 0)));
                float ap = (float)(double) Double.parseDouble(
                        String.valueOf(log.getOrDefault("score_apres", 0)));
                float av20 = Math.round(av / 100f * 20f * 10f) / 10f;
                float ap20 = Math.round(ap / 100f * 20f * 10f) / 10f;

                sb.append("<tr>");
                sb.append("<td style='color:#9ca3af'>").append(log.get("id")).append("</td>");
                sb.append("<td><strong>").append(esc(String.valueOf(log.getOrDefault("user_full_name",""))))
                        .append("</strong></td>");
                sb.append("<td style='font-size:12px'>")
                        .append(esc(String.valueOf(log.getOrDefault("test_title","")))).append("</td>");
                sb.append("<td><span class='badge ").append(badgeClass).append("'>")
                        .append(eventLabel).append("</span></td>");
                sb.append("<td style='text-align:center'>")
                        .append(log.getOrDefault("nb_sorties", 0)).append("</td>");
                sb.append("<td style='text-align:center'>")
                        .append(soumisAuto ? "<span style='color:#c62828;font-weight:700'>OUI</span>"
                                : "<span style='color:#2e7d32'>Non</span>").append("</td>");
                sb.append("<td style='text-align:center;color:#c62828;font-weight:700'>")
                        .append(penalite > 0 ? "-" + penalite + "%" : "—").append("</td>");
                sb.append("<td style='color:#6c7a99'>").append(av20).append("/20</td>");
                sb.append("<td style='font-weight:700;color:")
                        .append(ap20 >= 10 ? "#2fb344" : "#d63939").append("'>")
                        .append(ap20).append("/20</td>");
                sb.append("<td style='font-size:11px;color:#9ca3af'>")
                        .append(String.valueOf(log.getOrDefault("event_at","")).replace("T"," "))
                        .append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }
        sb.append("</div>"); // card

        sb.append("</div>"); // main
        sb.append("<div class='footer'>LinguaLearn — API Anti-Triche | ");
        sb.append("http://localhost:9091/api/anticheat/ | © 2026</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void addStatCard(StringBuilder sb, String val, String label) {
        sb.append("<div class='stat-card'>");
        sb.append("<div class='stat-val'>").append(val).append("</div>");
        sb.append("<div class='stat-lbl'>").append(label).append("</div>");
        sb.append("</div>");
    }

    // ── JSON builder simple ───────────────────────────────────────────────────

    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null)               sb.append("null");
            else if (v instanceof Number) sb.append(v);
            else if (v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(esc(v.toString())).append("\"");
        }
        return sb.append("}").toString();
    }

    // ── Parsers JSON ─────────────────────────────────────────────────────────

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static long parseLong(String json, String key, long def) {
        try {
            String v = extractValue(json, key);
            return v != null ? Long.parseLong(v.trim()) : def;
        } catch (Exception e) { return def; }
    }

    private static int parseInt(String json, String key, int def) {
        try {
            String v = extractValue(json, key);
            return v != null ? Integer.parseInt(v.trim()) : def;
        } catch (Exception e) { return def; }
    }

    private static float parseFloat(String json, String key, float def) {
        try {
            String v = extractValue(json, key);
            return v != null ? Float.parseFloat(v.trim()) : def;
        } catch (Exception e) { return def; }
    }

    private static boolean parseBool(String json, String key, boolean def) {
        try {
            String v = extractValue(json, key);
            return v != null ? Boolean.parseBoolean(v.trim()) : def;
        } catch (Exception e) { return def; }
    }

    private static String parseStr(String json, String key, String def) {
        try {
            String v = extractValue(json, key);
            return v != null ? v.replace("\"", "").trim() : def;
        } catch (Exception e) { return def; }
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        if (end < 0) end = json.length();
        return json.substring(start, end).trim();
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] b = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}