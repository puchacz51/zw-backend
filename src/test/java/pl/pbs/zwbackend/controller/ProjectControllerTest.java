package pl.pbs.zwbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService; // Updated import
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pl.pbs.zwbackend.dto.ProjectRequest;
import pl.pbs.zwbackend.dto.ProjectResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.service.ProjectService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private UserDetailsService userDetailsService; // Updated to UserDetailsService

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectRequest projectRequest;
    private ProjectResponse projectResponse;

    @BeforeEach
    void setUp() {
        projectRequest = ProjectRequest.builder()
                .name("Test Project")
                .description("A test project description.")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .build();

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(1L)
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        projectResponse = ProjectResponse.builder()
                .id(1L)
                .name("Test Project")
                .description("A test project description.")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .createdBy(userSummary)
                .createdAt(LocalDate.now()) // Changed LocalDateTime.now() to LocalDate.now()
                .build();
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void createProject_whenAuthenticated_shouldReturnCreated() throws Exception {
        when(projectService.createProject(any(ProjectRequest.class), eq("user@example.com"))).thenReturn(projectResponse);

        mockMvc.perform(post("/api/projects")
                        .with(csrf()) // Add CSRF token if CSRF protection is enabled
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(projectResponse.getName()));
    }

    @Test
    void createProject_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isUnauthorized()); // Or isForbidden() depending on security config
    }
    
    @Test
    @WithMockUser(username = "user@example.com")
    void getMyProjects_whenAuthenticated_shouldReturnOk() throws Exception {
        when(projectService.getProjectsCreatedByUser("user@example.com")).thenReturn(Collections.singletonList(projectResponse));

        mockMvc.perform(get("/api/projects/my-projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(projectResponse.getName()));
    }

    @Test
    void getMyProjects_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/projects/my-projects"))
                .andExpect(status().isUnauthorized()); // Or isForbidden()
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void updateProject_whenAuthenticatedAndOwner_shouldReturnOk() throws Exception {
        when(projectService.updateProject(eq(1L), any(ProjectRequest.class), eq("user@example.com"))).thenReturn(projectResponse);

        mockMvc.perform(put("/api/projects/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(projectResponse.getName()));
    }
    
    @Test
    void updateProject_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/api/projects/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "user@example.com")
    void deleteProject_whenAuthenticatedAndOwner_shouldReturnNoContent() throws Exception {
        doNothing().when(projectService).deleteProject(eq(1L), eq("user@example.com"));

        mockMvc.perform(delete("/api/projects/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProject_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/projects/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // Test for public endpoints (if any, e.g., getProjectById, getAllProjects)
    // These usually don't require @WithMockUser if they are truly public
    @Test
    void getProjectById_shouldReturnOk_whenProjectExists() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(projectResponse);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(projectResponse.getName()));
    }

    @Test
    void getAllProjects_shouldReturnOk() throws Exception {
        when(projectService.getAllProjects()).thenReturn(Collections.singletonList(projectResponse));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(projectResponse.getName()));
    }
}
