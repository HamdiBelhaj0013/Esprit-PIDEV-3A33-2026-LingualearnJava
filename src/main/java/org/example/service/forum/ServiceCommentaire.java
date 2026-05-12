package org.example.service.forum;

import org.example.interfaces.IServices;
import org.example.entities.forum.Commentaire;
import org.example.entity.User;
import org.example.util.MyDataBase;
import org.example.util.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire implements IServices<Commentaire> {

    private Connection cnx;

    public ServiceCommentaire() {
        cnx = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Commentaire c) throws Exception {
        String sql = "INSERT INTO commentaire (contenu_c, date_com, publication_id, utilisateur_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getContenuC());
        ps.setTimestamp(2, c.getDateCom() != null
                ? Timestamp.valueOf(c.getDateCom())
                : Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(3, c.getPublicationId());
        ps.setInt(4, c.getUtilisateurId());
        ps.executeUpdate();
        System.out.println("Commentaire enregistre en DB !");
    }

    @Override
    public void update(Commentaire c) throws Exception {
        // Guard: only the author or an admin may update
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Not authenticated");
        }
        boolean isAdmin = currentUser.hasRole("ROLE_ADMIN");
        boolean isOwner = currentUser.getId() != null
                && c.getUtilisateurId() == currentUser.getId().intValue();
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Access denied: you are not the author of this comment");
        }

        String sql = "UPDATE commentaire SET contenu_c=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getContenuC());
        ps.setInt(2, c.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws Exception {
        // Guard: only the author or an admin may delete
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("Not authenticated");
        }
        boolean isAdmin = currentUser.hasRole("ROLE_ADMIN");
        if (!isAdmin) {
            Commentaire existing = getById(id);
            if (existing == null) throw new Exception("Comment not found: " + id);
            boolean isOwner = currentUser.getId() != null
                    && existing.getUtilisateurId() == currentUser.getId().intValue();
            if (!isOwner) {
                throw new SecurityException("Access denied: you are not the author of this comment");
            }
        }

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

    // Recuperer les commentaires d'une publication
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
        c.setUtilisateurId(rs.getInt("utilisateur_id"));
        return c;
    }
}
