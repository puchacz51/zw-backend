package pl.pbs.zwbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.pbs.zwbackend.dto.ProjectFileResponse;
import pl.pbs.zwbackend.service.ProjectFileService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/files")
@RequiredArgsConstructor
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    @PostMapping("/upload")
    public ResponseEntity<ProjectFileResponse> uploadFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectFileResponse response = projectFileService.uploadFile(file, projectId, currentUser.getUsername(), description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectFileResponse>> getProjectFiles(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails currentUser) {
        List<ProjectFileResponse> files = projectFileService.getProjectFiles(projectId, currentUser.getUsername());
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<ProjectFileResponse> getFileDetails(
            @PathVariable Long projectId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectFileResponse response = projectFileService.getFileDetails(fileId, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long projectId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return projectFileService.downloadFile(fileId, currentUser.getUsername());
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long projectId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserDetails currentUser) {
        projectFileService.deleteFile(fileId, currentUser.getUsername());
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }
}
