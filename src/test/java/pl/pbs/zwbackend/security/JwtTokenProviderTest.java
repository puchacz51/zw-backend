package pl.pbs.zwbackend.security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {
    @Mock
    private UserDetails userDetails;
    
    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;
    
    private String jwtSecret = "testsecretkey12345678901234567890123456789012345678901234567890";
    private int jwtAccessExpirationMs = 3600000; // 1 hour
    private int jwtRefreshExpirationMs = 86400000; // 24 hours
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtAccessExpirationMs", jwtAccessExpirationMs);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtRefreshExpirationMs", jwtRefreshExpirationMs);
        
        testUser = User.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .role(Role.USER)
            .build();
    }
    
    @Test
    void generateAccessToken_ValidUser_ReturnsToken() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void generateRefreshToken_ValidUserDetails_ReturnsToken() {
        when(userDetails.getUsername()).thenReturn("john.doe@example.com");
        
        String token = jwtTokenProvider.generateRefreshToken(userDetails);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(userDetails).getUsername();
    }
    
    @Test
    void extractUsername_ValidToken_ReturnsUsername() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        
        String extractedUsername = jwtTokenProvider.extractUsername(token);
        
        assertEquals(testUser.getEmail(), extractedUsername);
    }
    
    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        when(userDetails.getUsername()).thenReturn(testUser.getEmail());
        
        boolean isValid = jwtTokenProvider.validateToken(token, userDetails);
        
        assertTrue(isValid);
        verify(userDetails).getUsername();
    }
    
    @Test
    void validateToken_TokenWithDifferentUsername_ReturnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        when(userDetails.getUsername()).thenReturn("different.user@example.com");
        
        boolean isValid = jwtTokenProvider.validateToken(token, userDetails);
        
        assertFalse(isValid);
        verify(userDetails).getUsername();
    }
    
    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";
        when(userDetails.getUsername()).thenReturn("john.doe@example.com");
        
        boolean isValid = jwtTokenProvider.validateToken(invalidToken, userDetails);
        
        assertFalse(isValid);
    }
    
    @Test
    void extractUsername_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.here";
        
        assertThrows(Exception.class, () -> jwtTokenProvider.extractUsername(invalidToken));
    }
    
    @Test
    void generateAccessToken_ContainsUserClaims() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        
        String username = jwtTokenProvider.extractUsername(token);
        
        assertEquals(testUser.getEmail(), username);
    }
}
