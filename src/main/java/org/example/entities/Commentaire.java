package org.example.entities;

import java.time.LocalDateTime;

public class Commentaire {
    private int id;
    private String contenuC;
    private LocalDateTime dateCom;
    private int publicationId;

    // Constructeur vide
    public Commentaire() {}

    // Constructeur complet
    public Commentaire(int id, String contenuC, LocalDateTime dateCom, int publicationId) {
        this.id = id;
        this.contenuC = contenuC;
        this.dateCom = dateCom;
        this.publicationId = publicationId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenuC() { return contenuC; }
    public void setContenuC(String contenuC) { this.contenuC = contenuC; }

    public LocalDateTime getDateCom() { return dateCom; }
    public void setDateCom(LocalDateTime dateCom) { this.dateCom = dateCom; }

    public int getPublicationId() { return publicationId; }
    public void setPublicationId(int publicationId) { this.publicationId = publicationId; }

    @Override
    public String toString() {
        return "Commentaire{id=" + id + ", contenu='" + contenuC +
                "', dateCom=" + dateCom + ", publicationId=" + publicationId + "}";
    }
}