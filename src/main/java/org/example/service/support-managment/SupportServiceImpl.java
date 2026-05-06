package org.example.service.supportManagment;

import org.example.entity.Reclamation;
import org.example.repository.supportmanagement.ReclamationDAO;
import org.example.service.supportManagment.BadWordsFilter;
import org.example.service.supportManagment.PriorityDetector;
import org.example.util.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class SupportServiceImpl implements ISupportService {

    private ReclamationDAO dao = new ReclamationDAO();

    @Override
    public boolean ajouterReclamation(Reclamation r) {
        // 1. Détecter la priorité automatiquement (pas de choix manuel)
        String priority = PriorityDetector.detect(r.getSubject(), r.getMessageBody());
        r.setPriority(priority);

        // 2. Calculer SLA selon priorité
        LocalDateTime sla;
        switch (priority) {
            case "URGENT" -> sla = LocalDateTime.now().plusHours(2);
            case "HIGH"   -> sla = LocalDateTime.now().plusHours(12);
            default       -> sla = LocalDateTime.now().plusHours(24);
        }
        r.setSlaDeadline(sla);

        // 3. Vérifier les bad words
        String text = r.getSubject() + " " + r.getMessageBody();
        if (BadWordsFilter.containsBadWord(text)) {
            // Bannir le user automatiquement
            banUser(r.getUserId(), BadWordsFilter.getFoundBadWord(text));
            return false; // réclamation refusée
        }

        return dao.ajouter(r);
    }

    private void banUser(int userId, String badWord) {
        String sql = "UPDATE users SET is_banned = 1, banned_at = NOW(), " +
                     "banned_until = DATE_ADD(NOW(), INTERVAL 7 DAY), " +
                     "ban_reason = ? WHERE id = ?";
        try (Connection conn = MyDataBase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "Bad word detected: " + badWord);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ban user failed: " + e.getMessage());
        }
    }

    @Override
    public boolean modifierReclamation(Reclamation r) {
        return dao.modifier(r);
    }

    @Override
    public boolean supprimerReclamation(int id) {
        return dao.supprimer(id);
    }

    @Override
    public boolean supprimerReclamationAdmin(int id) {
        return dao.supprimerAdmin(id);
    }

    @Override
    public boolean changerStatut(int id, String statut) {
        return dao.changerStatut(id, statut);
    }

    @Override
    public List<Reclamation> getAll() {
        return dao.getAll();
    }

    @Override
    public List<Reclamation> getByUserId(int userId) {
        return dao.getByUserId(userId);
    }
}