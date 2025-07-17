package pl.pbs.zwbackend.integration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.ProjectRequest;
import pl.pbs.zwbackend.dto.RegisterRequest;
import pl.pbs.zwbackend.dto.TaskCreateRequest;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.ProjectStatus;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.model.enums.TaskStatus;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;
import pl.pbs.zwbackend.service.UserService;
import java.time.LocalDate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ProjectIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserService userService;
    
    private User testUser;
    private Project testProject;
    
    @BeforeEach
    void setUp() {
        // Create test user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("password123");
        
        testUser = userService.createUser(registerRequest);
        
        // Create test project
        testProject = Project.builder()
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .createdBy(testUser)
            .build();
        
        testProject = projectRepository.save(testProject);
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void createProject_FullFlow_Success() throws Exception {
        ProjectRequest projectRequest = ProjectRequest.builder()
            .name("Integration Test Project")
            .description("Integration Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(60))
            .status(ProjectStatus.NOT_STARTED)
            .build();
        
        mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Integration Test Project"))
            .andExpect(jsonPath("$.description").value("Integration Test Description"))
            .andExpect(jsonPath("$.status").value("NOT_STARTED"))
            .andExpect(jsonPath("$.createdBy.email").value("john.doe@example.com"));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void getProject_FullFlow_Success() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", testProject.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testProject.getId()))
            .andExpect(jsonPath("$.name").value(testProject.getName()))
            .andExpect(jsonPath("$.description").value(testProject.getDescription()))
            .andExpect(jsonPath("$.status").value(testProject.getStatus().toString()))
            .andExpect(jsonPath("$.createdBy.email").value(testUser.getEmail()));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void updateProject_FullFlow_Success() throws Exception {
        ProjectRequest updateRequest = ProjectRequest.builder()
            .name("Updated Project Name")
            .description("Updated Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(90))
            .status(ProjectStatus.IN_PROGRESS)
            .build();
        
        mockMvc.perform(put("/api/projects/{id}", testProject.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testProject.getId()))
            .andExpect(jsonPath("$.name").value("Updated Project Name"))
            .andExpect(jsonPath("$.description").value("Updated Description"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void deleteProject_FullFlow_Success() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", testProject.getId())
                .with(csrf()))
            .andExpect(status().isNoContent());
        
        // Verify project is deleted
        mockMvc.perform(get("/api/projects/{id}", testProject.getId()))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void createTaskInProject_FullFlow_Success() throws Exception {
        TaskCreateRequest taskRequest = TaskCreateRequest.builder()
            .name("Integration Test Task")
            .description("Integration Test Task Description")
            .status(TaskStatus.TODO)
            .dueDate(LocalDate.now().plusDays(14))
            .projectId(testProject.getId())
            .assignedTo(testUser.getEmail())
            .build();
        
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Integration Test Task"))
            .andExpect(jsonPath("$.description").value("Integration Test Task Description"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.projectId").value(testProject.getId()))
            .andExpect(jsonPath("$.assignedTo.email").value(testUser.getEmail()));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void getMyProjects_FullFlow_Success() throws Exception {
        mockMvc.perform(get("/api/projects/my-projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(testProject.getId()))
            .andExpect(jsonPath("$[0].name").value(testProject.getName()))
            .andExpect(jsonPath("$[0].createdBy.email").value(testUser.getEmail()));
    }
    
    @Test
    @WithMockUser(username = "other.user@example.com")
    void updateProjectAsUnauthorizedUser_Returns403() throws Exception {
        ProjectRequest updateRequest = ProjectRequest.builder()
            .name("Unauthorized Update")
            .description("Should not work")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(90))
            .status(ProjectStatus.IN_PROGRESS)
            .build();
        
        mockMvc.perform(put("/api/projects/{id}", testProject.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isForbidden());
    }
    
    @Test
    void accessProtectedEndpointWithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized());
    }
}
