package org.example.service.tests;

import org.example.entity.tests.MockTest;
import org.example.entity.tests.TestQuestion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
/**
 * API #3 — ApiNinjasService
 *
 * Génère des questions linguistiques dans la LANGUE du test ciblé.
 * Si le test est en Français → questions en français.
 * Si le test est en Anglais  → questions en anglais.
 * etc.
 *
 * Endpoints :
 *   /v1/thesaurus?word={mot}   → Synonymes & Antonymes
 *   /v1/dictionary?word={mot}  → Définitions & Parties du discours
 *   /v1/wordoftheday           → Mot du jour en contexte
 */
public class ApiNinjasService {

    private static final Logger LOG     = Logger.getLogger(ApiNinjasService.class.getName());
    private static final String API_KEY = "lDFd15bXonrKuSO3gOSOTWYy5L7CJxSNgV2YWTMZ";
    private static final String BASE    = "https://api.api-ninjas.com/v1";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record QuestionGeneree(
            String questionText,
            List<String> options,
            String correctAnswer,
            String typeQuestion,
            String source
    ) {}

    // ── Détection de la langue ────────────────────────────────────────────────

    /**
     * Détecte la langue du test depuis platformLanguage.getName()
     * et retourne un code : "fr", "en", "es", "de", "ar", "it"
     */
    private String detecterLangue(MockTest test) {
        if (test.getPlatformLanguage() == null) return "en";
        String lang = test.getPlatformLanguage().getName().toLowerCase();
        if (lang.contains("fran") || lang.contains("french")) return "fr";
        if (lang.contains("english") || lang.contains("anglais"))  return "en";
        if (lang.contains("espagnol") || lang.contains("spanish")) return "es";
        if (lang.contains("allemand") || lang.contains("german"))  return "de";
        if (lang.contains("arabe")   || lang.contains("arabic"))   return "ar";
        if (lang.contains("italien") || lang.contains("italian"))  return "it";
        return "en"; // défaut
    }

    // ── Mots par langue ───────────────────────────────────────────────────────

    /**
     * Mots de vocabulaire courants selon la langue.
     * Pour FR/ES/DE/IT : on utilise des mots anglais équivalents pour l'API
     * mais on traduit les questions et les options dans la langue cible.
     *
     * Note : L'API Ninjas ne supporte que l'anglais pour thesaurus/dictionary.
     * On traduit donc les questions générées dans la langue du test.
     */
    private List<String> getMotsVocabulaire(String langCode) {
        // L'API Ninjas travaille en anglais — on utilise des mots anglais courants
        // et on traduit les questions autour
        return switch (langCode) {
            case "fr", "es", "de", "it" -> List.of(
                    "happy", "sad", "beautiful", "difficult", "brave",
                    "fast", "ancient", "simple", "curious", "strong",
                    "wise", "kind", "calm", "bright", "clear"
            );
            case "ar" -> List.of(
                    "happy", "sad", "beautiful", "difficult", "brave",
                    "fast", "ancient", "simple", "curious", "strong"
            );
            default -> List.of( // anglais
                    "eloquent", "resilient", "ambiguous", "verbose",
                    "coherent", "profound", "vivid", "concise",
                    "fluent", "articulate", "lucid", "precise",
                    "subtle", "candid", "earnest"
            );
        };
    }

    private List<String> getMotsGrammaire(String langCode) {
        return switch (langCode) {
            case "fr", "es", "de", "it" -> List.of(
                    "elegant", "fluent", "precise", "coherent",
                    "vivid", "concise", "subtle", "candid"
            );
            case "ar" -> List.of(
                    "elegant", "fluent", "precise", "coherent"
            );
            default -> List.of(
                    "eloquent", "resilient", "ambiguous", "verbose",
                    "coherent", "profound", "vivid", "concise"
            );
        };
    }

    // ── Templates de questions par langue ─────────────────────────────────────

    private record Templates(
            String synonyme,
            String antonyme,
            String definition,
            String partieDiscours,
            String motDuJour,
            String motDuJourContexte,
            List<String> partiesDiscours  // traductions des parties du discours
    ) {}

