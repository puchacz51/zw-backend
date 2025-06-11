package pl.pbs.zwbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct; // Or import javax.annotation.PostConstruct if using older Java EE / Spring versions
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path fileStorageLocation;
    private final String configuredUploadDir;

    /**
     * Constructs a FileStorageService.
     * The upload directory is configured via the 'file.upload-dir' application property.
     * If the property is not set or is empty, a fallback directory is used.
     *
     * @param uploadDir The base directory for storing uploaded files, injected from properties.
     * @throws RuntimeException if the file storage location cannot be initialized.
     */
    @Autowired
    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.configuredUploadDir = uploadDir; // Store the original configured value
        logger.info("FileStorageService constructor: Injected 'file.upload-dir' = '{}'", uploadDir);

        String effectiveUploadDir = uploadDir;
        if (effectiveUploadDir == null || effectiveUploadDir.trim().isEmpty()) {
            logger.warn("'file.upload-dir' property is null or empty. Using fallback default directory './uploads_fallback_from_code'.");
            effectiveUploadDir = "./uploads_fallback_from_code";
        }
        
        logger.info("FileStorageService constructor: Effective upload directory = '{}'", effectiveUploadDir);

        try {
            this.fileStorageLocation = Paths.get(effectiveUploadDir).toAbsolutePath().normalize();
            // Directory creation is handled in the @PostConstruct init() method.
        } catch (Exception ex) {
            logger.error("Error initializing file storage location with effectiveUploadDir='{}': {}", effectiveUploadDir, ex.getMessage(), ex);
            throw new RuntimeException("Could not initialize file storage location. Effective path: '" + effectiveUploadDir + "'", ex);
        }
    }

    /**
     * Initializes the service by creating the file storage directory if it doesn't exist.
     * This method is called automatically after the bean has been constructed.
     *
     * @throws RuntimeException if the storage directory cannot be created.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage directory created/ensured at: {}", this.fileStorageLocation.toString());
        } catch (IOException ex) {
            logger.error("Could not create the storage directory '{}': {}", this.fileStorageLocation.toString(), ex.getMessage(), ex);
            throw new RuntimeException("Could not create the storage directory: " + this.fileStorageLocation.toString(), ex);
        }
    }

    /**
     * Stores the given file in the specified subdirectory.
     * The file is saved with a unique name generated using UUID.
     *
     * @param file The multipart file to store.
     * @param subDirectory The subdirectory within the base upload directory to store the file.
     * @return The unique name of the stored file.
     * @throws RuntimeException if the file cannot be stored.
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            logger.warn("Could not determine file extension for '{}'", originalFileName);
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetDirectory = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetDirectory); // Ensure sub-directory exists

            Path targetPath = targetDirectory.resolve(newFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return newFileName;
        } catch (IOException ex) {
            logger.error("Could not store file {} in subdirectory {}: {}", newFileName, subDirectory, ex.getMessage(), ex);
            throw new RuntimeException("Could not store file " + newFileName + ". Please try again!", ex);
        }
    }

    /**
     * Loads a file as a Path object from the specified subdirectory.
     *
     * @param fileName The name of the file to load.
     * @param subDirectory The subdirectory where the file is located.
     * @return The Path object representing the loaded file.
     */
    public Path loadFile(String fileName, String subDirectory) {
        return this.fileStorageLocation.resolve(subDirectory).resolve(fileName).normalize();
    }

    /**
     * Loads a file as a Spring Resource from the specified subdirectory.
     *
     * @param fileName The name of the file to load.
     * @param subDirectory The subdirectory where the file is located.
     * @return The Resource object representing the loaded file.
     * @throws RuntimeException if the file cannot be read or found, or if the path results in a malformed URL.
     */
    public Resource loadFileAsResource(String fileName, String subDirectory) {
        try {
            Path filePath = loadFile(fileName, subDirectory);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                logger.error("Could not read file: {} from subdirectory {}", fileName, subDirectory);
                throw new RuntimeException("Could not read file: " + fileName);
            }
        } catch (MalformedURLException ex) {
            logger.error("Malformed URL for file: {} from subdirectory {}: {}", fileName, subDirectory, ex.getMessage(), ex);
            throw new RuntimeException("Could not read file: " + fileName, ex);
        }
    }

    /**
     * Deletes the specified file from the given subdirectory.
     * If the file does not exist, the operation is logged and no error is thrown.
     *
     * @param fileName The name of the file to delete.
     * @param subDirectory The subdirectory where the file is located.
     */
    public void deleteFile(String fileName, String subDirectory) {
        try {
            Path filePath = loadFile(fileName, subDirectory);
            Files.deleteIfExists(filePath);
            logger.info("Successfully deleted file: {} from subdirectory: {}", fileName, subDirectory);
        } catch (IOException ex) {
            logger.error("Could not delete file: {} from subdirectory: {}. Error: {}", fileName, subDirectory, ex.getMessage(), ex);
            // Depending on the application's needs, you might rethrow this as a custom exception
            // or handle it silently if deletion failure is not critical.
            // For now, we log the error and do not rethrow to avoid breaking operations if a file is already gone.
        }
    }

    /**
     * Gets the absolute, normalized path to the file storage location.
     *
     * @return The file storage location path.
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }

    /**
     * Gets the originally configured upload directory string, before any fallback logic was applied.
     *
     * @return The configured upload directory string.
     */
    public String getConfiguredUploadDir() {
        return configuredUploadDir;
    }
}
