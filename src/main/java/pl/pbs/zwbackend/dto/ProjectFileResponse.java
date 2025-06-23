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
public class ProjectFileResponse {
    private Long id;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private String description;
    private String downloadUrl;
    private UserSummaryResponse uploadedBy;
    private LocalDateTime uploadDate;
    private Long projectId;
}
