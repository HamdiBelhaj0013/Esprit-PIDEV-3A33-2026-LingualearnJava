package org.example.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Service d'envoi d'emails via Gmail SMTP.
 * Utilise la configuration validée de l'équipe (benali.mohamedzh@gmail.com).
 */
public class EmailService {

    private static final String SMTP_HOST  = "smtp.gmail.com";
    private static final String SMTP_PORT  = "587";
    private static final String FROM_EMAIL = "benali.mohamedzh@gmail.com";
    private static final String FROM_PASS  = "rpgebnxdjqonnkcq";
    private static final String ADMIN_EMAIL = "asma.rhayem@esprit.tn";

    public static void sendBadWordWarning(String commentContent, String publicationTitle) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.trust", SMTP_HOST);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM_EMAIL, FROM_PASS);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "LinguaLearn Modération"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(ADMIN_EMAIL));
                message.setSubject("⚠️ Commentaire inapproprié détecté - LinguaLearn");

                String htmlBody = """
                    <!DOCTYPE html>
                    <html><body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 0;">
                    <table width="560" style="background:white;border-radius:16px;overflow:hidden;
                           box-shadow:0 4px 24px rgba(0,0,0,0.10);">
                      <tr><td style="background:linear-gradient(135deg,#e74c3c,#c0392b);
                                     padding:32px 40px;text-align:center;">
                        <h1 style="color:white;margin:0;font-size:26px;">🛡️ LinguaLearn</h1>
                        <p style="color:#ffc9c9;margin:6px 0 0;font-size:15px;">Système de Modération</p>
                      </td></tr>
                      <tr><td style="padding:28px 40px 0;text-align:center;">
                        <h2 style="color:#e74c3c;font-size:20px;margin:0;">⚠️ Commentaire inapproprié bloqué</h2>
                        <p style="color:#64748b;font-size:13px;margin:8px 0 0;">Un commentaire a été automatiquement bloqué par le système.</p>
                      </td></tr>
                      <tr><td style="padding:20px 40px 24px;">
                        <div style="background:#fff8f8;border-radius:12px;padding:20px;
                                    border:2px solid #fde8e8;">
                          <p style="color:#475569;font-size:13px;margin:0 0 10px;">
                            <strong>📌 Publication :</strong> %s
                          </p>
                          <p style="color:#475569;font-size:13px;margin:0;">
                            <strong>💬 Commentaire bloqué :</strong>
                          </p>
                          <p style="color:#e74c3c;font-size:14px;font-style:italic;
                                    background:#fde8e8;padding:10px 14px;border-radius:8px;
                                    margin:8px 0 0;">"%s"</p>
                        </div>
                      </td></tr>
                      <tr><td style="padding:0 40px 28px;text-align:center;">
                        <p style="color:#475569;font-size:14px;line-height:1.7;margin:0;">
                          Ce commentaire <strong>n'a pas été publié</strong>.<br>
                          Veuillez prendre les mesures nécessaires si besoin.
                        </p>
                      </td></tr>
                      <tr><td style="background:#f8fafc;padding:16px 40px;text-align:center;
                                     border-top:1px solid #e2e8f0;">
                        <p style="color:#94a3b8;font-size:12px;margin:0;">
                          © 2026 LinguaLearn — Système de modération automatique
                        </p>
                      </td></tr>
                    </table>
                    </td></tr></table>
                    </body></html>
                    """.formatted(publicationTitle, commentContent);

                message.setContent(htmlBody, "text/html; charset=UTF-8");
                Transport.send(message);
                System.out.println("✅ Email d'avertissement envoyé à " + ADMIN_EMAIL);

            } catch (Exception e) {
                System.out.println("❌ Erreur envoi email : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
