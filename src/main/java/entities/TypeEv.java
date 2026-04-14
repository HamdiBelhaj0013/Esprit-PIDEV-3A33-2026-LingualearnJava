package entities;

public class TypeEv {
    private int id;
    private String nomType;
    private String descriptionT;
    private String modeE;
    private String niveauType;

    // Constructeur vide
    public TypeEv() {}

    // Constructeur complet
    public TypeEv(int id, String nomType, String descriptionT,
                  String modeE, String niveauType) {
        this.id = id;
        this.nomType = nomType;
        this.descriptionT = descriptionT;
        this.modeE = modeE;
        this.niveauType = niveauType;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomType() { return nomType; }
    public void setNomType(String nomType) { this.nomType = nomType; }

    public String getDescriptionT() { return descriptionT; }
    public void setDescriptionT(String descriptionT) { this.descriptionT = descriptionT; }

    public String getModeE() { return modeE; }
    public void setModeE(String modeE) { this.modeE = modeE; }

    public String getNiveauType() { return niveauType; }
    public void setNiveauType(String niveauType) { this.niveauType = niveauType; }

    @Override
    public String toString() {
        return "TypeEv{id=" + id + ", nom='" + nomType + "', mode='" + modeE +
                "', niveau='" + niveauType + "'}";
    }
}