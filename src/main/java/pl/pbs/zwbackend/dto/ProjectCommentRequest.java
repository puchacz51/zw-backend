package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCommentRequest {
    
    @NotBlank(message = "Comment content is required")
    @Size(max = 1000, message = "Comment content cannot exceed 1000 characters")
    private String content;
}
