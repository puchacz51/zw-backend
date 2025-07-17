package pl.pbs.zwbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.pbs.zwbackend.dto.ProjectFileResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.service.ProjectFileService;
import pl.pbs.zwbackend.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProjectFileService projectFileService;

    @PostMapping("/avatar")
    public ResponseEntity<UserSummaryResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails currentUser) {
        UserSummaryResponse response = userService.uploadAvatar(file, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/avatar/{fileName:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        return userService.serveAvatar(fileName);
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, String>> deleteAvatar(@AuthenticationPrincipal UserDetails currentUser) {
        userService.deleteAvatar(currentUser.getUsername());
        return ResponseEntity.ok(Map.of("message", "Avatar deleted successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserSummaryResponse> getProfile(@AuthenticationPrincipal UserDetails currentUser) {
        UserSummaryResponse response = userService.getUserProfile(currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<UserSummaryResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/my-files")
    public ResponseEntity<List<ProjectFileResponse>> getMyFiles(@AuthenticationPrincipal UserDetails currentUser) {
        List<ProjectFileResponse> files = projectFileService.getMyFiles(currentUser.getUsername());
        return ResponseEntity.ok(files);
    }
}
