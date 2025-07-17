package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.pbs.zwbackend.util.IDNMailHelper; // Import the new helper

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrlBase;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Async
    public void sendPasswordResetEmail(String to, String userName, String token) {
        // Detailed logging for 'to' address (can be removed or reduced later)
        logger.info("Raw 'to' address: '{}'", to);
        String processedTo = IDNMailHelper.toASCII(to);
        logger.info("Processed 'to' address using IDNMailHelper: '{}'", processedTo);

        // Detailed logging for mailFrom (can be removed or reduced later)
        logger.info("Raw mailFrom from @Value: '{}'", mailFrom);
        String processedMailFrom = IDNMailHelper.toASCII(mailFrom);
        logger.info("Processed mailFrom using IDNMailHelper: '{}'", processedMailFrom);
        
        if (processedTo == null || processedTo.isEmpty()) {
            logger.error("'To' address is invalid or empty after processing. Cannot send email.");
            return;
        }
        if (processedMailFrom == null || processedMailFrom.isEmpty()) {
            logger.error("'Mail From' address is invalid or empty after processing. Cannot send email.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String resetUrl = resetPasswordUrlBase + "?token=" + token;
            String emailContent = String.format(
                "<p>Hello %s,</p>" +
                "<p>You have requested to reset your password.</p>" +
                "<p>Click the link below to reset your password:</p>" +
                "<p><a href=\"%s\">Reset Password</a></p>" +
                "<p>If you did not request a password reset, please ignore this email.</p>" +
                "<br>" +
                "<p>Thanks,<br>Your Application Team</p>",
                userName, resetUrl
            );

            helper.setText(emailContent, true); // true indicates HTML
            helper.setTo(processedTo);
            helper.setSubject("Password Reset Request");
            helper.setFrom(processedMailFrom);

            logger.info("Attempting to send email. From: '{}', To: '{}', Subject: '{}'", 
                        processedMailFrom, processedTo, "Password Reset Request");

            mailSender.send(mimeMessage);
            logger.info("Password reset email sent successfully to {}", processedTo);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to {}: {}", processedTo, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending password reset email to {}: {}", processedTo, e.getMessage(), e);
        }
    }
}
