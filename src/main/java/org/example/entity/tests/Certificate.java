package org.example.entity.tests;

import java.time.LocalDateTime;

/**
 * Représente un certificat de réussite généré pour un utilisateur.
 * Chaque certificat a un UUID unique utilisé pour la vérification via l'API.
 */
public class Certificate {

    private Long          id;
    private String        uuid;         // UUID v4 pour URL vérification
    private Long          userId;
    private String        userFullName; // dénormalisé pour le PDF
    private String        userEmail;
    private String        niveau;       // BEGINNER / INTERMEDIATE / ADVANCED
    private String        languageName; // Français, English, etc.
    private float         scoreMoyen;  // score moyen du niveau (%)
    private String        pdfPath;     // chemin du PDF sur le disque
    private LocalDateTime issuedAt;
    private boolean       isValid = true;

    public Certificate() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId()                               { return id; }
    public void setId(Long id)                        { this.id = id; }

    public String getUuid()                           { return uuid; }
    public void setUuid(String uuid)                  { this.uuid = uuid; }

    public Long getUserId()                           { return userId; }
    public void setUserId(Long userId)                { this.userId = userId; }

    public String getUserFullName()                   { return userFullName; }
    public void setUserFullName(String n)             { this.userFullName = n; }

    public String getUserEmail()                      { return userEmail; }
    public void setUserEmail(String e)                { this.userEmail = e; }

    public String getNiveau()                         { return niveau; }
    public void setNiveau(String niveau)              { this.niveau = niveau; }

    public String getLanguageName()                   { return languageName; }
    public void setLanguageName(String l)             { this.languageName = l; }

    public float getScoreMoyen()                      { return scoreMoyen; }
    public void setScoreMoyen(float s)                { this.scoreMoyen = s; }

    public String getPdfPath()                        { return pdfPath; }
    public void setPdfPath(String p)                  { this.pdfPath = p; }

    public LocalDateTime getIssuedAt()                { return issuedAt; }
    public void setIssuedAt(LocalDateTime d)          { this.issuedAt = d; }

    public boolean isValid()                          { return isValid; }
    public void setValid(boolean valid)               { this.isValid = valid; }

    /** URL de vérification — utilisée dans le QR Code */
    public String getVerifyUrl() {
        return "http://localhost:9090/api/certificate/verify/" + uuid;
    }
}