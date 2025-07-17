package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.*;
import pl.pbs.zwbackend.model.RefreshToken;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.UserRepository;
import pl.pbs.zwbackend.security.JwtTokenProvider;
import pl.pbs.zwbackend.service.PasswordResetService;
import pl.pbs.zwbackend.service.RefreshTokenService;
import pl.pbs.zwbackend.service.UserService;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getLogin(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database, email: " + userDetails.getUsername()));

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        String avatarUrl = null;
        if (user.getAvatarFileName() != null) {
            avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/users/avatar/")
                    .path(user.getAvatarFileName())
                    .toUriString();
        }

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(avatarUrl)
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .role(Role.USER)
                .build();

        user.setPassword(registerRequest.getPassword());

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        Optional<RefreshToken> optRefreshToken = refreshTokenService.findByToken(requestRefreshToken);

        if (optRefreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Refresh token is not in database.");
        }

        RefreshToken refreshTokenEntity = optRefreshToken.get();
        try {
            refreshTokenService.verifyExpiration(refreshTokenEntity);
        } catch (RuntimeException ex) {
            if ("Refresh token was expired. Please make a new signin request".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
            }
            throw ex;
        }

        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        String avatarUrl = null;
        if (user.getAvatarFileName() != null) {
            avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/users/avatar/")
                    .path(user.getAvatarFileName())
                    .toUriString();
        }

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(requestRefreshToken)
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(avatarUrl)
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated properly.");
        }
        UserDetails userDetails = (UserDetails) principal;
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found for logout, email: " + userDetails.getUsername()));
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.ok("Log out successful!");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        passwordResetService.createPasswordResetTokenForUser(forgotPasswordRequest.getEmail());
        return ResponseEntity.ok("If an account with that email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        Optional<User> userOptional = passwordResetService.validatePasswordResetToken(resetPasswordRequest.getToken());
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired password reset token.");
        }

        User user = userOptional.get();
        passwordResetService.resetPassword(user, resetPasswordRequest.getNewPassword());
        return ResponseEntity.ok("Password has been successfully reset.");
    }
}