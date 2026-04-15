package org.example.services;

import org.example.entities.PlatformLanguage;
import org.example.interfaces.CrudService;
import org.example.utils.MyDataBase;
import org.example.validators.PlatformLanguageValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlatformLanguageService implements CrudService<PlatformLanguage> {

    private final Connection cnx;

    public PlatformLanguageService() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(PlatformLanguage language) throws SQLException {
        List<String> errors = PlatformLanguageValidator.validate(language);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "INSERT INTO platform_language(name, code, flag_url, is_enabled) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, language.getName());
        ps.setString(2, language.getCode());
        ps.setString(3, language.getFlagUrl());
        ps.setBoolean(4, language.isEnabled());
        ps.executeUpdate();
    }

    @Override
    public void update(PlatformLanguage language) throws SQLException {
        List<String> errors = PlatformLanguageValidator.validate(language);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        String sql = "UPDATE platform_language SET name=?, code=?, flag_url=?, is_enabled=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, language.getName());
        ps.setString(2, language.getCode());
        ps.setString(3, language.getFlagUrl());
        ps.setBoolean(4, language.isEnabled());
        ps.setInt(5, language.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM platform_language WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<PlatformLanguage> getAll() throws SQLException {
        List<PlatformLanguage> list = new ArrayList<>();
        String sql = "SELECT * FROM platform_language";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            PlatformLanguage language = new PlatformLanguage(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getString("flag_url"),
                    rs.getBoolean("is_enabled")
            );
            list.add(language);
        }

        return list;
    }

    @Override
    public PlatformLanguage getById(int id) throws SQLException {
        String sql = "SELECT * FROM platform_language WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new PlatformLanguage(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("code"),
                    rs.getString("flag_url"),
                    rs.getBoolean("is_enabled")
            );
        }
        return null;
    }
    public List<PlatformLanguage> getEnabledLanguages() throws Exception {
        String sql = "SELECT * FROM platform_language WHERE is_enabled = true";
        List<PlatformLanguage> languages = new ArrayList<>();

        PreparedStatement ps = cnx.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            PlatformLanguage language = new PlatformLanguage();
            language.setId(rs.getInt("id"));
            language.setName(rs.getString("name"));
            language.setCode(rs.getString("code"));
            language.setFlagUrl(rs.getString("flag_url"));
            language.setEnabled(rs.getBoolean("is_enabled"));
            languages.add(language);
        }

        return languages;
    }
}