package pl.pbs.zwbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;
    private String testSubDir = "test_subdir";

    @BeforeEach
    void setUp() {
        // For testing, we directly provide the tempDir as the upload directory.
        // This bypasses @Value injection but tests the core logic.
        fileStorageService = new FileStorageService(tempDir.toString());
        fileStorageService.init(); // Manually call init as @PostConstruct won't run in this manual instantiation
    }

    @Test
    void constructor_shouldCreateBaseDirectory() {
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.isDirectory(tempDir));
    }

    @Test
    void storeFile_shouldStoreFileAndCreateSubDirectory() throws IOException {
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Hello, World!".getBytes(StandardCharsets.UTF_8)
        );

        String storedFileName = fileStorageService.storeFile(multipartFile, testSubDir);

        assertNotNull(storedFileName);
        assertTrue(storedFileName.contains(".txt")); // Check if extension is preserved

        Path expectedSubDirPath = tempDir.resolve(testSubDir);
        assertTrue(Files.exists(expectedSubDirPath));
        assertTrue(Files.isDirectory(expectedSubDirPath));

        Path expectedFilePath = expectedSubDirPath.resolve(storedFileName);
        assertTrue(Files.exists(expectedFilePath));
        assertEquals("Hello, World!", Files.readString(expectedFilePath));
    }

    @Test
    void storeFile_shouldGenerateUUIDBasedName() {
        MultipartFile multipartFile = new MockMultipartFile("file", "original.txt", "text/plain", "content".getBytes());
        String storedFileName = fileStorageService.storeFile(multipartFile, testSubDir);

        // Check if the name (excluding extension) looks like a UUID
        String nameWithoutExtension = storedFileName.substring(0, storedFileName.lastIndexOf('.'));
        try {
            UUID.fromString(nameWithoutExtension);
            // If no exception, it's a valid UUID format
        } catch (IllegalArgumentException e) {
            fail("Stored file name (without extension) is not a valid UUID: " + nameWithoutExtension);
        }
    }


    @Test
    void loadFile_shouldReturnCorrectPath() {
        String fileName = "test_file.txt";
        Path expectedPath = tempDir.resolve(testSubDir).resolve(fileName).normalize();
        assertEquals(expectedPath, fileStorageService.loadFile(fileName, testSubDir));
    }

    @Test
    void loadFileAsResource_shouldLoadExistingFile() throws IOException {
        String content = "Test content for loading";
        MultipartFile multipartFile = new MockMultipartFile("loadTest", "load.txt", "text/plain", content.getBytes());
        String storedFileName = fileStorageService.storeFile(multipartFile, testSubDir);

        Resource resource = fileStorageService.loadFileAsResource(storedFileName, testSubDir);

        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
        assertEquals(content, new String(resource.getInputStream().readAllBytes()));
    }

    @Test
    void loadFileAsResource_shouldThrowRuntimeException_whenFileNotExists() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.loadFileAsResource("nonexistent.txt", testSubDir);
        });
        assertTrue(exception.getMessage().contains("Could not read file: nonexistent.txt"));
    }

    @Test
    void deleteFile_shouldDeleteExistingFile() throws IOException {
        MultipartFile multipartFile = new MockMultipartFile("deleteTest", "delete.txt", "text/plain", "to be deleted".getBytes());
        String storedFileName = fileStorageService.storeFile(multipartFile, testSubDir);
        Path filePath = tempDir.resolve(testSubDir).resolve(storedFileName);

        assertTrue(Files.exists(filePath)); // Ensure file exists before deletion

        fileStorageService.deleteFile(storedFileName, testSubDir);

        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteFile_shouldNotThrow_whenFileNotExists() {
        assertDoesNotThrow(() -> {
            fileStorageService.deleteFile("already_gone.txt", testSubDir);
        });
    }

    @Test
    void getFileStorageLocation_shouldReturnCorrectPath() {
        assertEquals(tempDir.toAbsolutePath().normalize(), fileStorageService.getFileStorageLocation());
    }

    @Test
    void getConfiguredUploadDir_shouldReturnConstructorArgument() {
        assertEquals(tempDir.toString(), fileStorageService.getConfiguredUploadDir());
    }

    @Test
    void storeFile_whenOriginalFilenameHasNoExtension_shouldHandleGracefully() throws IOException {
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "testfilewithoutextension",
                "application/octet-stream",
                "Binary data".getBytes(StandardCharsets.UTF_8)
        );

        String storedFileName = fileStorageService.storeFile(multipartFile, testSubDir);
        assertNotNull(storedFileName);
        // Check if the name (which should be just UUID) is a valid UUID
        try {
            UUID.fromString(storedFileName);
        } catch (IllegalArgumentException e) {
            fail("Stored file name is not a valid UUID when original has no extension: " + storedFileName);
        }


        Path expectedFilePath = tempDir.resolve(testSubDir).resolve(storedFileName);
        assertTrue(Files.exists(expectedFilePath));
        assertArrayEquals("Binary data".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(expectedFilePath));
    }
}
