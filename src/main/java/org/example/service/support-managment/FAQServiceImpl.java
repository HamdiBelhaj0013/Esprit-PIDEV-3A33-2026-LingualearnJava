package org.example.service.supportManagment;

import org.example.entity.FAQ;
import org.example.repository.supportmanagement.FAQDAO;  // ✅ CORRIGÉ
import java.util.List;

public class FAQServiceImpl implements IFAQService {

    private FAQDAO dao = new FAQDAO();

    @Override
    public boolean ajouterFAQ(FAQ faq) {
        return dao.ajouter(faq);
    }

    @Override
    public boolean modifierFAQ(FAQ faq) {
        return dao.modifier(faq);
    }

    @Override
    public boolean supprimerFAQ(int id) {
        return dao.supprimer(id);
    }

    @Override
    public List<FAQ> getAll() {
        return dao.getAll();
    }

    @Override
    public List<FAQ> rechercher(String motCle) {
        return dao.rechercher(motCle);
    }

    @Override
    public int countByCategory(String category) {
        return dao.countByCategory(category);
    }
}