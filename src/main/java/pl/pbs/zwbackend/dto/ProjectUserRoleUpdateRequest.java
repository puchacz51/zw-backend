package pl.pbs.zwbackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.ProjectRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserRoleUpdateRequest {
    
    @NotNull
    private ProjectRole role;
}
