package entities;

public class Utilisateur {
    private int id;
    private String nomU;
    private String prenomU;
    private String emailU;
    private String mdpU;
    private String roleU;
    private boolean isActive;

    // Constructeur vide
    public Utilisateur() {}

    // Constructeur complet
    public Utilisateur(int id, String nomU, String prenomU, String emailU,
                       String mdpU, String roleU, boolean isActive) {
        this.id = id;
        this.nomU = nomU;
        this.prenomU = prenomU;
        this.emailU = emailU;
        this.mdpU = mdpU;
        this.roleU = roleU;
        this.isActive = isActive;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomU() { return nomU; }
    public void setNomU(String nomU) { this.nomU = nomU; }

    public String getPrenomU() { return prenomU; }
    public void setPrenomU(String prenomU) { this.prenomU = prenomU; }

    public String getEmailU() { return emailU; }
    public void setEmailU(String emailU) { this.emailU = emailU; }

    public String getMdpU() { return mdpU; }
    public void setMdpU(String mdpU) { this.mdpU = mdpU; }

    public String getRoleU() { return roleU; }
    public void setRoleU(String roleU) { this.roleU = roleU; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "Utilisateur{id=" + id + ", nom='" + nomU + "', prenom='" + prenomU +
                "', email='" + emailU + "', role='" + roleU + "', actif=" + isActive + "}";
    }
}