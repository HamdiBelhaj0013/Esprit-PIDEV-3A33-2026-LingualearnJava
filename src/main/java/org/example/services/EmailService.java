package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.util.AppConfig;

import java.util.Properties;

/**
 * Service d'envoi d'emails via Gmail SMTP.
 * Configurez email.from, email.password et email.admin dans config.properties
 */
public class EmailService {

    public static void sendBadWordWarning(String commentContent, String publicationTitle) {
        new Thread(() -> {
            try {
                String fromEmail = AppConfig.get("email.from").trim();
                String fromPassword = AppConfig.get("email.password").trim();
                String adminEmail = AppConfig.get("email.admin").trim();

                if (fromEmail.isEmpty() || fromEmail.startsWith("votre.email")) {
                    System.out.println("⚠️ Email non configuré dans config.properties — email non envoyé.");
                    return;
                }

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                props.put("mail.debug", "false");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(fromEmail, fromPassword);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail, "LinguaLearn Modération"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmail));
                message.setSubject("⚠️ Commentaire inapproprié détecté - LinguaLearn");

                String htmlBody = """
                    <div style="font-family: Arial, sans-serif; background:#f4f6f9; padding:30px;">
                        <div style="max-width:600px; margin:auto; background:#fff; border-radius:10px; padding:30px;
                                    box-shadow: 0 5px 15px rgba(0,0,0,0.08);">
                            <h2 style="color:#e74c3c;">⚠️ Commentaire inapproprié détecté</h2>
                            <p style="font-size:15px; color:#333;">Bonjour Administrateur,</p>
                            <p style="font-size:15px; color:#555; line-height:1.6;">
                                Un commentaire contenant des mots inappropriés a été bloqué automatiquement.
                            </p>
                            <div style="margin:20px 0; padding:15px; background:#fff3cd; border-left:5px solid #ffc107; border-radius:5px;">
                                <strong>Publication :</strong> %s<br/><br/>
                                <strong>Commentaire bloqué :</strong><br/>
                                <span style="color:#e74c3c; font-style:italic;">"%s"</span>
                            </div>
                            <p style="font-size:14px; color:#777;">
                                Ce commentaire n'a pas été publié.<br/>
                                Veuillez prendre les mesures nécessaires si besoin.
                            </p>
                            <hr style="border:none; border-top:1px solid #eee; margin:20px 0;"/>
                            <p style="font-size:13px; color:#aaa;">
                                Système de modération automatique — LinguaLearn
                            </p>
                        </div>
                    </div>
                    """.formatted(publicationTitle, commentContent);

                message.setContent(htmlBody, "text/html; charset=utf-8");
                Transport.send(message);
                System.out.println("✅ Email d'avertissement envoyé à " + adminEmail);

            } catch (Exception e) {
                System.out.println("❌ Erreur envoi email : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
