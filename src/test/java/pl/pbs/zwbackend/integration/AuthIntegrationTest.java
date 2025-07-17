package pl.pbs.zwbackend.integration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pl.pbs.zwbackend.dto.LoginRequest;
import pl.pbs.zwbackend.dto.RegisterRequest;
import pl.pbs.zwbackend.dto.ForgotPasswordRequest;
import pl.pbs.zwbackend.repository.UserRepository;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("password123");
            
        loginRequest = new LoginRequest();
        loginRequest.setLogin("john.doe@example.com");
        loginRequest.setPassword("password123");
    }
    
    @Test
    void registerAndLogin_FullFlow_Success() throws Exception {
        // Step 1: Register user
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.email").value("john.doe@example.com"))
            .andExpect(jsonPath("$.firstName").value("John"))
            .andExpect(jsonPath("$.lastName").value("Doe"));
        
        // Step 2: Login with registered credentials
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }
    
    @Test
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        // Step 1: Register user first time
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());
        
        // Step 2: Try to register with same email
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isConflict());
    }
    
    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Register user first
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());
        
        // Try to login with wrong password
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setLogin("john.doe@example.com");
        wrongPasswordRequest.setPassword("wrongpassword");
        
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void login_NonExistentUser_ReturnsUnauthorized() throws Exception {
        // Try to login with non-existent user
        LoginRequest nonExistentUserRequest = new LoginRequest();
        nonExistentUserRequest.setLogin("nonexistent@example.com");
        nonExistentUserRequest.setPassword("password123");
        
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentUserRequest)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void forgotPassword_ValidEmail_ReturnsSuccess() throws Exception {
        // Register user first
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());
        
        // Request password reset
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("john.doe@example.com");
        
        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));
    }
    
    @Test
    void forgotPassword_NonExistentEmail_ReturnsSuccess() throws Exception {
        // Request password reset for non-existent email (should not reveal if email exists)
        ForgotPasswordRequest forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("nonexistent@example.com");
        
        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));
    }
    
    @Test
    void register_InvalidData_ReturnsBadRequest() throws Exception {
        // Test with invalid email format
        RegisterRequest invalidEmailRequest = new RegisterRequest();
        invalidEmailRequest.setFirstName("John");
        invalidEmailRequest.setLastName("Doe");
        invalidEmailRequest.setEmail("invalid-email");
        invalidEmailRequest.setPassword("password123");
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        // Test with too short password
        RegisterRequest shortPasswordRequest = new RegisterRequest();
        shortPasswordRequest.setFirstName("John");
        shortPasswordRequest.setLastName("Doe");
        shortPasswordRequest.setEmail("john.doe@example.com");
        shortPasswordRequest.setPassword("123");
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPasswordRequest)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void register_EmptyFields_ReturnsBadRequest() throws Exception {
        // Test with empty required fields
        RegisterRequest emptyFieldsRequest = new RegisterRequest();
        emptyFieldsRequest.setFirstName("");
        emptyFieldsRequest.setLastName("");
        emptyFieldsRequest.setEmail("john.doe@example.com");
        emptyFieldsRequest.setPassword("password123");
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyFieldsRequest)))
            .andExpect(status().isBadRequest());
    }
}
