package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {
    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long size;
    private String url; // Constructed URL to access the image
    private UserSummaryResponse uploadedBy;
    private LocalDateTime uploadDate;
    private String description;
    private Long projectId;
    private String subDirectory;
}
