package org.example.services;

import org.example.interfaces.IServices;
import org.example.entities.Commentaire;
import utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire implements IServices<Commentaire> {

    private Connection cnx;

    public ServiceCommentaire() {
        cnx = DBConnection.getInstance().getCnx();
    }

    @Override
    public void add(Commentaire c) throws Exception {
        String sql = "INSERT INTO commentaire (contenu_c, date_com, publication_id) VALUES (?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getContenuC());
        ps.setTimestamp(2, c.getDateCom() != null
                ? Timestamp.valueOf(c.getDateCom())
                : Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(3, c.getPublicationId());
        ps.executeUpdate();
        System.out.println("✅ Commentaire enregistré en DB !");
    }

    @Override
    public void update(Commentaire c) throws Exception {
        String sql = "UPDATE commentaire SET contenu_c=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getContenuC());
        ps.setInt(2, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM commentaire WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Commentaire> getAll() throws Exception {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    @Override
    public Commentaire getById(int id) throws Exception {
        String sql = "SELECT * FROM commentaire WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapResultSet(rs);
        return null;
    }

    // Récupérer les commentaires d'une publication
    public List<Commentaire> getByPublicationId(int publicationId) throws Exception {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE publication_id=? ORDER BY date_com ASC";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, publicationId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    @Override
    public List<Commentaire> search(String keyword) throws Exception {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE contenu_c LIKE ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(mapResultSet(rs));
        }
        return list;
    }

    private Commentaire mapResultSet(ResultSet rs) throws Exception {
        Commentaire c = new Commentaire();
        c.setId(rs.getInt("id"));
        c.setContenuC(rs.getString("contenu_c"));
        Timestamp ts = rs.getTimestamp("date_com");
        if (ts != null) c.setDateCom(ts.toLocalDateTime());
        c.setPublicationId(rs.getInt("publication_id"));
        return c;
    }
}