package org.example.entities;

public class Exercice {
    private int id;
    private String type;
    private String question;
    private String options;
    private String correctAnswer;
    private boolean aiGenerated;
    private boolean enabled;
    private int quizId;
    private String skillCodes;
    private int difficulty;

    // Constructeur vide
    public Exercice() {
    }

    // Constructeur avec paramètres (sans l'ID pour l'insertion)
    public Exercice(String type, String question, String options, String correctAnswer, boolean aiGenerated, boolean enabled, int quizId, String skillCodes, int difficulty) {
        this.type = type;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.aiGenerated = aiGenerated;
        this.enabled = enabled;
        this.quizId = quizId;
        this.skillCodes = skillCodes;
        this.difficulty = difficulty;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    public String getSkillCodes() { return skillCodes; }
    public void setSkillCodes(String skillCodes) { this.skillCodes = skillCodes; }

    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }

    @Override
    public String toString() {
        return question + " (" + type + ")";
    }
}
