package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFileUploadRequest {
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
