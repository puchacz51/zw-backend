package pl.pbs.zwbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pbs.zwbackend.dto.ProjectRequest;
import pl.pbs.zwbackend.dto.ProjectResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User user;
    private Project project;
    private ProjectRequest projectRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        projectRequest = ProjectRequest.builder()
                .name("Test Project")
                .description("Test Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build();

        project = Project.builder()
                .id(1L)
                .name(projectRequest.getName())
                .description(projectRequest.getDescription())
                .startDate(projectRequest.getStartDate())
                .endDate(projectRequest.getEndDate())
                .createdBy(user)
                .createdAt(LocalDate.now()) // Changed LocalDateTime.now() to LocalDate.now()
                .build();
    }

    @Test
    void createProject_shouldCreateProjectSuccessfully() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectResponse response = projectService.createProject(projectRequest, user.getEmail());

        assertNotNull(response);
        assertEquals(project.getName(), response.getName());
        assertEquals(user.getId(), response.getCreatedBy().getId());
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void createProject_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.createProject(projectRequest, "unknown@example.com");
        });
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void getProjectById_shouldReturnProject_whenProjectExists() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProjectById(project.getId());

        assertNotNull(response);
        assertEquals(project.getId(), response.getId());
    }

    @Test
    void getProjectById_shouldThrowResourceNotFoundException_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.getProjectById(99L);
        });
    }

    @Test
    void getAllProjects_shouldReturnAllProjects() {
        when(projectRepository.findAll()).thenReturn(Collections.singletonList(project));

        List<ProjectResponse> responses = projectService.getAllProjects();

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(project.getName(), responses.get(0).getName());
    }
    
    @Test
    void getProjectsCreatedByUser_shouldReturnProjects_whenUserExists() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findByCreatedBy(user)).thenReturn(Collections.singletonList(project));

        List<ProjectResponse> responses = projectService.getProjectsCreatedByUser(user.getEmail());

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(project.getName(), responses.get(0).getName());
        assertEquals(user.getId(), responses.get(0).getCreatedBy().getId());
    }

    @Test
    void getProjectsCreatedByUser_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.getProjectsCreatedByUser("unknown@example.com");
        });
        verify(projectRepository, never()).findByCreatedBy(any(User.class));
    }


    @Test
    void updateProject_shouldUpdateProjectSuccessfully_whenUserIsCreator() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenReturn(project); // Assume save returns the updated project

        ProjectRequest updateRequest = ProjectRequest.builder().name("Updated Name").description("Updated Desc").build();
        ProjectResponse response = projectService.updateProject(project.getId(), updateRequest, user.getEmail());

        assertNotNull(response);
        assertEquals("Updated Name", response.getName());
        verify(projectRepository, times(1)).save(project);
    }

    @Test
    void updateProject_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.updateProject(project.getId(), projectRequest, "unknown@example.com");
        });
    }

    @Test
    void updateProject_shouldThrowResourceNotFoundException_whenProjectNotFound() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.updateProject(99L, projectRequest, user.getEmail());
        });
    }

    @Test
    void updateProject_shouldThrowUnauthorizedOperationException_whenUserIsNotCreator() {
        User anotherUser = User.builder().id(2L).email("another@example.com").build();
        when(userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.of(anotherUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project)); // project created by 'user' (id 1L)

        assertThrows(UnauthorizedOperationException.class, () -> {
            projectService.updateProject(project.getId(), projectRequest, anotherUser.getEmail());
        });
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void deleteProject_shouldDeleteProjectSuccessfully_whenUserIsCreator() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        doNothing().when(projectRepository).delete(project);

        assertDoesNotThrow(() -> {
            projectService.deleteProject(project.getId(), user.getEmail());
        });
        verify(projectRepository, times(1)).delete(project);
    }

    @Test
    void deleteProject_shouldThrowResourceNotFoundException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.deleteProject(project.getId(), "unknown@example.com");
        });
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void deleteProject_shouldThrowResourceNotFoundException_whenProjectNotFound() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            projectService.deleteProject(99L, user.getEmail());
        });
        verify(projectRepository, never()).delete(any(Project.class));
    }

    @Test
    void deleteProject_shouldThrowUnauthorizedOperationException_whenUserIsNotCreator() {
        User anotherUser = User.builder().id(2L).email("another@example.com").build();
        when(userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.of(anotherUser));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project)); // project created by 'user' (id 1L)

        assertThrows(UnauthorizedOperationException.class, () -> {
            projectService.deleteProject(project.getId(), anotherUser.getEmail());
        });
        verify(projectRepository, never()).delete(any(Project.class));
    }
}
