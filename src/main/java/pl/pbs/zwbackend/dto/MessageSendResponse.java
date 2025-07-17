package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendResponse {
    private boolean success;
    private String message;
    private MessageResponse data;
}
