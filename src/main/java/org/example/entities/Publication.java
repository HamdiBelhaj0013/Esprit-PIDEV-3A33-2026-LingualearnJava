package org.example.entities;

import java.time.LocalDateTime;

public class Publication {
    private int id;
    private String titrePub;
    private String typePub;
    private String lienPub;
    private String contenuPub;
    private LocalDateTime datePub;
    private int likes;
    private int dislikes;
    private int reportPub;
    private int floue;
    private int utilisateurId;

    // Constructeur vide
    public Publication() {}

    // Constructeur complet
    public Publication(int id, String titrePub, String typePub, String lienPub,
                       String contenuPub, LocalDateTime datePub, int likes,
                       int dislikes, int reportPub, int floue, int utilisateurId) {
        this.id = id;
        this.titrePub = titrePub;
        this.typePub = typePub;
        this.lienPub = lienPub;
        this.contenuPub = contenuPub;
        this.datePub = datePub;
        this.likes = likes;
        this.dislikes = dislikes;
        this.reportPub = reportPub;
        this.floue = floue;
        this.utilisateurId = utilisateurId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitrePub() { return titrePub; }
    public void setTitrePub(String titrePub) { this.titrePub = titrePub; }

    public String getTypePub() { return typePub; }
    public void setTypePub(String typePub) { this.typePub = typePub; }

    public String getLienPub() { return lienPub; }
    public void setLienPub(String lienPub) { this.lienPub = lienPub; }

    public String getContenuPub() { return contenuPub; }
    public void setContenuPub(String contenuPub) { this.contenuPub = contenuPub; }

    public LocalDateTime getDatePub() { return datePub; }
    public void setDatePub(LocalDateTime datePub) { this.datePub = datePub; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }

    public int getReportPub() { return reportPub; }
    public void setReportPub(int reportPub) { this.reportPub = reportPub; }

    public int getFloue() { return floue; }
    public void setFloue(int floue) { this.floue = floue; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    @Override
    public String toString() {
        return "Publication{id=" + id + ", titre='" + titrePub + "', type='" + typePub +
                "', likes=" + likes + ", dislikes=" + dislikes + "}";
    }
}