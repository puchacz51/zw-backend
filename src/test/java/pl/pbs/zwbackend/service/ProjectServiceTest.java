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
import pl.pbs.zwbackend.model.enums.ProjectStatus;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;
import pl.pbs.zwbackend.repository.ProjectUserRepository;
import pl.pbs.zwbackend.repository.ProjectCommentRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ProjectUserRepository projectUserRepository;
    
    @Mock
    private ProjectCommentRepository projectCommentRepository;
    
    @InjectMocks
    private ProjectService projectService;
    
    private User testUser;
    private Project testProject;
    private ProjectRequest projectRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .role(Role.USER)
            .build();
            
        testProject = Project.builder()
            .id(1L)
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .createdBy(testUser)
            .build();
            
        projectRequest = ProjectRequest.builder()
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .build();
    }
    
    @Test
    void createProject_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        
        ProjectResponse response = projectService.createProject(projectRequest, testUser.getEmail());
        
        assertNotNull(response);
        assertEquals(testProject.getName(), response.getName());
        assertEquals(testProject.getDescription(), response.getDescription());
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(projectRepository).save(any(Project.class));
    }
    
    @Test
    void createProject_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> projectService.createProject(projectRequest, "nonexistent@example.com"));
    }
    
    @Test
    void getProjectById_Success() {
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        
        ProjectResponse response = projectService.getProjectById(1L);
        
        assertNotNull(response);
        assertEquals(testProject.getName(), response.getName());
        assertEquals(testProject.getDescription(), response.getDescription());
        verify(projectRepository).findById(1L);
    }
    
    @Test
    void getProjectById_NotFound_ThrowsException() {
        when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> projectService.getProjectById(1L));
    }
    
    @Test
    void getAllProjects_Success() {
        List<Project> projects = Arrays.asList(testProject);
        when(projectRepository.findAll()).thenReturn(projects);
        
        List<ProjectResponse> responses = projectService.getAllProjects();
        
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testProject.getName(), responses.get(0).getName());
        verify(projectRepository).findAll();
    }
    
    @Test
    void updateProject_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        
        ProjectRequest updateRequest = ProjectRequest.builder()
            .name("Updated Project")
            .description("Updated Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(60))
            .status(ProjectStatus.IN_PROGRESS)
            .build();
        
        ProjectResponse response = projectService.updateProject(1L, updateRequest, testUser.getEmail());
        
        assertNotNull(response);
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(projectRepository).findById(1L);
        verify(projectRepository).save(any(Project.class));
    }
    
    @Test
    void updateProject_UnauthorizedUser_ThrowsException() {
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .build();
            
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(otherUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        
        assertThrows(UnauthorizedOperationException.class, 
            () -> projectService.updateProject(1L, projectRequest, otherUser.getEmail()));
    }
    
    @Test
    void deleteProject_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        
        projectService.deleteProject(1L, testUser.getEmail());
        
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(projectRepository).findById(1L);
        verify(projectCommentRepository).deleteByProjectId(1L);
        verify(projectRepository).deleteById(1L);
    }
    
    @Test
    void deleteProject_UnauthorizedUser_ThrowsException() {
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .build();
            
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(otherUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        
        assertThrows(UnauthorizedOperationException.class, 
            () -> projectService.deleteProject(1L, otherUser.getEmail()));
    }
    
    @Test
    void getProjectsCreatedByUser_Success() {
        List<Project> projects = Arrays.asList(testProject);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(projectRepository.findByCreatedBy(any(User.class))).thenReturn(projects);
        
        List<ProjectResponse> responses = projectService.getProjectsCreatedByUser(testUser.getEmail());
        
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testProject.getName(), responses.get(0).getName());
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(projectRepository).findByCreatedBy(testUser);
    }
}
