package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private String type; // "CHAT", "JOIN", "LEAVE"
    private String content;
    private String senderEmail;
    private String senderName;
    private String timestamp;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String senderAvatarUrl;
}
