package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.avatar-subdir}")
    private String avatarSubDirectory;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    @Transactional
    public UserSummaryResponse uploadAvatar(MultipartFile file, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type. Only images are allowed.");
        }

        // Delete old avatar if exists
        if (user.getAvatarFileName() != null) {
            try {
                fileStorageService.deleteFile(user.getAvatarFileName(), avatarSubDirectory);
            } catch (Exception e) {
                // Log but don't fail if old file deletion fails
            }
        }

        String storedFileName = fileStorageService.storeFile(file, avatarSubDirectory);
        
        user.setAvatarFileName(storedFileName);
        user.setAvatarContentType(file.getContentType());
        userRepository.save(user);

        return convertToUserSummaryResponse(user);
    }

    public ResponseEntity<Resource> serveAvatar(String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName, avatarSubDirectory);
        User user = userRepository.findByAvatarFileName(fileName)
                .orElseThrow(() -> new ResourceNotFoundException("User", "avatarFileName", fileName));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getAvatarContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @Transactional
    public void deleteAvatar(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (user.getAvatarFileName() != null) {
            fileStorageService.deleteFile(user.getAvatarFileName(), avatarSubDirectory);
            user.setAvatarFileName(null);
            user.setAvatarContentType(null);
            userRepository.save(user);
        }
    }

    public UserSummaryResponse getUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return convertToUserSummaryResponse(user);
    }

    public List<UserSummaryResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserSummaryResponse)
                .collect(Collectors.toList());
    }

    public UserSummaryResponse convertToUserSummaryResponse(User user) {
        String avatarUrl = null;
        if (user.getAvatarFileName() != null) {
            avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/users/avatar/")
                    .path(user.getAvatarFileName())
                    .toUriString();
        }

        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(avatarUrl)
                .build();
    }
}
