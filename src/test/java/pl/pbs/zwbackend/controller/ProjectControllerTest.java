package pl.pbs.zwbackend.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.pbs.zwbackend.dto.ProjectRequest;
import pl.pbs.zwbackend.dto.ProjectResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.model.enums.ProjectStatus;
import pl.pbs.zwbackend.service.ProjectService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProjectService projectService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ProjectRequest projectRequest;
    private ProjectResponse projectResponse;
    
    @BeforeEach
    void setUp() {
        projectRequest = ProjectRequest.builder()
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .build();
            
        UserSummaryResponse createdBy = UserSummaryResponse.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .build();
            
        projectResponse = ProjectResponse.builder()
            .id(1L)
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .createdBy(createdBy)
            .build();
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void createProject_Success() throws Exception {
        when(projectService.createProject(any(ProjectRequest.class), anyString()))
            .thenReturn(projectResponse);
        
        mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Project"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.status").value("NOT_STARTED"));
            
        verify(projectService).createProject(any(ProjectRequest.class), eq("john.doe@example.com"));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void createProject_InvalidData_BadRequest() throws Exception {
        ProjectRequest invalidRequest = ProjectRequest.builder()
            .name("") // Empty name should fail validation
            .description("Test Description")
            .build();
        
        mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
            
        verify(projectService, never()).createProject(any(ProjectRequest.class), anyString());
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void getProjectById_Success() throws Exception {
        when(projectService.getProjectById(anyLong())).thenReturn(projectResponse);
        
        mockMvc.perform(get("/api/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Project"))
            .andExpect(jsonPath("$.description").value("Test Description"));
            
        verify(projectService).getProjectById(1L);
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void getAllProjects_Success() throws Exception {
        List<ProjectResponse> projects = Arrays.asList(projectResponse);
        when(projectService.getAllProjects()).thenReturn(projects);
        
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Test Project"));
            
        verify(projectService).getAllProjects();
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void getMyProjects_Success() throws Exception {
        List<ProjectResponse> projects = Arrays.asList(projectResponse);
        when(projectService.getProjectsCreatedByUser(anyString())).thenReturn(projects);
        
        mockMvc.perform(get("/api/projects/my-projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Test Project"));
            
        verify(projectService).getProjectsCreatedByUser("john.doe@example.com");
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void updateProject_Success() throws Exception {
        when(projectService.updateProject(anyLong(), any(ProjectRequest.class), anyString()))
            .thenReturn(projectResponse);
        
        mockMvc.perform(put("/api/projects/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Project"));
            
        verify(projectService).updateProject(eq(1L), any(ProjectRequest.class), eq("john.doe@example.com"));
    }
    
    @Test
    @WithMockUser(username = "john.doe@example.com")
    void deleteProject_Success() throws Exception {
        doNothing().when(projectService).deleteProject(anyLong(), anyString());
        
        mockMvc.perform(delete("/api/projects/1")
                .with(csrf()))
            .andExpect(status().isNoContent());
            
        verify(projectService).deleteProject(1L, "john.doe@example.com");
    }
    
    @Test
    void createProject_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projectRequest)))
            .andExpect(status().isUnauthorized());
            
        verify(projectService, never()).createProject(any(ProjectRequest.class), anyString());
    }
}
