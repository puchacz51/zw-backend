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
public class MessageRequest {
    
    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;
}
