package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pl.pbs.zwbackend.dto.ProjectUserAssignRequest;
import pl.pbs.zwbackend.dto.ProjectUserResponse;
import pl.pbs.zwbackend.dto.ProjectUserRoleUpdateRequest;
import pl.pbs.zwbackend.service.ProjectUserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/users")
@RequiredArgsConstructor
public class ProjectUserController {

    private final ProjectUserService projectUserService;

    @PostMapping
    public ResponseEntity<ProjectUserResponse> assignUserToProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUserAssignRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectUserResponse response = projectUserService.assignUserToProject(projectId, request, currentUser.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProjectUserResponse>> getProjectUsers(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails currentUser) {
        List<ProjectUserResponse> users = projectUserService.getProjectUsers(projectId, currentUser.getUsername());
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> removeUserFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails currentUser) {
        projectUserService.removeUserFromProject(projectId, userId, currentUser.getUsername());
        return ResponseEntity.ok(Map.of("message", "User removed from project successfully"));
    }    @PutMapping("/{userId}/role")
    public ResponseEntity<ProjectUserResponse> updateUserRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @Valid @RequestBody ProjectUserRoleUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        ProjectUserResponse response = projectUserService.updateUserRole(projectId, userId, request.getRole(), currentUser.getUsername());
        return ResponseEntity.ok(response);
    }
}
