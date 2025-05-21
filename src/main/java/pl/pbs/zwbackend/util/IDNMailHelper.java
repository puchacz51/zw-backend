package pl.pbs.zwbackend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.IDN;
import java.util.regex.Pattern;

public class IDNMailHelper {

    private static final Logger logger = LoggerFactory.getLogger(IDNMailHelper.class);

    // Basic email validation pattern, can be enhanced
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Converts an email address to its ASCII representation, particularly for the domain part (IDN to Punycode).
     * Also trims whitespace and performs basic validation.
     *
     * @param emailAddress The email address to process.
     * @return The ASCII-compatible email address, or null if the input is invalid or cannot be processed.
     */
    public static String toASCII(String emailAddress) {
        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            logger.warn("Input email address is null or empty.");
            return null;
        }

        String trimmedEmail = emailAddress.trim();
        int atIndex = trimmedEmail.lastIndexOf('@');

        if (atIndex == -1 || atIndex == 0 || atIndex == trimmedEmail.length() - 1) {
            logger.warn("Invalid email address format (no '@' or misplaced): {}", trimmedEmail);
            return null; // Or throw an IllegalArgumentException
        }

        String localPart = trimmedEmail.substring(0, atIndex);
        String domainPartWithPotentialComment = trimmedEmail.substring(atIndex + 1);

        // Attempt to remove comments or extra content from domain part
        String domainPart = domainPartWithPotentialComment;
        int spaceIndex = domainPart.indexOf(' ');
        if (spaceIndex != -1) {
            logger.debug("Space found in domain part '{}', trimming to '{}'", domainPart, domainPart.substring(0, spaceIndex));
            domainPart = domainPart.substring(0, spaceIndex);
        }
        // Also consider '#' if it wasn't caught by space
        int hashIndex = domainPart.indexOf('#');
        if (hashIndex != -1) {
            logger.debug("Hash '#' found in domain part '{}', trimming to '{}'", domainPart, domainPart.substring(0, hashIndex));
            domainPart = domainPart.substring(0, hashIndex);
        }

        if (localPart.isEmpty() || domainPart.isEmpty()) {
            logger.warn("Invalid email address format (empty local or domain part after cleaning): {}", trimmedEmail);
            return null;
        }

        try {
            // Convert domain to ASCII (Punycode for IDNs)
            String asciiDomain = IDN.toASCII(domainPart);
            String processedEmail = localPart + "@" + asciiDomain;

            // Optional: Re-validate with a regex after IDN conversion if needed,
            // but IDN.toASCII should produce a valid domain format if the original was plausible.
            // if (!EMAIL_PATTERN.matcher(processedEmail).matches()) {
            //     logger.warn("Email address failed validation after IDN conversion: {}", processedEmail);
            //     return null;
            // }
            logger.debug("Original email: '{}', Cleaned domain: '{}', Processed email (IDN to ASCII): '{}'", trimmedEmail, domainPart, processedEmail);
            return processedEmail;
        } catch (IllegalArgumentException e) {
            // This can happen if the domain part is malformed for IDN processing
            logger.warn("Failed to convert domain to ASCII for email: {}. Original domain part: '{}', Cleaned domain part: '{}'. Reason: {}", 
                        trimmedEmail, domainPartWithPotentialComment, domainPart, e.getMessage());
            return null; // Or return the trimmedEmail if you prefer to try sending it as is
        }
    }
}
