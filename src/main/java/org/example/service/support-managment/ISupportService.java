package org.example.service.supportManagment;

import org.example.entity.Reclamation;
import java.util.List;

public interface ISupportService {
    boolean ajouterReclamation(Reclamation r);
    boolean modifierReclamation(Reclamation r);
    boolean supprimerReclamation(int id);
    boolean supprimerReclamationAdmin(int id);
    boolean changerStatut(int id, String statut);
    List<Reclamation> getAll();
    List<Reclamation> getByUserId(int userId);
}