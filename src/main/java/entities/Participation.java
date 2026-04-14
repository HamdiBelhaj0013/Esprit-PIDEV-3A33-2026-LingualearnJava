package entities;

import java.time.LocalDate;

public class Participation {
    private int id;
    private LocalDate dateInscri;
    private String statutParti;
    private int nbPlaces;
    private int evenementId;
    private int utilisateurId;

    // Constructeur vide
    public Participation() {}

    // Constructeur complet
    public Participation(int id, LocalDate dateInscri, String statutParti,
                         int nbPlaces, int evenementId, int utilisateurId) {
        this.id = id;
        this.dateInscri = dateInscri;
        this.statutParti = statutParti;
        this.nbPlaces = nbPlaces;
        this.evenementId = evenementId;
        this.utilisateurId = utilisateurId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDateInscri() { return dateInscri; }
    public void setDateInscri(LocalDate dateInscri) { this.dateInscri = dateInscri; }

    public String getStatutParti() { return statutParti; }
    public void setStatutParti(String statutParti) { this.statutParti = statutParti; }

    public int getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(int nbPlaces) { this.nbPlaces = nbPlaces; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    @Override
    public String toString() {
        return "Participation{id=" + id + ", date='" + dateInscri + "', statut='" +
                statutParti + "', nbPlaces=" + nbPlaces + ", evenementId=" + evenementId +
                ", utilisateurId=" + utilisateurId + "}";
    }
}