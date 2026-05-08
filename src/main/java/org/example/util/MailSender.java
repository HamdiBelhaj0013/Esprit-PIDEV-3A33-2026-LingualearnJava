package org.example.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Shared SMTP utility — the single place in the application that touches
 * javax/jakarta.mail Transport.  All email service classes delegate here.
 *
 * Configuration is read from AppConfig (environment variables):
 *   MAIL_USERNAME, MAIL_PASSWORD, MAIL_SMTP_HOST, MAIL_SMTP_PORT
 */
public class MailSender {

    private static final Session SESSION;

    static {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            AppConfig.getMailSmtpHost());
        props.put("mail.smtp.port",            AppConfig.getMailSmtpPort());
        props.put("mail.smtp.ssl.trust",       AppConfig.getMailSmtpHost());

        SESSION = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        AppConfig.getMailUsername(),
                        AppConfig.getMailPassword());
            }
        });
    }

    /**
     * Sends an email.
     *
     * @param toEmail  recipient address
     * @param subject  email subject line
     * @param htmlBody email body — may be plain text or HTML
     * @throws RuntimeException wrapping MessagingException if sending fails
     */
    public static void send(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = new MimeMessage(SESSION);
            message.setFrom(new InternetAddress(AppConfig.getMailUsername()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("[MailSender] Email sent to " + toEmail + " — subject: " + subject);
        } catch (MessagingException e) {
            System.err.println("[MailSender] ERROR: Failed to send to " + toEmail
                    + " — subject: " + subject + " — " + e.getMessage());
            throw new RuntimeException("Failed to send email to " + toEmail, e);
        }
    }
}
