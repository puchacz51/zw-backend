package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.ProjectUserAssignRequest;
import pl.pbs.zwbackend.dto.ProjectUserResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.ProjectUser;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.ProjectRole;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.ProjectUserRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectUserService {

    private final ProjectUserRepository projectUserRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public ProjectUserResponse assignUserToProject(Long projectId, ProjectUserAssignRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        
        // Check if current user is owner or manager of the project
        if (!isUserAuthorizedToManageProject(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to assign users to this project");
        }
        
        User userToAssign = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getUserEmail()));
        
        // Check if user is already assigned to the project
        if (projectUserRepository.existsByProjectIdAndUserId(projectId, userToAssign.getId())) {
            throw new IllegalStateException("User is already assigned to this project");
        }
        
        ProjectUser projectUser = ProjectUser.builder()
                .project(project)
                .user(userToAssign)
                .role(request.getRole())
                .build();
        
        ProjectUser savedProjectUser = projectUserRepository.save(projectUser);
        return convertToResponse(savedProjectUser);
    }

    @Transactional(readOnly = true)
    public List<ProjectUserResponse> getProjectUsers(Long projectId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        
        // Check if current user has access to view project members
        if (!isUserAuthorizedToViewProject(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to view project members");
        }
        
        return projectUserRepository.findByProjectIdWithUsers(projectId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeUserFromProject(Long projectId, Long userId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        
        // Check if current user is authorized to remove users from project
        if (!isUserAuthorizedToManageProject(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to remove users from this project");
        }
        
        // Check if the assignment exists
        if (!projectUserRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ResourceNotFoundException("ProjectUser", "projectId and userId", projectId + " and " + userId);
        }
        
        projectUserRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Transactional
    public ProjectUserResponse updateUserRole(Long projectId, Long userId, ProjectRole newRole, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        
        // Check if current user is authorized to modify user roles
        if (!isUserAuthorizedToManageProject(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to modify user roles in this project");
        }
        
        ProjectUser projectUser = projectUserRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectUser", "projectId and userId", projectId + " and " + userId));
        
        projectUser.setRole(newRole);
        ProjectUser savedProjectUser = projectUserRepository.save(projectUser);
        return convertToResponse(savedProjectUser);
    }

    private boolean isUserAuthorizedToManageProject(Project project, User user) {
        // Project owner can always manage
        if (project.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        
        // Check if user is a manager of the project
        return projectUserRepository.findByProjectIdAndUserId(project.getId(), user.getId())
                .map(pu -> pu.getRole() == ProjectRole.MANAGER)
                .orElse(false);
    }

    private boolean isUserAuthorizedToViewProject(Project project, User user) {
        // Project owner can always view
        if (project.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        
        // Check if user is assigned to the project
        return projectUserRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private ProjectUserResponse convertToResponse(ProjectUser projectUser) {
        return ProjectUserResponse.builder()
                .user(userService.convertToUserSummaryResponse(projectUser.getUser()))
                .role(projectUser.getRole())
                .build();
    }
}
