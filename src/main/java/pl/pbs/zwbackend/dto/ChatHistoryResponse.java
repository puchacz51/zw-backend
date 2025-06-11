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
public class ChatHistoryResponse {
    private Long id;
    private String content;
    private String senderFirstName;
    private String senderLastName;
    private String senderEmail;
    private LocalDateTime timestamp;
}
