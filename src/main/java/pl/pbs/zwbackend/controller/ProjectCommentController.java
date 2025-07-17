package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.ProjectCommentRequest;
import pl.pbs.zwbackend.dto.ProjectCommentResponse;
import pl.pbs.zwbackend.service.ProjectCommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
@RequiredArgsConstructor
public class ProjectCommentController {

    private final ProjectCommentService projectCommentService;

    @PostMapping
    public ResponseEntity<ProjectCommentResponse> addComment(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectCommentResponse response = projectCommentService.addComment(projectId, request, currentUser.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectCommentResponse>> getProjectComments(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails currentUser) {
        List<ProjectCommentResponse> comments = projectCommentService.getProjectComments(projectId, currentUser.getUsername());
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<ProjectCommentResponse> getComment(
            @PathVariable Long projectId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectCommentResponse comment = projectCommentService.getCommentById(projectId, commentId, currentUser.getUsername());
        return ResponseEntity.ok(comment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ProjectCommentResponse> updateComment(
            @PathVariable Long projectId,
            @PathVariable Long commentId,
            @Valid @RequestBody ProjectCommentRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectCommentResponse response = projectCommentService.updateComment(projectId, commentId, request, currentUser.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long projectId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails currentUser) {
        projectCommentService.deleteComment(projectId, commentId, currentUser.getUsername());
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}
