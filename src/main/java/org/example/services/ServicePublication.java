package org.example.services;

import org.example.interfaces.IServices;
import org.example.entities.Publication;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePublication implements IServices<Publication> {

    private Connection cnx;

    public ServicePublication() {
        cnx = DBConnection.getInstance().getCnx();
    }

    private int pageActuelle = 1;
    private final int PUBLICATIONS_PAR_PAGE = 5;
    private List<Publication> toutesPublications = new ArrayList<>();
    @Override
    public void add(Publication p) throws Exception {
        String sql = "INSERT INTO publication (titre_pub, type_pub, lien_pub, contenu_pub, date_pub, likes, dislikes, utilisateur_id) VALUES (?, ?, ?, ?, ?, 0, 0, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getTitrePub());
        ps.setString(2, p.getTypePub());
        ps.setString(3, p.getLienPub());
        ps.setString(4, p.getContenuPub());
        ps.setTimestamp(5, Timestamp.valueOf(p.getDatePub()));
        ps.setInt(6, p.getUtilisateurId());
        ps.executeUpdate();
        System.out.println("✅ Publication ajoutée !");
    }

    @Override
    public void update(Publication p) throws Exception {
        String sql = "UPDATE publication SET titre_pub=?, type_pub=?, lien_pub=?, contenu_pub=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, p.getTitrePub());
        ps.setString(2, p.getTypePub());
        ps.setString(3, p.getLienPub());
        ps.setString(4, p.getContenuPub());
        ps.setInt(5, p.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws Exception {
        // Supprimer d'abord tous les commentaires associés à cette publication
        String deleteCommentsSql = "DELETE FROM commentaire WHERE publication_id=?";
        PreparedStatement psComments = cnx.prepareStatement(deleteCommentsSql);
        psComments.setInt(1, id);
        psComments.executeUpdate();
        
        // Ensuite supprimer la publication
        String sql = "DELETE FROM publication WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        
        System.out.println("✅ Publication et ses commentaires supprimés !");
    }

    @Override
    public List<Publication> getAll() throws Exception {
        List<Publication> list = new ArrayList<>();
        String sql = "SELECT * FROM publication";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Publication p = new Publication();
            p.setId(rs.getInt("id"));
            p.setTitrePub(rs.getString("titre_pub"));
            p.setTypePub(rs.getString("type_pub"));
            p.setLienPub(rs.getString("lien_pub"));
            p.setContenuPub(rs.getString("contenu_pub"));
            p.setDatePub(rs.getTimestamp("date_pub").toLocalDateTime());
            p.setLikes(rs.getInt("likes"));
            p.setDislikes(rs.getInt("dislikes"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            list.add(p);
        }
        return list;
    }

    @Override
    public Publication getById(int id) throws Exception {
        String sql = "SELECT * FROM publication WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Publication p = new Publication();
            p.setId(rs.getInt("id"));
            p.setTitrePub(rs.getString("titre_pub"));
            p.setTypePub(rs.getString("type_pub"));
            p.setLienPub(rs.getString("lien_pub"));
            p.setContenuPub(rs.getString("contenu_pub"));
            p.setDatePub(rs.getTimestamp("date_pub").toLocalDateTime());
            p.setLikes(rs.getInt("likes"));
            p.setDislikes(rs.getInt("dislikes"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            return p;
        }
        return null;
    }

    @Override
    public List<Publication> search(String keyword) throws Exception {
        List<Publication> list = new ArrayList<>();
        String sql = "SELECT * FROM publication WHERE titre_pub LIKE ? OR contenu_pub LIKE ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ps.setString(2, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Publication p = new Publication();
            p.setId(rs.getInt("id"));
            p.setTitrePub(rs.getString("titre_pub"));
            p.setTypePub(rs.getString("type_pub"));
            p.setLienPub(rs.getString("lien_pub"));
            p.setContenuPub(rs.getString("contenu_pub"));
            p.setDatePub(rs.getTimestamp("date_pub").toLocalDateTime());
            p.setLikes(rs.getInt("likes"));
            p.setDislikes(rs.getInt("dislikes"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            list.add(p);
        }
        return list;
    }

    public void like(int id) throws Exception {
        String sql = "UPDATE publication SET likes = likes + 1 WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void dislike(int id) throws Exception {
        String sql = "UPDATE publication SET dislikes = dislikes + 1 WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void unlikePublication(int id) throws Exception {
        String sql = "UPDATE publication SET likes = likes - 1 WHERE id = ? AND likes > 0";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void undislikePublication(int id) throws Exception {
        String sql = "UPDATE publication SET dislikes = dislikes - 1 WHERE id = ? AND dislikes > 0";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Publication> getAllStories() throws Exception {
        List<Publication> list = new ArrayList<>();
        String sql = "SELECT * FROM publication WHERE type_pub = 'story' ORDER BY date_pub DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Publication p = new Publication();
            p.setId(rs.getInt("id"));
            p.setTitrePub(rs.getString("titre_pub"));
            p.setTypePub(rs.getString("type_pub"));
            p.setLienPub(rs.getString("lien_pub"));
            p.setContenuPub(rs.getString("contenu_pub"));
            p.setDatePub(rs.getTimestamp("date_pub").toLocalDateTime());
            p.setLikes(rs.getInt("likes"));
            p.setDislikes(rs.getInt("dislikes"));
            p.setUtilisateurId(rs.getInt("utilisateur_id"));
            list.add(p);
        }
        return list;
    }
}