package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.ProjectCommentRequest;
import pl.pbs.zwbackend.dto.ProjectCommentResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.ProjectComment;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.ProjectRole;
import pl.pbs.zwbackend.repository.ProjectCommentRepository;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.ProjectUserRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectCommentRepository projectCommentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;
    private final UserService userService;

    @Transactional
    public ProjectCommentResponse addComment(Long projectId, ProjectCommentRequest request, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Check if user has access to the project
        if (!hasProjectAccess(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to comment on this project");
        }

        ProjectComment comment = ProjectComment.builder()
                .project(project)
                .user(currentUser)
                .content(request.getContent())
                .build();

        ProjectComment savedComment = projectCommentRepository.save(comment);
        return convertToResponse(savedComment, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getProjectComments(Long projectId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Check if user has access to the project
        if (!hasProjectAccess(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to view comments for this project");
        }

        return projectCommentRepository.findByProjectIdWithUser(projectId)
                .stream()
                .map(comment -> convertToResponse(comment, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectCommentResponse updateComment(Long projectId, Long commentId, ProjectCommentRequest request, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectComment comment = projectCommentRepository.findByIdAndProjectId(commentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectComment", "id", commentId));

        // Check if user can edit this comment (only comment author)
        if (!canUserEditComment(comment, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to edit this comment");
        }

        comment.setContent(request.getContent());
        ProjectComment updatedComment = projectCommentRepository.save(comment);
        return convertToResponse(updatedComment, currentUser);
    }

    @Transactional
    public void deleteComment(Long projectId, Long commentId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectComment comment = projectCommentRepository.findByIdAndProjectId(commentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectComment", "id", commentId));

        // Check if user can delete this comment (comment author, project owner, or project manager)
        if (!canUserDeleteComment(comment, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to delete this comment");
        }

        projectCommentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public ProjectCommentResponse getCommentById(Long projectId, Long commentId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectComment comment = projectCommentRepository.findByIdAndProjectId(commentId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectComment", "id", commentId));

        // Check if user has access to the project
        if (!hasProjectAccess(comment.getProject(), currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to view this comment");
        }

        return convertToResponse(comment, currentUser);
    }

    private boolean hasProjectAccess(Project project, User user) {
        // Project owner can always access
        if (project.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Check if user is assigned to the project
        return projectUserRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private boolean canUserEditComment(ProjectComment comment, User user) {
        // Only the comment author can edit
        return comment.getUser().getId().equals(user.getId());
    }

    private boolean canUserDeleteComment(ProjectComment comment, User user) {
        // Comment author can delete
        if (comment.getUser().getId().equals(user.getId())) {
            return true;
        }

        // Project owner can delete
        if (comment.getProject().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Project managers can delete
        return projectUserRepository.findByProjectIdAndUserId(comment.getProject().getId(), user.getId())
                .map(pu -> pu.getRole() == ProjectRole.MANAGER)
                .orElse(false);
    }

    private ProjectCommentResponse convertToResponse(ProjectComment comment, User currentUser) {
        UserSummaryResponse userSummary = userService.convertToUserSummaryResponse(comment.getUser());

        return ProjectCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(userSummary)
                .projectId(comment.getProject().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .canEdit(canUserEditComment(comment, currentUser))
                .canDelete(canUserDeleteComment(comment, currentUser))
                .build();
    }
}
