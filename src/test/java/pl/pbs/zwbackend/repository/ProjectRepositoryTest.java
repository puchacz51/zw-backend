package pl.pbs.zwbackend.repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.ProjectStatus;
import pl.pbs.zwbackend.model.enums.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    private User testUser;
    private Project testProject;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .password("password123")
            .role(Role.USER)
            .build();
        
        testProject = Project.builder()
            .name("Test Project")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .status(ProjectStatus.NOT_STARTED)
            .createdBy(testUser)
            .build();
    }
    
    @Test
    void findByCreatedBy_ExistingUser_ReturnsProjects() {
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(testProject);
        
        List<Project> projects = projectRepository.findByCreatedBy(testUser);
        
        assertNotNull(projects);
        assertEquals(1, projects.size());
        assertEquals(testProject.getName(), projects.get(0).getName());
        assertEquals(testProject.getDescription(), projects.get(0).getDescription());
        assertEquals(testProject.getCreatedBy().getId(), projects.get(0).getCreatedBy().getId());
    }
    
    @Test
    void findByCreatedBy_UserWithNoProjects_ReturnsEmptyList() {
        User otherUser = User.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane.smith@example.com")
            .password("password123")
            .role(Role.USER)
            .build();
        entityManager.persistAndFlush(otherUser);
        
        List<Project> projects = projectRepository.findByCreatedBy(otherUser);
        
        assertNotNull(projects);
        assertTrue(projects.isEmpty());
    }
    
    @Test
    void save_NewProject_PersistsProject() {
        entityManager.persistAndFlush(testUser);
        
        Project savedProject = projectRepository.save(testProject);
        
        assertNotNull(savedProject.getId());
        assertEquals(testProject.getName(), savedProject.getName());
        assertEquals(testProject.getDescription(), savedProject.getDescription());
        assertEquals(testProject.getStatus(), savedProject.getStatus());
        assertEquals(testProject.getCreatedBy().getId(), savedProject.getCreatedBy().getId());
    }
    
    @Test
    void findById_ExistingProject_ReturnsProject() {
        entityManager.persistAndFlush(testUser);
        Project savedProject = entityManager.persistAndFlush(testProject);
        
        Optional<Project> found = projectRepository.findById(savedProject.getId());
        
        assertTrue(found.isPresent());
        assertEquals(savedProject.getName(), found.get().getName());
        assertEquals(savedProject.getDescription(), found.get().getDescription());
        assertEquals(savedProject.getStatus(), found.get().getStatus());
    }
    
    @Test
    void findById_NonExistingProject_ReturnsEmpty() {
        Optional<Project> found = projectRepository.findById(999L);
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void deleteById_ExistingProject_DeletesProject() {
        entityManager.persistAndFlush(testUser);
        Project savedProject = entityManager.persistAndFlush(testProject);
        
        projectRepository.deleteById(savedProject.getId());
        entityManager.flush();
        
        Optional<Project> found = projectRepository.findById(savedProject.getId());
        assertFalse(found.isPresent());
    }
}
