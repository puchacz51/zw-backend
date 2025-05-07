package pl.pbs.zwbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pl.pbs.zwbackend.config.FileStorageProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path baseFileStorageLocation;

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.baseFileStorageLocation = Paths.get(fileStorageProperties.getBaseUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.baseFileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the base directory where the uploaded files will be stored.", ex);
        }
    }

    private Path getTargetLocation(String subDirectory, String fileName) {
        Path subDirPath = this.baseFileStorageLocation.resolve(subDirectory).normalize();
        try {
            Files.createDirectories(subDirPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the sub-directory: " + subDirectory, ex);
        }
        return subDirPath.resolve(fileName);
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            if (storedFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + storedFileName);
            }

            Path targetLocation = getTargetLocation(subDirectory, storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFileName + ". Please try again!", ex);
        }
    }

    public String storeFile(InputStream inputStream, String originalFileNameForExtension, String subDirectory) {
        String extension = "";
        int i = originalFileNameForExtension.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileNameForExtension.substring(i);
        }
        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            if (storedFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + storedFileName);
            }
            Path targetLocation = getTargetLocation(subDirectory, storedFileName);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName, String subDirectory) {
        try {
            Path filePath = this.baseFileStorageLocation.resolve(subDirectory).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName + " in " + subDirectory);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName + " in " + subDirectory, ex);
        }
    }

    public void deleteFile(String fileName, String subDirectory) {
        try {
            Path filePath = this.baseFileStorageLocation.resolve(subDirectory).resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + fileName + " from " + subDirectory + ". Please try again!", ex);
        }
    }
}
