package org.example.config;

public class ApiConfig {
    // 🔑 Gemini API Key - Récupérer de: https://ai.google.dev/
    public static final String GEMINI_API_KEY = "AIzaSyBDfqMrHRMT-QmxgdD_xMzrqza7t5Q4spQ";

    // API Endpoints
    public static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    // TTS Configuration
    public static final String GOOGLE_TTS_URL = "https://translate.google.com/translate_tts";

    // Export directories
    public static final String EXPORT_DIR = "exports";
    public static final String EXPORT_PDF_DIR = "exports/pdf";
    public static final String EXPORT_AUDIO_DIR = "exports/audio";
    public static final String EXPORT_RECORDINGS_DIR = "exports/recordings";
}