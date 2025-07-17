package pl.pbs.zwbackend.repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .password("password123")
            .role(Role.USER)
            .build();
    }
    
    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        entityManager.persistAndFlush(testUser);
        
        Optional<User> found = userRepository.findByEmail("john.doe@example.com");
        
        assertTrue(found.isPresent());
        assertEquals(testUser.getEmail(), found.get().getEmail());
        assertEquals(testUser.getFirstName(), found.get().getFirstName());
        assertEquals(testUser.getLastName(), found.get().getLastName());
    }
    
    @Test
    void findByEmail_NonExistingUser_ReturnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        assertFalse(found.isPresent());
    }
    
    @Test
    void existsByEmail_ExistingUser_ReturnsTrue() {
        entityManager.persistAndFlush(testUser);
        
        boolean exists = userRepository.existsByEmail("john.doe@example.com");
        
        assertTrue(exists);
    }
    
    @Test
    void existsByEmail_NonExistingUser_ReturnsFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");
        
        assertFalse(exists);
    }
    
    @Test
    void save_NewUser_PersistsUser() {
        User savedUser = userRepository.save(testUser);
        
        assertNotNull(savedUser.getId());
        assertEquals(testUser.getEmail(), savedUser.getEmail());
        assertEquals(testUser.getFirstName(), savedUser.getFirstName());
        assertEquals(testUser.getLastName(), savedUser.getLastName());
        assertEquals(testUser.getRole(), savedUser.getRole());
    }
}
