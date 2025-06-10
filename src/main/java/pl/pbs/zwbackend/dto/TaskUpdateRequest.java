package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TaskUpdateRequest {

    @NotBlank(message = "Task name cannot be blank")
    @Size(max = 100, message = "Task name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Task description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Task status is required")
    private TaskStatus status;

    private String assignedTo;

    private LocalDate dueDate;
}
