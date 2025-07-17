package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.ProjectStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Project name cannot be blank")
    @Size(max = 255, message = "Project name cannot exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Project description cannot exceed 1000 characters")
    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private ProjectStatus status;
}
