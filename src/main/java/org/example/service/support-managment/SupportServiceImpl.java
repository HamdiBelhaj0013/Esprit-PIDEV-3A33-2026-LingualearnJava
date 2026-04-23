package org.example.service.supportManagment;
import org.example.entity.Reclamation;
import org.example.repository.ReclamationDAO;
import java.util.List;

public class SupportServiceImpl implements ISupportService {

    private ReclamationDAO dao = new ReclamationDAO();

    @Override
    public boolean ajouterReclamation(Reclamation r) {
        return dao.ajouter(r);
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