    private Templates getTemplates(String langCode) {
        return switch (langCode) {

            case "fr" -> new Templates(
                    "Quel mot est un synonyme de \"%s\" ?",
                    "Quel mot est un antonyme (contraire) de \"%s\" ?",
                    "Quelle est la définition correcte du mot \"%s\" ?",
                    "Dans la phrase : \"%s\" — quelle est la nature grammaticale de \"%s\" ?",
                    "Quelle est la signification du mot \"%s\" ?",
                    "Lisez cette phrase : \"%s\"\nQue signifie le mot \"%s\" ?",
                    List.of("nom", "verbe", "adjectif", "adverbe",
                            "pronom", "préposition", "conjonction")
            );

            case "es" -> new Templates(
                    "¿Qué palabra es sinónimo de \"%s\" ?",
                    "¿Qué palabra es antónimo de \"%s\" ?",
                    "¿Cuál es la definición correcta de la palabra \"%s\" ?",
                    "En la frase: \"%s\" — ¿qué función gramatical tiene \"%s\" ?",
                    "¿Cuál es el significado de la palabra \"%s\" ?",
                    "Lee esta frase: \"%s\"\n¿Qué significa \"%s\" ?",
                    List.of("sustantivo", "verbo", "adjetivo", "adverbio",
                            "pronombre", "preposición", "conjunción")
            );

            case "de" -> new Templates(
                    "Welches Wort ist ein Synonym für \"%s\" ?",
                    "Welches Wort ist ein Antonym für \"%s\" ?",
                    "Was ist die korrekte Definition des Wortes \"%s\" ?",
                    "Im Satz: \"%s\" — welche Wortart ist \"%s\" ?",
                    "Was bedeutet das Wort \"%s\" ?",
                    "Lesen Sie diesen Satz: \"%s\"\nWas bedeutet \"%s\" ?",
                    List.of("Substantiv", "Verb", "Adjektiv", "Adverb",
                            "Pronomen", "Präposition", "Konjunktion")
            );

            case "it" -> new Templates(
                    "Quale parola è un sinonimo di \"%s\" ?",
                    "Quale parola è un antonimo di \"%s\" ?",
                    "Qual è la definizione corretta della parola \"%s\" ?",
                    "Nella frase: \"%s\" — che parte del discorso è \"%s\" ?",
                    "Qual è il significato della parola \"%s\" ?",
                    "Leggi questa frase: \"%s\"\nCosa significa \"%s\" ?",
                    List.of("sostantivo", "verbo", "aggettivo", "avverbio",
                            "pronome", "preposizione", "congiunzione")
            );

            case "ar" -> new Templates(
                    "ما هي الكلمة المرادفة لـ \"%s\" ؟",
                    "ما هي كلمة المضاد لـ \"%s\" ؟",
                    "ما هو التعريف الصحيح لكلمة \"%s\" ؟",
                    "في الجملة: \"%s\" — ما هو نوع الكلمة \"%s\" ؟",
                    "ما معنى الكلمة \"%s\" ؟",
                    "اقرأ هذه الجملة: \"%s\"\nماذا تعني كلمة \"%s\" ؟",
                    List.of("اسم", "فعل", "صفة", "ظرف", "ضمير", "حرف جر", "أداة عطف")
            );

            default -> new Templates( // anglais
                    "Which word is a synonym of \"%s\" ?",
                    "Which word is an antonym (opposite) of \"%s\" ?",
                    "What is the correct definition of the word \"%s\" ?",
                    "In the sentence: \"%s\" — what part of speech is \"%s\" ?",
                    "What is the meaning of the word \"%s\" ?",
                    "Read this sentence: \"%s\"\nWhat does the word \"%s\" mean ?",
                    List.of("noun", "verb", "adjective", "adverb",
                            "pronoun", "preposition", "conjunction")
            );
        };
    }

    // ── Dictionnaires de traduction mot anglais → français/espagnol/etc. ──────
    // Couvre les mots utilisés dans getMotsVocabulaire() et leurs synonymes courants

