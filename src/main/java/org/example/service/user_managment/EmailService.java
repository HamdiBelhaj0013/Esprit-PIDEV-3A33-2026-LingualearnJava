package org.example.service;

import org.example.util.MailSender;

public class EmailService {

    public static void sendOTP(String toEmail, String otp) {
        String subject = "LinguaLearn \u2014 Password Reset Code";
        String body =
                "Your password reset code is: " + otp + "\n\n" +
                "This code expires in 10 minutes.\n" +
                "If you did not request this, please ignore this email.";
        MailSender.send(toEmail, subject, body);
    }

    public static void sendVerificationCode(String toEmail, String code) {
        String subject = "LinguaLearn - Verify your email address";
        String body =
                "Welcome to LinguaLearn!\n\n" +
                "Your email verification code is: " + code + "\n\n" +
                "Enter this code in the app to activate your account.\n" +
                "This code expires in 30 minutes.\n\n" +
                "If you did not create an account, ignore this email.";
        MailSender.send(toEmail, subject, body);
    }
}
