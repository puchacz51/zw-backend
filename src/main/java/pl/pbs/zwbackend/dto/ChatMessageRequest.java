package pl.pbs.zwbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.MessageType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for sending chat messages")
public class ChatMessageRequest {
    
    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    @Schema(description = "Content of the chat message", example = "Hello everyone!", required = true, maxLength = 1000)
    private String content;
    
    @Schema(description = "ID of the project (null for global chat)", example = "1")
    private Long projectId;
    
    @Builder.Default
    @Schema(description = "Type of the message", example = "CHAT", allowableValues = {"CHAT", "JOIN", "LEAVE"})
    private MessageType type = MessageType.CHAT;
    
    @Schema(description = "Email of the message sender", example = "user@example.com", required = true)
    private String senderEmail;
}
