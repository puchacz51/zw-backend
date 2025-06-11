package pl.pbs.zwbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.MessageType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing chat message information")
public class ChatMessageResponse {
    
    @Schema(description = "Unique identifier of the message", example = "1")
    private Long id;
    
    @Schema(description = "Content of the chat message", example = "Hello everyone!")
    private String content;
    
    @Schema(description = "Type of the message", example = "CHAT", allowableValues = {"CHAT", "JOIN", "LEAVE"})
    private MessageType type;
    
    @Schema(description = "Information about the message sender")
    private UserSummaryResponse sender;
    
    @Schema(description = "ID of the project (null for global messages)", example = "1")
    private Long projectId;
    
    @Schema(description = "Name of the project (null for global messages)", example = "My Project")
    private String projectName;
    
    @Schema(description = "Timestamp when the message was sent", example = "2023-12-01T10:30:00")
    private LocalDateTime timestamp;
}
