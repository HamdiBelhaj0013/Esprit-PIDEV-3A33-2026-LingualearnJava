package org.example.service.user_managment;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class CaptchaServer {

    private static HttpServer server;
    private static final int PORT = 18081;

    public static synchronized String start() {
        if (server != null) {
            System.out.println("[CaptchaServer] Already running at " + getUrl());
            return getUrl();
        }

        // Two attempts — second attempt handles TCP TIME_WAIT after a clean shutdown
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);

                server.createContext("/captcha.html", exchange -> {
                    try (InputStream is = CaptchaServer.class
                            .getResourceAsStream("/captcha/captcha.html")) {

                        if (is == null) {
                            String msg = "captcha.html not found at /captcha/captcha.html";
                            byte[] msgBytes = msg.getBytes();
                            exchange.sendResponseHeaders(404, msgBytes.length);
                            exchange.getResponseBody().write(msgBytes);
                            exchange.getResponseBody().close();
                            System.err.println("[CaptchaServer] " + msg);
                            return;
                        }

                        byte[] bytes = is.readAllBytes();
                        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        exchange.getResponseHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
                        exchange.getResponseHeaders().add("Pragma", "no-cache");
                        exchange.getResponseHeaders().add("Expires", "0");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                        exchange.getResponseBody().close();
                        System.out.println("[CaptchaServer] Served captcha.html ("
                                + bytes.length + " bytes)");
                    }
                });

                server.setExecutor(null);
                server.start();
                System.out.println("[CaptchaServer] Started at " + getUrl());
                return getUrl();

            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Address already in use")) {
                    if (attempt == 1) {
                        System.out.println("[CaptchaServer] Port " + PORT
                                + " busy — waiting 3s for TIME_WAIT to clear (attempt " + attempt + "/2)...");
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        server = null; // reset before retry
                    } else {
                        // Port still busy after wait — a stale process is genuinely holding it.
                        // We can't serve fresh content so we log clearly and give up.
                        System.err.println("[CaptchaServer] Port " + PORT
                                + " still in use after wait. A stale Java process is running.");
                        System.err.println("[CaptchaServer] Run: taskkill /F /IM java.exe  then restart the app.");
                    }
                } else {
                    System.err.println("[CaptchaServer] Failed to start: " + e.getMessage());
                    break;
                }
            }
        }

        return getUrl();
    }

    public static String getUrl() {
        return "http://127.0.0.1:" + PORT + "/captcha.html";
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            System.out.println("[CaptchaServer] Stopped.");
        }
    }
}