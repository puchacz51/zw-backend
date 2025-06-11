package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryRequest {

    @Min(value = 0, message = "Offset cannot be negative")
    @Builder.Default
    private int offset = 0;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @Builder.Default
    private int limit = 20;

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String senderEmail;
    private String searchKeyword;
}
