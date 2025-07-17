package pl.pbs.zwbackend.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private FileStorageService fileStorageService;
    
    private String testSubDirectory = "test-uploads";
    
    @BeforeEach
    void setUp() {
    }
    
    @Test
    void storeFile_ValidFile_ReturnsFileName() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getBytes()).thenReturn("test content".getBytes());
        
        String fileName = fileStorageService.storeFile(multipartFile, testSubDirectory);
        
        assertNotNull(fileName);
        assertTrue(fileName.contains("test-file"));
        assertTrue(fileName.endsWith(".txt"));
        verify(multipartFile).getOriginalFilename();
        verify(multipartFile).getBytes();
    }
    
    @Test
    void storeFile_EmptyFile_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(true);
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.storeFile(multipartFile, testSubDirectory));
    }
    
    @Test
    void storeFile_NullFileName_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.storeFile(multipartFile, testSubDirectory));
    }
    
    @Test
    void storeFile_InvalidFileName_ThrowsException() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("../invalid-file.txt");
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.storeFile(multipartFile, testSubDirectory));
    }
    
    @Test
    void storeFile_IOError_ThrowsException() throws IOException {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test-file.txt");
        when(multipartFile.getBytes()).thenThrow(new IOException("IO Error"));
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.storeFile(multipartFile, testSubDirectory));
    }
    
    @Test
    void deleteFile_ValidFile_Success() {
        String fileName = "test-file.txt";
        
        assertDoesNotThrow(() -> fileStorageService.deleteFile(fileName, testSubDirectory));
    }
    
    @Test
    void deleteFile_InvalidPath_ThrowsException() {
        String fileName = "../invalid-path.txt";
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.deleteFile(fileName, testSubDirectory));
    }
    
    @Test
    void loadFileAsResource_ValidFile_ReturnsResource() {
        String fileName = "test-file.txt";
        
        
        assertDoesNotThrow(() -> fileStorageService.loadFileAsResource(fileName, testSubDirectory));
    }
    
    @Test
    void loadFileAsResource_NonExistentFile_ThrowsException() {
        String fileName = "non-existent-file.txt";
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.loadFileAsResource(fileName, testSubDirectory));
    }
    
    @Test
    void loadFileAsResource_InvalidPath_ThrowsException() {
        String fileName = "../invalid-path.txt";
        
        assertThrows(RuntimeException.class, 
            () -> fileStorageService.loadFileAsResource(fileName, testSubDirectory));
    }
}
