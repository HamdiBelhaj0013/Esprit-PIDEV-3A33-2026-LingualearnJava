package org.example.entities;

public class PlatformLanguage {

    private int id;
    private String name;
    private String code;
    private String flagUrl;
    private boolean isEnabled;

    public PlatformLanguage() {
    }

    public PlatformLanguage(String name, String code, String flagUrl, boolean isEnabled) {
        this.name = name;
        this.code = code;
        this.flagUrl = flagUrl;
        this.isEnabled = isEnabled;
    }

    public PlatformLanguage(int id, String name, String code, String flagUrl, boolean isEnabled) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.flagUrl = flagUrl;
        this.isEnabled = isEnabled;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFlagUrl() {
        return flagUrl;
    }

    public void setFlagUrl(String flagUrl) {
        this.flagUrl = flagUrl;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}