    private static final Map<String, Map<String, String>> TRADUCTIONS = new HashMap<>();
    static {
        // Format : mot_anglais → { "fr": traduction, "es": traduction, "de": traduction, "it": traduction }
        Map<String, Map<String, String>> t = TRADUCTIONS;

        t.put("happy",       Map.of("fr","heureux",    "es","feliz",       "de","glücklich",  "it","felice"));
        t.put("sad",         Map.of("fr","triste",     "es","triste",      "de","traurig",    "it","triste"));
        t.put("beautiful",   Map.of("fr","beau",       "es","hermoso",     "de","schön",      "it","bello"));
        t.put("difficult",   Map.of("fr","difficile",  "es","difícil",     "de","schwierig",  "it","difficile"));
        t.put("brave",       Map.of("fr","courageux",  "es","valiente",    "de","mutig",      "it","coraggioso"));
        t.put("fast",        Map.of("fr","rapide",     "es","rápido",      "de","schnell",    "it","veloce"));
        t.put("ancient",     Map.of("fr","ancien",     "es","antiguo",     "de","alt",        "it","antico"));
        t.put("simple",      Map.of("fr","simple",     "es","simple",      "de","einfach",    "it","semplice"));
        t.put("curious",     Map.of("fr","curieux",    "es","curioso",     "de","neugierig",  "it","curioso"));
        t.put("strong",      Map.of("fr","fort",       "es","fuerte",      "de","stark",      "it","forte"));
        t.put("wise",        Map.of("fr","sage",       "es","sabio",       "de","weise",      "it","saggio"));
        t.put("kind",        Map.of("fr","gentil",     "es","amable",      "de","freundlich", "it","gentile"));
        t.put("calm",        Map.of("fr","calme",      "es","tranquilo",   "de","ruhig",      "it","calmo"));
        t.put("bright",      Map.of("fr","brillant",   "es","brillante",   "de","hell",       "it","brillante"));
        t.put("clear",       Map.of("fr","clair",      "es","claro",       "de","klar",       "it","chiaro"));
        // Synonymes/antonymes courants retournés par l'API
        t.put("fortunate",   Map.of("fr","chanceux",   "es","afortunado",  "de","glücklich",  "it","fortunato"));
        t.put("unhappy",     Map.of("fr","malheureux", "es","infeliz",     "de","unglücklich","it","infelice"));
        t.put("lovely",      Map.of("fr","charmant",   "es","encantador",  "de","wunderschön","it","incantevole"));
        t.put("ugly",        Map.of("fr","laid",       "es","feo",         "de","hässlich",   "it","brutto"));
        t.put("challenging", Map.of("fr","difficile",  "es","desafiante",  "de","anspruchsvoll","it","impegnativo"));
        t.put("easy",        Map.of("fr","facile",     "es","fácil",       "de","leicht",     "it","facile"));
        t.put("courageous",  Map.of("fr","courageux",  "es","valiente",    "de","mutig",      "it","coraggioso"));
        t.put("coward",      Map.of("fr","lâche",      "es","cobarde",     "de","feige",      "it","codardo"));
        t.put("quick",       Map.of("fr","rapide",     "es","rápido",      "de","schnell",    "it","veloce"));
        t.put("slow",        Map.of("fr","lent",       "es","lento",       "de","langsam",    "it","lento"));
        t.put("joyful",      Map.of("fr","joyeux",     "es","alegre",      "de","fröhlich",   "it","gioioso"));
        t.put("sorrowful",   Map.of("fr","chagriné",   "es","afligido",    "de","traurig",    "it","addolorato"));
        t.put("gorgeous",    Map.of("fr","magnifique", "es","precioso",    "de","wunderschön","it","stupendo"));
        t.put("plain",       Map.of("fr","ordinaire",  "es","sencillo",    "de","schlicht",   "it","semplice"));
        t.put("bold",        Map.of("fr","audacieux",  "es","audaz",       "de","mutig",      "it","audace"));
        t.put("timid",       Map.of("fr","timide",     "es","tímido",      "de","schüchtern", "it","timido"));
        t.put("old",         Map.of("fr","vieux",      "es","viejo",       "de","alt",        "it","vecchio"));
        t.put("modern",      Map.of("fr","moderne",    "es","moderno",     "de","modern",     "it","moderno"));
        t.put("complex",     Map.of("fr","complexe",   "es","complejo",    "de","komplex",    "it","complesso"));
        t.put("inquisitive", Map.of("fr","curieux",    "es","curioso",     "de","neugierig",  "it","curioso"));
        t.put("obscure",     Map.of("fr","obscur",     "es","oscuro",      "de","obskur",     "it","oscuro"));
        t.put("trivial",     Map.of("fr","trivial",    "es","trivial",     "de","trivial",    "it","triviale"));
        t.put("mundane",     Map.of("fr","banal",      "es","mundano",     "de","alltäglich", "it","banale"));
        t.put("archaic",     Map.of("fr","archaïque",  "es","arcaico",     "de","archaisch",  "it","arcaico"));
        t.put("verbose",     Map.of("fr","verbeux",    "es","verboso",     "de","weitschweifig","it","verboso"));
        t.put("terse",       Map.of("fr","concis",     "es","conciso",     "de","knapp",      "it","conciso"));
        t.put("laconic",     Map.of("fr","laconique",  "es","lacónico",    "de","lakonisch",  "it","laconico"));
        t.put("pedantic",    Map.of("fr","pédant",     "es","pedante",     "de","pedantisch", "it","pedante"));
    }

