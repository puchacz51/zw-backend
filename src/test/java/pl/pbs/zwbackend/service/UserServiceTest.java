package pl.pbs.zwbackend.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.model.enums.Role;
import pl.pbs.zwbackend.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .role(Role.USER)
            .build();
    }
    
    @Test
    void uploadAvatar_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getOriginalFilename()).thenReturn("avatar.jpg");
        when(fileStorageService.storeFile(any(MultipartFile.class), anyString())).thenReturn("avatar.jpg");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserSummaryResponse response = userService.uploadAvatar(multipartFile, testUser.getEmail());
        
        assertNotNull(response);
        assertEquals(testUser.getFirstName(), response.getFirstName());
        assertEquals(testUser.getLastName(), response.getLastName());
        assertEquals(testUser.getEmail(), response.getEmail());
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(fileStorageService).storeFile(multipartFile, "avatars");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void uploadAvatar_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> userService.uploadAvatar(multipartFile, "nonexistent@example.com"));
    }
    
    @Test
    void uploadAvatar_InvalidFileType_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(multipartFile.getContentType()).thenReturn("text/plain");
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.uploadAvatar(multipartFile, testUser.getEmail()));
    }
    
    @Test
    void getAllUsers_Success() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);
        
        List<UserSummaryResponse> responses = userService.getAllUsers();
        
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testUser.getFirstName(), responses.get(0).getFirstName());
        assertEquals(testUser.getLastName(), responses.get(0).getLastName());
        assertEquals(testUser.getEmail(), responses.get(0).getEmail());
        verify(userRepository).findAll();
    }
    
    @Test
    void deleteAvatar_Success() {
        testUser.setAvatarFileName("avatar.jpg");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        userService.deleteAvatar(testUser.getEmail());
        
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(fileStorageService).deleteFile("avatar.jpg", "avatars");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void deleteAvatar_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, 
            () -> userService.deleteAvatar("nonexistent@example.com"));
    }
}
