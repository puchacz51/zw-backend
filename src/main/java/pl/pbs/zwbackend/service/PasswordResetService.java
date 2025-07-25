package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.model.PasswordResetToken;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.PasswordResetTokenRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            logger.warn("Password reset requested for non-existent email: {}", email);
            return;
        }
        User user = userOptional.get();

        tokenRepository.deleteByUser(user);
        tokenRepository.flush(); // Explicitly flush changes to the database

        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(token, user);
        tokenRepository.save(myToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);

        logger.info("Password reset token generated for user (email {}). Email has been sent.", user.getEmail());
    }

    public Optional<User> validatePasswordResetToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(resetToken -> !resetToken.isExpired())
                .map(PasswordResetToken::getUser);
    }

    @Transactional
    public void resetPassword(User user, String newPassword) {
        user.setPassword(newPassword);
        userRepository.save(user);
        tokenRepository.deleteByUser(user);
        logger.info("Password has been reset for user: {}", user.getEmail());
    }
}
