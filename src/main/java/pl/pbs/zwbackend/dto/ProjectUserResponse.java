package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.ProjectRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserResponse {
    private UserSummaryResponse user;
    private ProjectRole role;
}
