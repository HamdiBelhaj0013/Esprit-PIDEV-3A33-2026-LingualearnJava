package org.example.service.supportManagment;

import org.example.entity.FAQ;
import java.util.List;

public interface IFAQService {
    boolean ajouterFAQ(FAQ faq);
    boolean modifierFAQ(FAQ faq);
    boolean supprimerFAQ(int id);
    List<FAQ> getAll();
    List<FAQ> rechercher(String motCle);
    int countByCategory(String category);
}