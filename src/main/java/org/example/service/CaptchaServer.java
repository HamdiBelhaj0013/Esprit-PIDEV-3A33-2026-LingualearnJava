package org.example.service;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class CaptchaServer {

    private static HttpServer server;
    private static final int PORT = 18080;
    private static boolean initialized = false;

    public static String start() throws IOException {
        if (server != null && initialized) {
            return getUrl();
        }

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);

            server.createContext("/captcha.html", exchange -> {
                try (InputStream is = CaptchaServer.class
                        .getResourceAsStream("/captcha/captcha.html")) {

                    if (is == null) {
                        String msg = "captcha.html not found";
                        exchange.sendResponseHeaders(404, msg.length());
                        exchange.getResponseBody().write(msg.getBytes());
                        exchange.getResponseBody().close();
                        return;
                    }

                    byte[] bytes = is.readAllBytes();
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                }
            });

            server.setExecutor(null);
            server.start();
            initialized = true;
            System.out.println("[CaptchaServer] Started at " + getUrl());
            return getUrl();

        } catch (IOException e) {
            if (e.getMessage().contains("Address already in use")) {
                System.err.println("[CaptchaServer] Port " + PORT + " already in use. Attempting to reuse...");
                initialized = true; // Mark as initialized to prevent retry loops
                return getUrl();
            }
            throw e;
        }
    }

    public static String getUrl() {
        return "http://127.0.0.1:" + PORT + "/captcha.html";
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            initialized = false;
            System.out.println("[CaptchaServer] Stopped.");
        }
    }
}