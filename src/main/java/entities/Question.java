package entities;

public class Question {
    private int id;
    private String enonce;
    private String domaine;
    private String niveau;
    private boolean actif;

    public Question() {
    }

    public Question(int id, String enonce, String domaine, String niveau, boolean actif) {
        this.id = id;
        this.enonce = enonce;
        this.domaine = domaine;
        this.niveau = niveau;
        this.actif = actif;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEnonce() {
        return enonce;
    }

    public void setEnonce(String enonce) {
        this.enonce = enonce;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}

