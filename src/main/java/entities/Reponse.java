package entities;

public class Reponse {
    private int id;
    private int questionId;
    private String texte;
    private boolean correcte;

    public Reponse() {
    }

    public Reponse(int id, int questionId, String texte, boolean correcte) {
        this.id = id;
        this.questionId = questionId;
        this.texte = texte;
        this.correcte = correcte;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getTexte() {
        return texte;
    }

    public void setTexte(String texte) {
        this.texte = texte;
    }

    public boolean isCorrecte() {
        return correcte;
    }

    public void setCorrecte(boolean correcte) {
        this.correcte = correcte;
    }
}

