package org.example.service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String GMAIL_ADDRESS      = "lingualeran@gmail.com";
    private static final String GMAIL_APP_PASSWORD = "geghlqbzbsvmvpld";
    private static final String SMTP_HOST          = "smtp.gmail.com";
    private static final String SMTP_PORT          = "587";

    public static void sendOTP(String toEmail, String otp) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_ADDRESS, GMAIL_APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(GMAIL_ADDRESS));
            message.setRecipients(Message.RecipientType.TO,
                    jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject("LinguaLearn \u2014 Password Reset Code");
            message.setText(
                "Your password reset code is: " + otp + "\n\n" +
                "This code expires in 10 minutes.\n" +
                "If you did not request this, please ignore this email."
            );
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public static void sendVerificationCode(String toEmail, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(GMAIL_ADDRESS, GMAIL_APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(GMAIL_ADDRESS));
            message.setRecipients(Message.RecipientType.TO,
                jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject("LinguaLearn - Verify your email address");
            message.setText(
                "Welcome to LinguaLearn!\n\n" +
                "Your email verification code is: " + code + "\n\n" +
                "Enter this code in the app to activate your account.\n" +
                "This code expires in 30 minutes.\n\n" +
                "If you did not create an account, ignore this email."
            );
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }
}
