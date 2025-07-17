package pl.pbs.zwbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.pbs.zwbackend.dto.ImageResponse;
import pl.pbs.zwbackend.dto.UserSummaryResponse;
import pl.pbs.zwbackend.exception.ResourceNotFoundException;
import pl.pbs.zwbackend.exception.UnauthorizedOperationException;
import pl.pbs.zwbackend.model.Image;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;
import pl.pbs.zwbackend.repository.ImageRepository;
import pl.pbs.zwbackend.repository.ProjectRepository;
import pl.pbs.zwbackend.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    @Transactional
    public ImageResponse uploadImage(MultipartFile file, String subDirectory, String userEmail,
                                     Long projectId, String description) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Project project = null;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        }

        String storedFileName = fileStorageService.storeFile(file, subDirectory);

        Image image = Image.builder()
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .subDirectory(subDirectory)
                .uploadedBy(currentUser)
                .project(project)
                .description(description)
                .build();

        Image savedImage = imageRepository.save(image);
        return convertToResponse(savedImage);
    }

    public ResponseEntity<Resource> serveImage(String subDirectory, String storedFileName) {
        Resource resource = fileStorageService.loadFileAsResource(storedFileName, subDirectory);
        Image image = imageRepository.findByStoredFileNameAndSubDirectory(storedFileName, subDirectory)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "storedFileName/subDirectory", storedFileName + "/" + subDirectory));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getOriginalFileName() + "\"")
                .body(resource);
    }

    @Transactional(readOnly = true)
    public ImageResponse getImageDetails(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));
        return convertToResponse(image);
    }

    @Transactional
    public void deleteImage(Long imageId, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        // Allow deletion if user is admin or the uploader of the image
        if (!currentUser.getRole().equals(pl.pbs.zwbackend.model.enums.Role.ADMIN) &&
            !image.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new UnauthorizedOperationException("User not authorized to delete this image");
        }

        fileStorageService.deleteFile(image.getStoredFileName(), image.getSubDirectory());
        imageRepository.delete(image);
    }
    
    @Transactional(readOnly = true)
    public List<ImageResponse> getImagesForProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", "id", projectId);
        }
        return imageRepository.findAllByProjectId(projectId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ImageResponse> getMyImages(String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return imageRepository.findAllByUploadedById(currentUser.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ImageResponse convertToResponse(Image image) {
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/")
                .path(image.getSubDirectory())
                .path("/")
                .path(image.getStoredFileName())
                .toUriString();

        UserSummaryResponse userSummary = userService.convertToUserSummaryResponse(image.getUploadedBy());

        return ImageResponse.builder()
                .id(image.getId())
                .originalFileName(image.getOriginalFileName())
                .storedFileName(image.getStoredFileName())
                .contentType(image.getContentType())
                .size(image.getSize())
                .url(fileDownloadUri)
                .uploadedBy(userSummary)
                .uploadDate(image.getUploadDate())
                .description(image.getDescription())
                .projectId(image.getProject() != null ? image.getProject().getId() : null)
                .subDirectory(image.getSubDirectory())
                .build();
    }
}
