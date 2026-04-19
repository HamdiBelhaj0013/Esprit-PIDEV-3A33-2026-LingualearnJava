package org.example.services;

import org.json.JSONObject;

/**
 * Service d'amélioration de publications via GROQ (LLaMA 3).
 * Utilise la même clé API et service que le chatbot pour la cohérence.
 */
public class GeminiService {

    private final GroqService groqService = new GroqService();

    /**
     * Améliore le titre et le contenu d'une publication via GROQ.
     * Retourne [nouveauTitre, nouveauContenu].
     */
    public String[] ameliorerPublication(String titre, String contenu) throws Exception {
        String systemPrompt = "Tu es un expert en rédaction. Améliore le titre et le contenu pour qu'ils soient clairs, engageants et professionnels. "
                + "Réponds UNIQUEMENT en JSON valide avec les clés 'titre' et 'contenu'. Aucun texte avant ou après le JSON.";

        String userMessage = "Titre : " + titre + "\nContenu : " + contenu;

        String rawText = groqService.chat(systemPrompt, userMessage);

        // Nettoyer le markdown si présent
        rawText = rawText.replaceAll("(?s)```json\\s*", "")
                         .replaceAll("(?s)```\\s*", "")
                         .trim();

        System.out.println("🔍 Réponse IA brute : " + rawText);

        // Parser le JSON avec org.json (déjà dans pom.xml)
        try {
            JSONObject json = new JSONObject(rawText);
            String newTitre = json.optString("titre", titre);
            String newContenu = json.optString("contenu", contenu);

            System.out.println("✅ Amélioration appliquée");
            System.out.println("  Titre : " + newTitre);
            System.out.println("  Contenu : " + newContenu);

            return new String[]{newTitre, newContenu};

        } catch (Exception e) {
            System.out.println("⚠️ Erreur parsing JSON: " + e.getMessage());
            System.out.println("  Texte non valide: " + rawText);
            throw new Exception("Impossible de parser la réponse IA. Vérifiez le format JSON.");
        }
    }
}
