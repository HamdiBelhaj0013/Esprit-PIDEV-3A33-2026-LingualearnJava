package entities;

import java.time.LocalDate;

public class Evenement {
    private int id;
    private String titreE;
    private String descriptionEv;
    private String statutEv;
    private LocalDate dateEv;
    private String lieuE;
    private Double longitude;
    private Double latitude;
    private String imgEv;
    private int typeEvId;
    private boolean estPayant;
    private Integer prixMillimes;

    // Constructeur vide
    public Evenement() {}

    // Constructeur complet
    public Evenement(int id, String titreE, String descriptionEv, String statutEv,
                     LocalDate dateEv, String lieuE, Double longitude, Double latitude,
                     String imgEv, int typeEvId, boolean estPayant, Integer prixMillimes) {
        this.id = id;
        this.titreE = titreE;
        this.descriptionEv = descriptionEv;
        this.statutEv = statutEv;
        this.dateEv = dateEv;
        this.lieuE = lieuE;
        this.longitude = longitude;
        this.latitude = latitude;
        this.imgEv = imgEv;
        this.typeEvId = typeEvId;
        this.estPayant = estPayant;
        this.prixMillimes = prixMillimes;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitreE() { return titreE; }
    public void setTitreE(String titreE) { this.titreE = titreE; }

    public String getDescriptionEv() { return descriptionEv; }
    public void setDescriptionEv(String descriptionEv) { this.descriptionEv = descriptionEv; }

    public String getStatutEv() { return statutEv; }
    public void setStatutEv(String statutEv) { this.statutEv = statutEv; }

    public LocalDate getDateEv() { return dateEv; }
    public void setDateEv(LocalDate dateEv) { this.dateEv = dateEv; }

    public String getLieuE() { return lieuE; }
    public void setLieuE(String lieuE) { this.lieuE = lieuE; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public String getImgEv() { return imgEv; }
    public void setImgEv(String imgEv) { this.imgEv = imgEv; }

    public int getTypeEvId() { return typeEvId; }
    public void setTypeEvId(int typeEvId) { this.typeEvId = typeEvId; }

    public boolean isEstPayant() { return estPayant; }
    public void setEstPayant(boolean estPayant) { this.estPayant = estPayant; }

    public Integer getPrixMillimes() { return prixMillimes; }
    public void setPrixMillimes(Integer prixMillimes) { this.prixMillimes = prixMillimes; }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", titre='" + titreE + "', statut='" + statutEv +
                "', date=" + dateEv + ", lieu='" + lieuE + "', payant=" + estPayant + "}";
    }
}