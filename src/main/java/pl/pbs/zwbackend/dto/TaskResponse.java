package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.TaskStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String name;
    private String description;
    private TaskStatus status;
    private Long projectId;
    private UserSummaryResponse assignedTo;
    private LocalDate dueDate;
    private LocalDate createdAt;
}
