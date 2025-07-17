package pl.pbs.zwbackend.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pl.pbs.zwbackend.dto.LoginRequest;
import pl.pbs.zwbackend.dto.RegisterRequest;
import pl.pbs.zwbackend.dto.TokenResponse;
import pl.pbs.zwbackend.dto.ForgotPasswordRequest;
import pl.pbs.zwbackend.model.RefreshToken;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.UserRepository;
import pl.pbs.zwbackend.security.JwtTokenProvider;
import pl.pbs.zwbackend.service.PasswordResetService;
import pl.pbs.zwbackend.service.RefreshTokenService;
import pl.pbs.zwbackend.service.UserService;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthenticationManager authenticationManager;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    
    @MockBean
    private RefreshTokenService refreshTokenService;
    
    @MockBean
    private PasswordResetService passwordResetService;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private ForgotPasswordRequest forgotPasswordRequest;
    private User testUser;
    private Authentication authentication;
    private UserDetails userDetails;
    
    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setLogin("john.doe@example.com");
        loginRequest.setPassword("password123");
            
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("password123");
            
        forgotPasswordRequest = new ForgotPasswordRequest();
        forgotPasswordRequest.setEmail("john.doe@example.com");
            
        testUser = User.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .role(Role.USER)
            .build();
            
        authentication = mock(Authentication.class);
        userDetails = mock(UserDetails.class);
    }
    
    @Test
    void login_Success() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("john.doe@example.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("jwt-token");
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token("refresh-token")
            .user(testUser)
            .build();
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);
        
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
            
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateAccessToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser);
    }
    
    @Test
    void login_InvalidCredentials_Unauthorized() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new RuntimeException("Bad credentials"));
        
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
            
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
    }
    
    @Test
    void login_InvalidInput_BadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setLogin(""); // Empty login should fail validation
        invalidRequest.setPassword("password123");
        
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
            
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
    
    @Test
    void register_Success() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userService.createUser(any(RegisterRequest.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("jwt-token");
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token("refresh-token")
            .user(testUser)
            .build();
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(refreshToken);
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").value("jwt-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
            
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userService).createUser(registerRequest);
        verify(jwtTokenProvider).generateAccessToken(testUser);
        verify(refreshTokenService).createRefreshToken(testUser);
    }
    
    @Test
    void register_EmailAlreadyExists_Conflict() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isConflict());
            
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userService, never()).createUser(any(RegisterRequest.class));
    }
    
    @Test
    void register_InvalidInput_BadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setFirstName("");
        invalidRequest.setLastName("Doe");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("123"); // Too short
        
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
            
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userService, never()).createUser(any(RegisterRequest.class));
    }
    
    @Test
    void forgotPassword_Success() throws Exception {
        doNothing().when(passwordResetService).createPasswordResetTokenForUser(anyString());
        
        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("If an account with that email exists, a password reset link has been sent."));
            
        verify(passwordResetService).createPasswordResetTokenForUser(forgotPasswordRequest.getEmail());
    }
    
    @Test
    void forgotPassword_InvalidEmail_BadRequest() throws Exception {
        ForgotPasswordRequest invalidRequest = new ForgotPasswordRequest();
        invalidRequest.setEmail("invalid-email");
        
        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());
            
        verify(passwordResetService, never()).createPasswordResetTokenForUser(anyString());
    }
}
