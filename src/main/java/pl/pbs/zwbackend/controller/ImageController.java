package pl.pbs.zwbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.pbs.zwbackend.dto.ImageResponse;
import pl.pbs.zwbackend.service.ImageService;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subDirectory") String subDirectory,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetails currentUser) {
        ImageResponse imageResponse = imageService.uploadImage(file, subDirectory, currentUser.getUsername(), projectId, description);
        return new ResponseEntity<>(imageResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{subDirectory}/{storedFileName:.+}")
    public ResponseEntity<Resource> serveImage(
            @PathVariable String subDirectory,
            @PathVariable String storedFileName) {
        return imageService.serveImage(subDirectory, storedFileName);
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<ImageResponse> getImageDetails(@PathVariable Long id) {
        ImageResponse imageResponse = imageService.getImageDetails(id);
        return ResponseEntity.ok(imageResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails currentUser) {
        imageService.deleteImage(id, currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ImageResponse>> getImagesForProject(@PathVariable Long projectId) {
        List<ImageResponse> images = imageService.getImagesForProject(projectId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/my-images")
    public ResponseEntity<List<ImageResponse>> getMyImages(@AuthenticationPrincipal UserDetails currentUser) {
        List<ImageResponse> images = imageService.getMyImages(currentUser.getUsername());
        return ResponseEntity.ok(images);
    }
}
