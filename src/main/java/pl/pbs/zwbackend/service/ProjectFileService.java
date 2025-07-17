package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.pbs.zwbackend.dto.ProjectFileResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.ProjectFile;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.ProjectFileRepository;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.ProjectUserRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectUserRepository projectUserRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    @Value("${file.project-files-subdir}")
    private String projectFilesSubDirectory;

    @Value("${file.max-file-size-mb}")
    private Long maxFileSizeMB;

    @Value("${file.max-filename-length}")
    private Integer maxFileNameLength;

    // Allowed file extensions (you can modify this list as needed)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rtf", "odt", "ods", "odp",
            "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "bmp", "tiff",
            "mp4", "avi", "mov", "wmv", "flv",
            "mp3", "wav", "flac", "aac",
            "csv", "json", "xml", "sql"
    );

    // Forbidden file extensions for security reasons
    private static final List<String> FORBIDDEN_EXTENSIONS = Arrays.asList(
            "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "js", "jar", "msi"
    );

    @Transactional
    public ProjectFileResponse uploadFile(MultipartFile file, Long projectId, String userEmail, String description) {
        // Validate user
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        // Validate project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Check if user has access to the project
        if (!hasProjectAccess(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to upload files to this project");
        }

        // Validate file
        validateFile(file);

        // Store file
        String storedFileName = fileStorageService.storeFile(file, projectFilesSubDirectory);

        // Create ProjectFile entity
        ProjectFile projectFile = ProjectFile.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .description(description)
                .uploadedBy(currentUser)
                .project(project)
                .build();

        ProjectFile savedFile = projectFileRepository.save(projectFile);
        return convertToResponse(savedFile);
    }

    public ResponseEntity<Resource> downloadFile(Long fileId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectFile projectFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFile", "id", fileId));

        // Check if user has access to the project
        if (!hasProjectAccess(projectFile.getProject(), currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to download this file");
        }

        Resource resource = fileStorageService.loadFileAsResource(projectFile.getStoredFileName(), projectFilesSubDirectory);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(projectFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + projectFile.getOriginalFileName() + "\"")
                .body(resource);
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> getProjectFiles(Long projectId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        // Check if user has access to the project
        if (!hasProjectAccess(project, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to view project files");
        }

        return projectFileRepository.findAllByProjectId(projectId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectFileResponse getFileDetails(Long fileId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectFile projectFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFile", "id", fileId));

        // Check if user has access to the project
        if (!hasProjectAccess(projectFile.getProject(), currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to view this file");
        }

        return convertToResponse(projectFile);
    }

    @Transactional
    public void deleteFile(Long fileId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        ProjectFile projectFile = projectFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFile", "id", fileId));

        // Check if user is authorized to delete (project owner, file uploader, or project manager)
        if (!canDeleteFile(projectFile, currentUser)) {
            throw new UnauthorizedOperationException("User not authorized to delete this file");
        }

        // Delete physical file
        fileStorageService.deleteFile(projectFile.getStoredFileName(), projectFilesSubDirectory);

        // Delete database record
        projectFileRepository.delete(projectFile);
    }

    @Transactional(readOnly = true)
    public List<ProjectFileResponse> getMyFiles(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        return projectFileRepository.findAllByUploadedById(currentUser.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Check file size
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > maxFileSizeMB) {
            throw new IllegalArgumentException("File size cannot exceed " + maxFileSizeMB + "MB");
        }

        // Check filename length
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFileName.length() > maxFileNameLength) {
            throw new IllegalArgumentException("Filename cannot exceed " + maxFileNameLength + " characters");
        }

        // Check file extension
        String extension = getFileExtension(originalFileName).toLowerCase();
        
        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type ." + extension + " is not allowed for security reasons");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension) && !extension.isEmpty()) {
            throw new IllegalArgumentException("File type ." + extension + " is not supported. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // Check for malicious filenames
        if (originalFileName.contains("..") || originalFileName.contains("/") || originalFileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private boolean hasProjectAccess(Project project, User user) {
        // Project owner always has access
        if (project.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Check if user is assigned to the project
        return projectUserRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private boolean canDeleteFile(ProjectFile projectFile, User user) {
        // Admin can always delete
        if (user.getRole().equals(pl.pbs.zwbackend.model.enums.Role.ADMIN)) {
            return true;
        }

        // File uploader can delete their own files
        if (projectFile.getUploadedBy().getId().equals(user.getId())) {
            return true;
        }

        // Project owner can delete any file in their project
        if (projectFile.getProject().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Project manager can delete files
        return projectUserRepository.findByProjectIdAndUserId(projectFile.getProject().getId(), user.getId())
                .map(pu -> pu.getRole() == pl.pbs.zwbackend.model.enums.ProjectRole.MANAGER)
                .orElse(false);
    }

    private ProjectFileResponse convertToResponse(ProjectFile projectFile) {
        String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/projects/")
                .path(projectFile.getProject().getId().toString())
                .path("/files/")
                .path(projectFile.getId().toString())
                .path("/download")
                .toUriString();

        UserSummaryResponse userSummary = userService.convertToUserSummaryResponse(projectFile.getUploadedBy());

        return ProjectFileResponse.builder()
                .id(projectFile.getId())
                .originalFileName(projectFile.getOriginalFileName())
                .storedFileName(projectFile.getStoredFileName())
                .contentType(projectFile.getContentType())
                .fileSize(projectFile.getFileSize())
                .description(projectFile.getDescription())
                .downloadUrl(downloadUrl)
                .uploadedBy(userSummary)
                .uploadDate(projectFile.getUploadDate())
                .projectId(projectFile.getProject().getId())
                .build();
    }
}
