package org.example.service.supportManagment;

import org.example.util.MailSender;

public class EmailService {

    public static void envoyerNotificationReponse(String toEmail, String sujetReclamation, String reponse) {
        new Thread(() -> {
            try {
                String subject = "Reponse a votre reclamation : " + sujetReclamation;
                String htmlBody = "<h2>Reponse recue</h2><p>" + reponse + "</p>";
                MailSender.send(toEmail, subject, htmlBody);
                System.out.println("Email envoye a " + toEmail);
            } catch (Exception e) {
                System.err.println("Erreur email: " + e.getMessage());
            }
        }).start();
    }
}
