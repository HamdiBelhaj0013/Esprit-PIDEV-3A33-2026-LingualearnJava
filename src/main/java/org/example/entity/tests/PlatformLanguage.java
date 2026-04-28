package org.example.entity.tests;

public class PlatformLanguage {

    private Long    id;
    private String  name;
    private String  code;
    private String  flagUrl;
    private boolean isEnabled = true;

    public PlatformLanguage() {}

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public String getCode()                     { return code; }
    public void setCode(String code)            { this.code = code; }
    public String getFlagUrl()                  { return flagUrl; }
    public void setFlagUrl(String flagUrl)      { this.flagUrl = flagUrl; }
    public boolean isEnabled()                  { return isEnabled; }
    public void setEnabled(boolean enabled)     { this.isEnabled = enabled; }

    @Override
    public String toString() {
        return name != null ? name : "PlatformLanguage#" + id;
    }
}
