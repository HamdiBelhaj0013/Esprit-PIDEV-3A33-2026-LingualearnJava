package org.example.service.supportManagment;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String FROM     = "benhamedeya04@gmail.com";
    private static final String PASSWORD = "jqlbaoxhzxiaklat";

    public static void envoyerNotificationReponse(String toEmail, String sujetReclamation, String reponse) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM, PASSWORD);
                    }
                });
                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(FROM, "LinguaLearn Support"));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject("Reponse a votre reclamation : " + sujetReclamation);
                msg.setContent("<h2>Reponse recue</h2><p>" + reponse + "</p>", "text/html; charset=UTF-8");
                Transport.send(msg);
                System.out.println("Email envoye a " + toEmail);
            } catch (Exception e) {
                System.err.println("Erreur email: " + e.getMessage());
            }
        }).start();
    }
}
