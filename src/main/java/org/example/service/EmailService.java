package org.example.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private final String senderEmail = "zayenimaryem.2@gmail.com";
    private final String senderPassword = "mbjd sdjw huuv qebf"; 

    public void sendPaymentConfirmation(String toEmail, String userName, String itemName, int amount) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "LinguaLearn Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("✨ Payment Confirmed - Welcome to Premium Content!");

            String htmlContent = "<html>" +
                    "<body style='margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>" +
                    "  <div style='max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);'>" +
                    "    <div style='background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); padding: 40px 20px; text-align: center; color: #ffffff;'>" +
                    "      <h1 style='margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.025em;'>LinguaLearn</h1>" +
                    "      <p style='margin: 10px 0 0; font-size: 16px; opacity: 0.9;'>Unlock your potential with Premium Access</p>" +
                    "    </div>" +
                    "    <div style='padding: 40px; color: #1e293b; line-height: 1.6;'>" +
                    "      <h2 style='margin-top: 0; color: #0f172a;'>Hello " + userName + ",</h2>" +
                    "      <p style='font-size: 16px;'>Great news! Your payment was successful, and your access to <strong>" + itemName + "</strong> has been activated immediately.</p>" +
                    "      " +
                    "      <div style='background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; margin: 30px 0;'>" +
                    "        <h3 style='margin: 0 0 15px; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em; color: #64748b;'>Order Summary</h3>" +
                    "        <div style='display: flex; justify-content: space-between; margin-bottom: 8px;'>" +
                    "          <span style='color: #64748b;'>Product:</span>" +
                    "          <span style='font-weight: 600; color: #0f172a;'>" + itemName + "</span>" +
                    "        </div>" +
                    "        <div style='display: flex; justify-content: space-between; margin-bottom: 8px;'>" +
                    "          <span style='color: #64748b;'>Amount Paid:</span>" +
                    "          <span style='font-weight: 600; color: #0f172a;'>$" + amount + ".00 USD</span>" +
                    "        </div>" +
                    "        <div style='display: flex; justify-content: space-between;'>" +
                    "          <span style='color: #64748b;'>Status:</span>" +
                    "          <span style='background-color: #dcfce7; color: #166534; padding: 2px 8px; border-radius: 99px; font-size: 12px; font-weight: 700;'>SUCCESSFUL</span>" +
                    "        </div>" +
                    "      </div>" +
                    "      " +
                    "      <p>You can now return to the application and start your quiz. We're excited to see your progress!</p>" +
                    "      " +
                    "      <div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #94a3b8; text-align: center;'>" +
                    "        <p style='margin: 0;'>© 2024 LinguaLearn Inc. All rights reserved.</p>" +
                    "        <p style='margin: 5px 0 0;'>If you have any questions, reply to this email.</p>" +
                    "      </div>" +
                    "    </div>" +
                    "  </div>" +
                    "</body>" +
                    "</html>";

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println(">>> [SUCCESS] Confirmation email sent to " + toEmail);

        } catch (Exception e) {
            System.err.println(">>> [ERROR] EmailService failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