    /**
     * Traduit un mot anglais dans la langue cible.
     * Si pas de traduction connue → retourne le mot anglais tel quel.
     */
    private String traduire(String motAnglais, String langCode) {
        if ("en".equals(langCode)) return motAnglais;
        Map<String, String> trad = TRADUCTIONS.get(motAnglais.toLowerCase().trim());
        if (trad != null && trad.containsKey(langCode))
            return trad.get(langCode);
        return motAnglais; // fallback anglais si pas de traduction
    }

    /**
     * Traduit une liste de mots.
     */
    private List<String> traduireListe(List<String> mots, String langCode) {
        if ("en".equals(langCode)) return mots;
        List<String> traduits = new ArrayList<>();
        for (String m : mots) traduits.add(traduire(m, langCode));
        return traduits;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ① VOCABULAIRE — Synonymes & Antonymes
    // ════════════════════════════════════════════════════════════════════════

    public List<QuestionGeneree> genererQuestionsVocabulaireSynonymes(
            List<String> mots, String langCode) {

        List<QuestionGeneree> questions = new ArrayList<>();
        Templates t = getTemplates(langCode);

        for (String mot : mots) {
            try {
                String json = get(BASE + "/thesaurus?word=" + mot.toLowerCase().trim());
                if (json == null || json.isBlank()) continue;

                List<String> synonymes = extraireListeChamp(json, "synonyms");
                List<String> antonymes = extraireListeChamp(json, "antonyms");

                // Mot à afficher dans la question (traduit si non-anglais)
                String motAffiche = traduire(mot, langCode);

                // Question A : Synonyme
                if (synonymes.size() >= 2) {
                    String bonneEn  = synonymes.get(0);
                    String bonneTrad = traduire(bonneEn, langCode);

                    List<String> options = new ArrayList<>();
                    options.add(bonneTrad);
                    for (int i = 1; i < Math.min(2, synonymes.size()); i++)
                        options.add(traduire(synonymes.get(i), langCode));
                    if (!antonymes.isEmpty())
                        options.add(traduire(antonymes.get(0), langCode));
                    while (options.size() < 4) options.add(motLeurreVocab(langCode));
                    options = new ArrayList<>(options.subList(0, 4));
                    // Supprimer les doublons
                    options = new ArrayList<>(new LinkedHashSet<>(options));
                    while (options.size() < 4) options.add(motLeurreVocab(langCode));
                    Collections.shuffle(options);

                    questions.add(new QuestionGeneree(
                            String.format(t.synonyme(), motAffiche),
                            options, bonneTrad, "VOCABULAIRE", "thesaurus/synonyms"
                    ));
                }

                // Question B : Antonyme
                if (antonymes.size() >= 2) {
                    String bonneEn   = antonymes.get(0);
                    String bonneTrad = traduire(bonneEn, langCode);

                    List<String> options = new ArrayList<>();
                    options.add(bonneTrad);
                    for (int i = 1; i < Math.min(2, antonymes.size()); i++)
                        options.add(traduire(antonymes.get(i), langCode));
                    if (!synonymes.isEmpty())
                        options.add(traduire(synonymes.get(0), langCode));
                    while (options.size() < 4) options.add(motLeurreVocab(langCode));
                    options = new ArrayList<>(options.subList(0, 4));
                    options = new ArrayList<>(new LinkedHashSet<>(options));
                    while (options.size() < 4) options.add(motLeurreVocab(langCode));
                    Collections.shuffle(options);

                    questions.add(new QuestionGeneree(
                            String.format(t.antonyme(), motAffiche),
                            options, bonneTrad, "VOCABULAIRE", "thesaurus/antonyms"
                    ));
                }

                Thread.sleep(300);
            } catch (Exception e) {
                LOG.warning("thesaurus/" + mot + " : " + e.getMessage());
            }
        }
        return questions;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ② GRAMMAIRE — Définitions & Parties du discours
    // ════════════════════════════════════════════════════════════════════════

    public List<QuestionGeneree> genererQuestionsGrammaireDictionnaire(
            List<String> mots, String langCode) {

        List<QuestionGeneree> questions = new ArrayList<>();
        Templates t = getTemplates(langCode);

        for (String mot : mots) {
            try {
                String json = get(BASE + "/dictionary?word=" + mot.toLowerCase().trim());
                if (json == null || json.isBlank()) continue;

                String definition   = extraireChamp(json, "definition");
                String partOfSpeech = extraireChamp(json, "part_of_speech");
                String example      = extraireChamp(json, "example");

                // Question A : Définition
                if (definition != null && definition.length() >= 10) {
                    if (definition.length() > 130)
                        definition = definition.substring(0, 127) + "...";
                    List<String> options = genererDefinitionsLeurres(3);
                    options.add(definition);
                    Collections.shuffle(options);
                    questions.add(new QuestionGeneree(
                            String.format(t.definition(), mot),
                            options, definition, "VOCABULAIRE", "dictionary/definition"
                    ));
                }

                // Question B : Partie du discours (traduite dans la langue cible)
                if (partOfSpeech != null && !partOfSpeech.isBlank()) {
                    // Traduire la partie du discours dans la langue cible
                    String partTraduite = traduirePartieDiscours(partOfSpeech, t.partiesDiscours());
                    List<String> options = new ArrayList<>(t.partiesDiscours());
                    // S'assurer que la bonne réponse est dans les options
                    if (!options.contains(partTraduite)) options.add(partTraduite);
                    options.removeIf(p -> p.equals(partTraduite));
                    Collections.shuffle(options);
                    List<String> finalOptions = new ArrayList<>();
                    finalOptions.add(partTraduite);
                    finalOptions.addAll(options.subList(0, Math.min(3, options.size())));
                    Collections.shuffle(finalOptions);

                    String qText = (example != null && !example.isBlank())
                            ? String.format(t.partieDiscours(), example, mot)
                            : String.format(t.definition(), mot)
                            .replace("définition correcte", "nature grammaticale")
                            .replace("correct definition", "part of speech");

                    questions.add(new QuestionGeneree(
                            qText, finalOptions, partTraduite,
                            "GRAMMAIRE", "dictionary/part_of_speech"
                    ));
                }

                Thread.sleep(300);
            } catch (Exception e) {
                LOG.warning("dictionary/" + mot + " : " + e.getMessage());
            }
        }
        return questions;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ③ MOT DU JOUR
    // ════════════════════════════════════════════════════════════════════════

    public QuestionGeneree genererQuestionMotDuJour(String langCode) {
        try {
            String json = get(BASE + "/wordoftheday");
            if (json == null || json.isBlank()) return null;

            String word       = extraireChamp(json, "word");
            String definition = extraireChamp(json, "definition");
            String example    = extraireChamp(json, "example");

            if (word == null || definition == null) return null;
            if (definition.length() > 130) definition = definition.substring(0, 127) + "...";

            Templates t = getTemplates(langCode);
            String qText = (example != null && !example.isBlank())
                    ? String.format(t.motDuJourContexte(), example, word)
                    : String.format(t.motDuJour(), word);

            List<String> options = genererDefinitionsLeurres(3);
            options.add(definition);
            Collections.shuffle(options);

            return new QuestionGeneree(
                    qText, options, definition,
                    "VOCABULAIRE", "wordoftheday"
            );

        } catch (Exception e) {
            LOG.warning("wordoftheday : " + e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE — Générer et sauvegarder dans un test
    // ════════════════════════════════════════════════════════════════════════

    public int genererEtSauvegarder(MockTest test,
                                    MockTestService questionService,
                                    String typeGeneration,
                                    int nbQuestions,
                                    int pointsParQ) {

        // ── Détecter la langue du test automatiquement ────────────────────────
        String langCode = detecterLangue(test);
        LOG.info("Langue détectée : " + langCode + " pour le test : " + test.getTitle());

        List<QuestionGeneree> generees = new ArrayList<>();

        switch (typeGeneration) {

            case "VOCABULAIRE" -> {
                List<String> mots = getMotsVocabulaire(langCode);
                generees.addAll(genererQuestionsVocabulaireSynonymes(
                        mots.subList(0, Math.min(nbQuestions, mots.size())), langCode));
            }

            case "GRAMMAIRE" -> {
                List<String> mots = getMotsGrammaire(langCode);
                generees.addAll(genererQuestionsGrammaireDictionnaire(
                        mots.subList(0, Math.min(nbQuestions, mots.size())), langCode));
            }

            case "MOT_DU_JOUR" -> {
                QuestionGeneree motJour = genererQuestionMotDuJour(langCode);
                if (motJour != null) generees.add(motJour);
                if (nbQuestions > 1) {
                    List<String> mots = getMotsVocabulaire(langCode);
                    generees.addAll(genererQuestionsVocabulaireSynonymes(
                            mots.subList(0, Math.min(nbQuestions - 1, mots.size())), langCode));
                }
            }

            case "MIXTE" -> {
                int parType = Math.max(1, nbQuestions / 3);
                int reste   = nbQuestions - (parType * 3);

                // Vocabulaire
                List<String> mv = getMotsVocabulaire(langCode);
                generees.addAll(genererQuestionsVocabulaireSynonymes(
                        mv.subList(0, Math.min(parType, mv.size())), langCode));

                // Grammaire
                List<String> mg = getMotsGrammaire(langCode);
                generees.addAll(genererQuestionsGrammaireDictionnaire(
                        mg.subList(0, Math.min(parType, mg.size())), langCode));

                // Mot du jour
                QuestionGeneree motJour = genererQuestionMotDuJour(langCode);
                if (motJour != null) generees.add(motJour);

                // Reste → vocabulaire
                if (reste > 0) {
                    List<String> mr = getMotsVocabulaire(langCode);
                    generees.addAll(genererQuestionsVocabulaireSynonymes(
                            mr.subList(0, Math.min(reste, mr.size())), langCode));
                }
            }

            default -> {
                List<String> mv = getMotsVocabulaire(langCode);
                generees.addAll(genererQuestionsVocabulaireSynonymes(
                        mv.subList(0, Math.min(nbQuestions, mv.size())), langCode));
                QuestionGeneree motJour = genererQuestionMotDuJour(langCode);
                if (motJour != null) generees.add(motJour);
            }
        }

        // Sauvegarder
        int count = 0;
        for (QuestionGeneree q : generees) {
            try {
                TestQuestion tq = new TestQuestion();
                tq.setMockTest(test);
                tq.setSectionCategory("Reading");
                tq.setQuestionType("Reading");
                tq.setQuestionText(q.questionText());
                tq.setOptions(toJsonArray(q.options()));
                tq.setCorrectAnswer(q.correctAnswer());
                tq.setPoints(pointsParQ);
                tq.setActive(true);
                questionService.createQuestion(tq);
                count++;
            } catch (Exception e) {
                LOG.warning("Sauvegarde question échouée : " + e.getMessage());
            }
        }

        LOG.info("API Ninjas [" + langCode.toUpperCase() + "] : "
                + count + " questions sauvegardées.");
        return count;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Traduit la partie du discours anglaise vers la langue cible */
    private String traduirePartieDiscours(String partOfSpeech, List<String> partieTraduite) {
        Map<String, Integer> mapping = new LinkedHashMap<>();
        mapping.put("noun",        0);
        mapping.put("verb",        1);
        mapping.put("adjective",   2);
        mapping.put("adverb",      3);
        mapping.put("pronoun",     4);
        mapping.put("preposition", 5);
        mapping.put("conjunction", 6);
        Integer idx = mapping.get(partOfSpeech.toLowerCase().trim());
        if (idx != null && idx < partieTraduite.size())
            return partieTraduite.get(idx);
        return partOfSpeech; // retourner en anglais si pas de traduction
    }

    private List<String> genererDefinitionsLeurres(int nb) {
        List<String> leurres = new ArrayList<>(List.of(
                "A formal agreement between two or more parties.",
                "The process of transforming raw materials into finished goods.",
                "A recurring pattern observed in natural phenomena.",
                "The act of expressing ideas through spoken or written language.",
                "A systematic approach to solving complex problems.",
                "The quality of being clear and easy to understand.",
                "An ancient practice used in ceremonial rituals.",
                "A scientific measurement of atmospheric pressure."
        ));
        Collections.shuffle(leurres);
        return new ArrayList<>(leurres.subList(0, Math.min(nb, leurres.size())));
    }

    private String motLeurreVocab(String langCode) {
        Map<String, List<String>> leurresParLangue = Map.of(
                "fr", List.of("obscur","banal","timide","lent","vieux","ordinaire","complexe","sage"),
                "es", List.of("oscuro","trivial","tímido","lento","viejo","sencillo","complejo","sabio"),
                "de", List.of("obskur","alltäglich","schüchtern","langsam","alt","schlicht","komplex","weise"),
                "it", List.of("oscuro","banale","timido","lento","vecchio","semplice","complesso","saggio"),
                "ar", List.of("غامض","بسيط","خجول","بطيء","قديم","عادي","معقد","حكيم"),
                "en", List.of("obscure","trivial","timid","slow","old","plain","complex","wise")
        );
        List<String> pool = leurresParLangue.getOrDefault(langCode,
                leurresParLangue.get("en"));
        return pool.get(new Random().nextInt(pool.size()));
    }

    // Surcharge pour compatibilité
    private String motLeurreVocab() { return motLeurreVocab("en"); }

    private String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", API_KEY)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return resp.body();
        LOG.warning("API Ninjas HTTP " + resp.statusCode() + " — " + url);
        return null;
    }

    private String extraireChamp(String json, String champ) {
        String search = "\"" + champ + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(":", idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' '
                || json.charAt(start) == '"')) start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != '"'
                && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        String val = json.substring(start, end).trim();
        return val.isBlank() ? null : val;
    }

    private List<String> extraireListeChamp(String json, String champ) {
        List<String> result = new ArrayList<>();
        String search = "\"" + champ + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return result;
        int debut = json.indexOf("[", idx);
        int fin   = json.indexOf("]", debut);
        if (debut < 0 || fin < 0) return result;
        String inner = json.substring(debut + 1, fin);
        for (String part : inner.split(",")) {
            String s = part.trim().replace("\"", "");
            if (!s.isBlank()) result.add(s);
        }
        return result;
    }

    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(items.get(i).replace("\"", "'")).append("\"");
        }
        return sb.append("]").toString();
    }
}