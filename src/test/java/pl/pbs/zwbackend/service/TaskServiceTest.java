package pl.pbs.zwbackend.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pbs.zwbackend.dto.TaskCreateRequest;
import pl.pbs.zwbackend.dto.TaskResponse;
import pl.pbs.zwbackend.dto.TaskUpdateRequest;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.Task;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.TaskStatus;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.TaskRepository;
import pl.pbs.zwbackend.repository.UserRepository;
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
class TaskServiceTest {
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private TaskService taskService;
    
    private User testUser;
    private Project testProject;
    private Task testTask;
    private TaskCreateRequest taskCreateRequest;
    private TaskUpdateRequest taskUpdateRequest;
    
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
            .createdBy(testUser)
            .build();
            
        testTask = Task.builder()
            .id(1L)
            .name("Test Task")
            .description("Test Task Description")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(7))
            .project(testProject)
            .assignedTo(testUser)
            .build();
            
        taskCreateRequest = TaskCreateRequest.builder()
            .name("Test Task")
            .description("Test Task Description")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(7))
            .projectId(1L)
            .assignedTo(testUser.getEmail())
            .build();
            
        taskUpdateRequest = TaskUpdateRequest.builder()
            .name("Updated Task")
            .description("Updated Task Description")
            .status(TaskStatus.IN_PROGRESS)
            .dueDate(LocalDate.now().plusDays(14))
            .assignedTo(testUser.getEmail())
            .build();
    }
    
    @Test
    void createTask_Success() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(testProject));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        TaskResponse response = taskService.createTask(taskCreateRequest, testUser.getEmail());
        
        assertNotNull(response);
        assertEquals(testTask.getName(), response.getName());
        assertEquals(testTask.getDescription(), response.getDescription());
        assertEquals(testTask.getStatus(), response.getStatus());
        verify(userRepository, times(2)).findByEmail(testUser.getEmail());
        verify(projectRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }
    
    @Test
    void createTask_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> taskService.createTask(taskCreateRequest, "nonexistent@example.com"));
    }
    
    @Test
    void createTask_ProjectNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> taskService.createTask(taskCreateRequest, testUser.getEmail()));
    }
    
    @Test
    void getTaskById_Success() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.of(testTask));
        
        TaskResponse response = taskService.getTaskById(1L);
        
        assertNotNull(response);
        assertEquals(testTask.getName(), response.getName());
        assertEquals(testTask.getDescription(), response.getDescription());
        assertEquals(testTask.getStatus(), response.getStatus());
        verify(taskRepository).findById(1L);
    }
    
    @Test
    void getTaskById_NotFound_ThrowsException() {
        when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> taskService.getTaskById(1L));
    }
    
    @Test
    void getTasksByProject_Success() {
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findByProjectId(anyLong())).thenReturn(tasks);
        
        List<TaskResponse> responses = taskService.getTasksByProject(1L);
        
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testTask.getName(), responses.get(0).getName());
        verify(taskRepository).findByProjectId(1L);
    }
    
    @Test
    void updateTask_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(anyLong())).thenReturn(Optional.of(testTask));
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        
        TaskResponse response = taskService.updateTask(1L, taskUpdateRequest, testUser.getEmail());
        
        assertNotNull(response);
        verify(userRepository, times(2)).findByEmail(testUser.getEmail());
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }
    
    @Test
    void updateTask_UnauthorizedUser_ThrowsException() {
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .build();
            
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(otherUser));
        when(taskRepository.findById(anyLong())).thenReturn(Optional.of(testTask));
        
        assertThrows(UnauthorizedOperationException.class, 
            () -> taskService.updateTask(1L, taskUpdateRequest, otherUser.getEmail()));
    }
    
    @Test
    void deleteTask_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(taskRepository.findById(anyLong())).thenReturn(Optional.of(testTask));
        
        taskService.deleteTask(1L, testUser.getEmail());
        
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(taskRepository).findById(1L);
        verify(taskRepository).deleteById(1L);
    }
    
    @Test
    void deleteTask_UnauthorizedUser_ThrowsException() {
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .build();
            
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(otherUser));
        when(taskRepository.findById(anyLong())).thenReturn(Optional.of(testTask));
        
        assertThrows(UnauthorizedOperationException.class, 
            () -> taskService.deleteTask(1L, otherUser.getEmail()));
    }
    
    @Test
    void getTasksAssignedToUser_Success() {
        List<Task> tasks = Arrays.asList(testTask);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(taskRepository.findByAssignedToId(anyLong())).thenReturn(tasks);
        
        List<TaskResponse> responses = taskService.getTasksAssignedToUser(testUser.getEmail());
        
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testTask.getName(), responses.get(0).getName());
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(taskRepository).findByAssignedToId(testUser.getId());
    }
}
